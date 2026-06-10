from __future__ import annotations
import csv
import hashlib
import io
import json
import logging
import os
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

import pandas as pd
import psycopg2
import psycopg2.extras
from dotenv import load_dotenv

load_dotenv(override=False)

logger = logging.getLogger("upload_raw_optimized")

CHUNK_SIZE = 50_000

PROCESSED_COLUMNS = {
    "user_id", "customer_id", "product_id", "category", "type",
    "amount", "quantity", "score", "value", "source", "region",
    "device", "event_date", "status",
}

COLUMN_ALIASES = {
    "precio": "value",       "price": "value",
    "monto":  "amount",      "total": "amount",
    "fecha":  "event_date",  "fecha_evento": "event_date",  "date": "event_date",
    "tipo":   "type",        "estado": "status",
    "cantidad": "quantity",  "qty": "quantity",
    "puntaje": "score",      "puntuacion": "score",
    "canal":  "source",      "fuente": "source",
    "zona":   "region",      "dispositivo": "device",
    "nombre": "product_id",  "name": "product_id",
    "id_producto": "product_id",
    "id_cliente":  "customer_id",
    "id_usuario":  "user_id",
    "cliente":     "customer_id",
}


# ═══════════════════════════════════════════════════════════════════════════════
# Conexión
# ═══════════════════════════════════════════════════════════════════════════════

def _get_connection() -> psycopg2.extensions.connection:
    conn = psycopg2.connect(
        host=os.getenv("host", "localhost"),
        port=int(os.getenv("port", 5432)),
        dbname=os.getenv("database"),
        user=os.getenv("user"),
        password=os.getenv("password"),
    )
    # autocommit OFF → controlamos las transacciones manualmente
    conn.autocommit = False
    psycopg2.extras.register_uuid()
    return conn


# ═══════════════════════════════════════════════════════════════════════════════
# Helpers de tipo / sanitización (reutilizados de upload_raw.py)
# ═══════════════════════════════════════════════════════════════════════════════

def _checksum(s: str) -> str:
    return hashlib.md5(s.encode()).hexdigest()


def _safe_uuid(v) -> Optional[str]:
    if v is None:
        return None
    try:
        return str(uuid.UUID(str(v)))
    except (ValueError, AttributeError):
        return None


def _safe_numeric(v) -> Optional[float]:
    if v is None:
        return None
    try:
        f = float(v)
        return None if (f != f) else f   # NaN → None
    except (ValueError, TypeError):
        return None


def _safe_timestamp(v) -> Optional[str]:
    if v is None:
        return None
    if isinstance(v, datetime):
        return v.isoformat()
    try:
        return pd.to_datetime(v).isoformat()
    except Exception:
        return None


def _normalizar_fila(fila: dict) -> tuple[dict, dict]:
    """Devuelve (conocidas, extras) después de aplicar aliases."""
    norm: dict = {}
    for k, v in fila.items():
        ck = COLUMN_ALIASES.get(k.lower().strip(), k.lower().strip())
        if ck not in norm:
            norm[ck] = v
        else:
            norm[k] = v   # colisión: conservar con nombre original

    conocidas = {k: v for k, v in norm.items() if k in PROCESSED_COLUMNS}
    extras    = {k: v for k, v in norm.items() if k not in PROCESSED_COLUMNS}

    # user_id / product_id no-UUID → mover a extras
    for campo in ("user_id", "customer_id", "product_id"):
        if conocidas.get(campo) is not None and _safe_uuid(conocidas[campo]) is None:
            extras[campo] = conocidas.pop(campo)

    return conocidas, extras


# ═══════════════════════════════════════════════════════════════════════════════
# Helpers de catálogo (idénticos API a upload_raw.py original)
# ═══════════════════════════════════════════════════════════════════════════════

def _upsert_data_source(cur, archivo: str) -> uuid.UUID:
    nombre = Path(archivo).name
    ext    = Path(archivo).suffix.lstrip(".").lower() or "unknown"
    cur.execute(
        """
        INSERT INTO data_sources (source_type, file_name, description)
        VALUES (%s, %s, %s)
        ON CONFLICT (file_name) DO UPDATE SET source_type = EXCLUDED.source_type
        RETURNING source_id
        """,
        (ext, nombre, f"Cargado automáticamente desde {archivo}"),
    )
    return cur.fetchone()[0]


def _insert_data_ingestion(cur, source_id, archivo: str, record_count: int) -> uuid.UUID:
    cur.execute(
        """
        INSERT INTO data_ingestions (source_id, file_name, record_count, status)
        VALUES (%s, %s, %s, 'processing')
        RETURNING batch_id
        """,
        (source_id, Path(archivo).name, record_count),
    )
    return cur.fetchone()[0]


def _update_ingestion_status(cur, batch_id, status: str, notes: Optional[str] = None):
    cur.execute(
        "UPDATE data_ingestions SET status = %s, notes = %s WHERE batch_id = %s",
        (status, notes, batch_id),
    )


def _insert_audit_log(cur, *, process_name, level, batch_id=None,
                      error_message=None, execution_time=None, metadata=None):
    cur.execute(
        """
        INSERT INTO audit_logs
            (process_name, log_level, batch_id, error_message, execution_time, metadata)
        VALUES (%s, %s, %s, %s, %s, %s)
        """,
        (
            process_name, level, batch_id, error_message,
            execution_time,
            psycopg2.extras.Json(metadata) if metadata else None,
        ),
    )

def _escape_tsv(v) -> str:
    """Convierte un valor Python a su representación TSV segura."""
    if v is None:
        return r"\N"
    s = str(v)
    # El protocolo COPY TEXT de PostgreSQL requiere escapar \n, \r, \t, \
    return (
        s.replace("\\", "\\\\")
         .replace("\n", "\\n")
         .replace("\r", "\\r")
         .replace("\t", "\\t")
    )


def _registros_a_tsv_raw(
    registros: list[dict],
    batch_id: str,
    source_id: str,
    file_format: str,
    offset: int,
) -> tuple[io.StringIO, list[str]]:

    buf = io.StringIO()
    checksums: list[str] = []

    for i, fila in enumerate(registros):
        raw_id   = str(uuid.uuid4())
        content  = json.dumps(fila, ensure_ascii=False, default=str)
        cs       = _checksum(f"{batch_id}:{offset + i}:{content}")
        checksums.append((raw_id, cs))   # (raw_id tentativo, checksum)

        row = "\t".join([
            raw_id,
            batch_id,
            source_id,
            file_format,
            _escape_tsv(content),
            cs,
        ])
        buf.write(row + "\n")

    buf.seek(0)
    return buf, checksums


def _copy_raw_data_chunk(cur, buf: io.StringIO):

    cur.copy_expert(
        """
        COPY raw_data (raw_id, batch_id, source_id, file_format, raw_content, checksum)
        FROM STDIN
        WITH (FORMAT TEXT, NULL '\\N', ENCODING 'UTF8')
        """,
        buf,
    )


def _registros_a_tsv_processed(
    registros: list[dict],
    raw_ids: dict[str, str],
    batch_id: str,
    checksums_chunk: list[tuple[str, str]],
) -> io.StringIO:

    buf = io.StringIO()

    for (raw_id_tent, cs), fila in zip(checksums_chunk, registros):
        conocidas, extras = _normalizar_fila(fila)
        raw_id = raw_ids.get(cs, raw_id_tent)

        row = "\t".join([
            str(uuid.uuid4()),
            raw_id,
            batch_id,
            _escape_tsv(str(conocidas.get("status", "processed"))[:20]),
            _escape_tsv(_safe_uuid(conocidas.get("user_id"))),
            _escape_tsv(_safe_uuid(conocidas.get("customer_id"))),
            _escape_tsv(_safe_uuid(conocidas.get("product_id"))),
            _escape_tsv(str(conocidas["category"])[:100] if conocidas.get("category") else None),
            _escape_tsv(str(conocidas["type"])[:100]     if conocidas.get("type")     else None),
            _escape_tsv(_safe_numeric(conocidas.get("amount"))),
            _escape_tsv(_safe_numeric(conocidas.get("quantity"))),
            _escape_tsv(_safe_numeric(conocidas.get("score"))),
            _escape_tsv(_safe_numeric(conocidas.get("value"))),
            _escape_tsv(str(conocidas["source"])[:100]   if conocidas.get("source")   else None),
            _escape_tsv(str(conocidas["region"])[:100]   if conocidas.get("region")   else None),
            _escape_tsv(str(conocidas["device"])[:100]   if conocidas.get("device")   else None),
            _escape_tsv(_safe_timestamp(conocidas.get("event_date"))),
            _escape_tsv(json.dumps(extras, default=str) if extras else None),
        ])
        buf.write(row + "\n")

    buf.seek(0)
    return buf


def _copy_processed_data_chunk(cur, buf: io.StringIO):
    cur.copy_expert(
        """
        COPY processed_data (
            record_id, raw_id, batch_id, status,
            user_id, customer_id, product_id, category, type,
            amount, quantity, score, value, source, region, device,
            event_date, data
        )
        FROM STDIN
        WITH (FORMAT TEXT, NULL '\\N', ENCODING 'UTF8')
        """,
        buf,
    )

def _fetch_raw_ids_by_checksums(cur, checksums: list[str]) -> dict[str, str]:
    if not checksums:
        return {}
    cur.execute(
        "SELECT checksum, raw_id FROM raw_data WHERE checksum = ANY(%s)",
        (checksums,),
    )
    return {row[0]: str(row[1]) for row in cur.fetchall()}

_INDEX_DDL = {
    "raw_data": [
        "CREATE UNIQUE INDEX IF NOT EXISTS raw_data_checksum_key ON raw_data (checksum)",
        "CREATE INDEX IF NOT EXISTS raw_data_batch_id_idx ON raw_data (batch_id)",
    ],
    "processed_data": [
        "CREATE INDEX IF NOT EXISTS processed_data_batch_id_idx ON processed_data (batch_id)",
        "CREATE INDEX IF NOT EXISTS processed_data_event_date_idx ON processed_data (event_date)",
        "CREATE UNIQUE INDEX IF NOT EXISTS processed_data_raw_batch_uidx ON processed_data (raw_id, batch_id)",
    ],
}

_INDEX_DROP = {
    "raw_data": [
        "DROP INDEX IF EXISTS raw_data_checksum_key",
        "DROP INDEX IF EXISTS raw_data_batch_id_idx",
    ],
    "processed_data": [
        "DROP INDEX IF EXISTS processed_data_batch_id_idx",
        "DROP INDEX IF EXISTS processed_data_event_date_idx",
        "DROP INDEX IF EXISTS processed_data_raw_batch_uidx",
    ],
}


def _disable_indexes(cur, tablas: list[str]):

    for tabla in tablas:
        for ddl in _INDEX_DROP.get(tabla, []):
            logger.debug("DROP INDEX: %s", ddl)
            cur.execute(ddl)


def _rebuild_indexes(cur, tablas: list[str]):
    for tabla in tablas:
        for ddl in _INDEX_DDL.get(tabla, []):
            logger.debug("CREATE INDEX: %s", ddl)
            cur.execute(ddl)

def bulk_upload(
    cur,
    registros: list[dict],
    source_id: uuid.UUID,
    batch_id: uuid.UUID,
    file_format: str,
    disable_idx: bool = True,
) -> int:

    if not registros:
        return 0

    batch_id_str  = str(batch_id)
    source_id_str = str(source_id)
    total         = 0

    if disable_idx:
        _disable_indexes(cur, ["raw_data", "processed_data"])

    try:
        offset = 0
        while offset < len(registros):
            chunk = registros[offset: offset + CHUNK_SIZE]

            buf_raw, checksums_chunk = _registros_a_tsv_raw(
                chunk, batch_id_str, source_id_str, file_format, offset
            )
            _copy_raw_data_chunk(cur, buf_raw)

            only_cs    = [cs for (_, cs) in checksums_chunk]
            raw_id_map = _fetch_raw_ids_by_checksums(cur, only_cs)

            buf_proc = _registros_a_tsv_processed(
                chunk, raw_id_map, batch_id_str, checksums_chunk
            )
            _copy_processed_data_chunk(cur, buf_proc)

            total  += len(chunk)
            offset += CHUNK_SIZE
            logger.info("  Chunk %d-%d cargado ✓", offset - CHUNK_SIZE, offset - 1)

    finally:
        if disable_idx:
            _rebuild_indexes(cur, ["raw_data", "processed_data"])

    return total

def upload_batch(
    cur,
    registros: list[dict],
    archivo: str,
    disable_idx: bool = True,
) -> tuple[uuid.UUID, uuid.UUID, int]:

    inicio      = datetime.now(timezone.utc)
    file_format = Path(archivo).suffix.lstrip(".").lower() or "json"

    source_id = _upsert_data_source(cur, archivo)
    batch_id  = _insert_data_ingestion(cur, source_id, archivo, len(registros))

    try:
        total = bulk_upload(cur, registros, source_id, batch_id, file_format, disable_idx)
        _update_ingestion_status(cur, batch_id, "completed")

        elapsed = datetime.now(timezone.utc) - inicio
        _insert_audit_log(
            cur,
            process_name   = f"bulk_upload:{Path(archivo).name}",
            level          = "INFO",
            batch_id       = batch_id,
            execution_time = elapsed,
            metadata       = {
                "total_registros": total,
                "estrategia":      "COPY_bulk",
                "chunk_size":      CHUNK_SIZE,
            },
        )
        logger.info(
            "✔ Upload completado — %d registros en %.2fs",
            total, elapsed.total_seconds(),
        )
        return source_id, batch_id, total

    except Exception as exc:
        _update_ingestion_status(cur, batch_id, "failed", notes=str(exc))
        _insert_audit_log(
            cur,
            process_name  = f"bulk_upload:{Path(archivo).name}",
            level         = "ERROR",
            batch_id      = batch_id,
            error_message = str(exc),
        )
        raise