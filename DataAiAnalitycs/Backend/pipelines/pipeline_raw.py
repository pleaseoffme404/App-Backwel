from __future__ import annotations
import json
import logging
import uuid
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Optional, List
import pandas as pd
import psycopg2
import psycopg2.extras
from Backend.src.ingestion.readers import BaseReader
from Backend.src.processing.jsonParser import JsonParser
from Backend.src.processing.clean_data import DataCleaner
from data.upload_raw import (_get_connection, upload_batch,)
import re

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("pipeline_raw_optimized")

UNIQUE_KEYS: Optional[List[str]] = None


_PATTERNS: list[tuple[str, list[str]]] = [
    ("uuid",     [r"\bid\b", r"_id$", r"^id_", r"uuid", r"guid"]),
    ("email",    [r"email", r"correo", r"mail"]),
    ("datetime", [r"date", r"fecha", r"timestamp", r"_at$", r"_on$", r"tiempo", r"hora"]),
    ("boolean",  [r"^is_", r"^has_", r"^activo", r"^active", r"^flag", r"bool"]),
    ("currency", [r"price", r"precio", r"amount", r"monto", r"total", r"cost", r"costo",
                  r"revenue", r"ingreso", r"salary", r"salario"]),
    ("quantity", [r"qty", r"quantity", r"cantidad", r"count", r"stock", r"units",
                  r"unidades", r"num_", r"numero"]),
]


def _infer_col_type(col: str, series: pd.Series) -> str:
    nombre = col.lower().strip()
    for tipo, patrones in _PATTERNS:
        for patron in patrones:
            if re.search(patron, nombre):
                return tipo

    dtype = series.dtype
    if pd.api.types.is_bool_dtype(dtype):           return "boolean"
    if pd.api.types.is_integer_dtype(dtype):        return "quantity"
    if pd.api.types.is_float_dtype(dtype):          return "currency"
    if pd.api.types.is_datetime64_any_dtype(dtype): return "datetime"

    muestra  = series.dropna().astype(str).head(5)
    fecha_re = re.compile(r"\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4}")
    if len(muestra) and muestra.apply(lambda v: bool(fecha_re.search(v))).all():
        return "datetime"

    return "text"


def _build_dynamic_schema(df: pd.DataFrame) -> dict:
    schema = {col: _infer_col_type(col, df[col]) for col in df.columns}
    logger.info("Schema dinámico: %s", schema)
    return schema


def _a_nombre_json(archivo: str) -> str:
    origen = Path(archivo)
    if origen.suffix.lstrip(".").lower() == "json":
        return archivo
    return str(origen.with_name(f"{origen.stem}_{origen.suffix.lstrip('.').lower()}.json"))


class _JsonSanitizer(json.JSONEncoder):
    def default(self, obj):
        try:
            import numpy as np
            if isinstance(obj, np.integer):  return int(obj)
            if isinstance(obj, np.floating): return None if np.isnan(obj) else float(obj)
            if isinstance(obj, np.bool_):    return bool(obj)
            if isinstance(obj, np.ndarray):  return obj.tolist()
        except ImportError:
            pass
        if hasattr(obj, "isoformat"):  return obj.isoformat()
        if obj is pd.NaT:              return None
        try:
            if pd.isna(obj): return None
        except (TypeError, ValueError):
            pass
        return str(obj)


def _parsear_a_json(parser: JsonParser, df: pd.DataFrame, archivo: str, id_archivo: str) -> dict:
    json_str = parser._df_a_json(df, archivo, id_archivo)
    raw_dict = json.loads(json_str)
    return json.loads(json.dumps(raw_dict, cls=_JsonSanitizer, ensure_ascii=False))


def _determinar_tabla_destino(archivo: str, df: pd.DataFrame) -> str:

    nombre = Path(archivo).stem.lower()
    columnas = set(df.columns)

    if {"sku", "base_price"}.issubset(columnas) or "sku" in columnas:
        return "product"

    if {"email", "nombre", "apellido"}.issubset(columnas) or {"email", "name"}.issubset(columnas):
        return "user_info"

    if {"amount", "quantity", "type"}.issubset(columnas):
        return "processed_data"

    if {"process_name", "log_level"}.issubset(columnas):
        return "audit_logs"

    if {"source_type", "region"}.issubset(columnas) and "batch_id" not in columnas:
        return "data_sources"

    if {"batch_id", "source_id", "ingestion_date"}.issubset(columnas):
        return "data_ingestions"

    if "catalogo" in nombre: return "product"
    if "cliente" in nombre: return "user_info"
    if "transacciones" in nombre: return "processed_data"
    if "audit" in nombre: return "audit_logs"

    logger.warning(
        "Router ciego: No se reconocieron columnas ni el nombre en '%s'. Fallback → raw_data.",
        archivo
    )
    return "raw_data"


def _adaptar_transacciones_processed_data(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()

    if "event_date" in df.columns:
        df["event_date"] = pd.to_datetime(df["event_date"], errors="coerce")

    if "status" in df.columns:
        mapeo_estatus = {
            "Emitida":   "completed",
            "Pendiente": "pending",
            "Cancelada": "cancelled",
        }
        df["status"] = df["status"].map(mapeo_estatus).fillna("anomalo")

    if "user_id" not in df.columns and "customer_id" in df.columns:
        df["user_id"] = df["customer_id"]

    if "client_id" in df.columns:
        df = df.drop(columns=["client_id"])

    return df

def _parse_execution_time(valor: str) -> Optional[str]:
    if pd.isna(valor) or valor is None:
        return None

    texto = str(valor).strip().lower()
    try:
        m = re.match(r"^([\d.]+)\s*ms$", texto)
        if m:
            ms = float(m.group(1))
            return str(timedelta(milliseconds=ms))

        m = re.match(r"^([\d.]+)\s*s(?:ec)?$", texto)
        if m:
            secs = float(m.group(1))
            return str(timedelta(seconds=secs))

        m = re.match(r"^([\d.]+)\s*min$", texto)
        if m:
            mins = float(m.group(1))
            return str(timedelta(minutes=mins))

    except (ValueError, AttributeError):
        pass

    logger.warning("No se pudo parsear execution_time: '%s'", valor)
    return None


def _cargar_tabla_estructurada(cur, df: pd.DataFrame, tabla: str) -> int:

    if df.empty:
        return 0

    registros: list[dict] = []

    if tabla == "product":
        cur.execute("SELECT id FROM category LIMIT 1;")
        cat_row = cur.fetchone()

        if cat_row:
            valid_category_id = cat_row[0]
        else:
            valid_category_id = str(uuid.uuid4())
            cur.execute(
                "INSERT INTO category (id, name, created_at) VALUES (%s, %s, NOW()) ON CONFLICT DO NOTHING;",
                (valid_category_id, "Categoría Automática")
            )

        sql = """
              INSERT INTO product (id, brand, name, description, category_id)
              VALUES (%(id)s, %(brand)s, %(name)s, %(description)s, %(category_id)s)
              ON CONFLICT (id) DO NOTHING; \
              """
        for _, row in df.iterrows():
            registros.append({
                "id": row.get("product_id") or str(uuid.uuid4()),
                "brand": row.get("marca") or row.get("brand") or "Genérica",
                "name": row.get("name") or row.get("nombre") or "Producto sin nombre",
                "description": row.get("description") or "Importado masivamente",
                "category_id": valid_category_id,
            })

    elif tabla == "user_info":
        sql = """
              INSERT INTO user_info (uuid, email, name, surname, phone_number, \
                                     country_code, currency_code, picture_url)
              VALUES (%(uuid)s, %(email)s, %(name)s, %(surname)s, %(phone_number)s, \
                      %(country_code)s, %(currency_code)s, %(picture_url)s)
              ON CONFLICT (uuid) DO NOTHING; \
              """
        for _, row in df.iterrows():
            registros.append({
                "uuid": str(row.get("user_id") or uuid.uuid4()),
                # AQUÍ ESTABA EL ERROR: Faltaba mapear el email
                "email": row.get("email") or f"user_{uuid.uuid4().hex[:8]}@ejemplo.com",
                "name": row.get("nombre") or row.get("name") or "Sin Nombre",
                "surname": row.get("apellido") or row.get("surname") or "Sin Apellido",
                "phone_number": str(row.get("telefono") or row.get("phone_number") or "0000000000"),
                "country_code": row.get("country_code") or "MX",
                "currency_code": row.get("currency_code") or "MXN",
                "picture_url": row.get("picture_url") or "",
            })

    elif tabla == "audit_logs":
        sql = """
              INSERT INTO audit_logs (log_id, execution_time, created_at)
              VALUES (%(log_id)s, %(execution_time)s, %(created_at)s)
              ON CONFLICT (log_id) DO NOTHING; \
              """
        for _, row in df.iterrows():
            registros.append({
                "log_id": str(row.get("log_id") or uuid.uuid4()),
                "execution_time": _parse_execution_time(row.get("execution_time")),
                "created_at": row.get("created_at") or datetime.now(timezone.utc).isoformat(),
            })

    elif tabla == "data_sources":
        sql = """
              INSERT INTO data_sources (source_id, source_type, file_name, description)
              VALUES (%(source_id)s, %(source_type)s, %(file_name)s, %(description)s)
              ON CONFLICT (source_id) DO NOTHING; \
              """
        for _, row in df.iterrows():
            registros.append({
                "source_id": str(row.get("source_id") or uuid.uuid4()),
                "source_type": row.get("source_type") or "",
                "file_name": row.get("file_name") or "",
                "description": row.get("description") or "",
            })

    elif tabla == "data_ingestions":
        sql = """
              INSERT INTO data_ingestions (batch_id, source_id, ingestion_date, status)
              VALUES (%(batch_id)s, %(source_id)s, %(ingestion_date)s, %(status)s)
              ON CONFLICT (batch_id) DO NOTHING; \
              """
        for _, row in df.iterrows():
            registros.append({
                "batch_id": str(row.get("batch_id") or uuid.uuid4()),
                "source_id": str(row.get("source_id") or ""),
                "ingestion_date": row.get("ingestion_date") or datetime.now(timezone.utc).isoformat(),
                "status": row.get("status") or "unknown",
            })
    else:
        return 0

    if not registros:
        return 0

    cur.executemany(sql, registros)
    return len(registros)

def _cargar_df_inteligente(archivo: str) -> pd.DataFrame:

    try:
        with open(archivo, 'r', encoding='utf-8') as f:
            primeros_caracteres = f.read(150).strip()

        if primeros_caracteres.startswith('<?xml') or primeros_caracteres.startswith('<'):
            logger.info("👁️ Sniffer: Detectado formato XML por contenido.")
            return pd.read_xml(archivo)

        elif primeros_caracteres.startswith('[') or primeros_caracteres.startswith('{'):
            logger.info("👁️ Sniffer: Detectado formato JSON por contenido.")
            return pd.read_json(archivo)

        else:
            with open(archivo, 'r', encoding='utf-8') as f:
                primera_linea = f.readline()

            if '\t' in primera_linea:
                logger.info("👁️ Sniffer: Detectado formato TXT (Tab-separated).")
                return pd.read_csv(archivo, sep='\t')
            else:
                logger.info("👁️ Sniffer: Detectado formato CSV estándar.")
                return pd.read_csv(archivo)

    except Exception as e:
        logger.error("✖️ Error en _cargar_df_inteligente: %s", e)
        return pd.DataFrame()

def _procesar_archivo_en_memoria(
    cur,
    archivo: str,
    parser: JsonParser,
    carpeta_salida: Path,
) -> None:
    inicio      = datetime.now(timezone.utc)
    id_archivo  = str(uuid.uuid5(uuid.NAMESPACE_URL, archivo))
    nombre_json = _a_nombre_json(archivo)

    df_crudo = _cargar_df_inteligente(archivo)

    if df_crudo is None or df_crudo.empty:
        logger.warning("DataFrame vacío, se omite: %s", archivo)
        return

    df_crudo["id_archivo_origen"] = id_archivo
    logger.info("READ OK — %d filas de '%s'", len(df_crudo), archivo)

    tabla_destino = _determinar_tabla_destino(archivo, df_crudo)
    logger.info("ROUTER → archivo '%s' enrutado a tabla '%s'", Path(archivo).name, tabla_destino)

    schema   = _build_dynamic_schema(df_crudo)
    raw_json = _parsear_a_json(parser, df_crudo, nombre_json, id_archivo)

    if not raw_json.get("datos"):
        logger.warning("Sin registros tras parseo, se omite: %s", archivo)
        return

    logger.info("PARSE OK — %d registros serializados", raw_json["total_registros"])

    cleaner = DataCleaner(
        schema=schema,
        drop_duplicates=True,
        dropna_all=True,
        unique_keys=UNIQUE_KEYS,
    )
    df_limpio = cleaner.process(df_crudo)

    if df_limpio is None or df_limpio.empty:
        logger.warning("DataFrame vacío tras limpieza, se omite: %s", archivo)
        return


    if tabla_destino == "processed_data":
        df_limpio   = _adaptar_transacciones_processed_data(df_limpio)
        json_limpio = _parsear_a_json(parser, df_limpio, nombre_json, id_archivo)
        registros   = json_limpio["datos"]

        if not registros:
            logger.warning("Sin registros limpios para processed_data, se omite: %s", archivo)
            return

        logger.info("CLEAN OK (transacciones) — %d registros listos para carga", len(registros))

        parser.carpeta_salida = carpeta_salida
        destino    = parser._ruta_salida(archivo)
        salida_str = json.dumps(json_limpio, ensure_ascii=False, indent=4, default=str)
        destino.write_text(salida_str, encoding="utf-8")
        logger.info("SAVE OK — %s", destino)

        _, batch_id, total = upload_batch(cur, registros, nombre_json, disable_idx=True)
        logger.info("UPLOAD OK (upload_batch) — %d registros", total)

    elif tabla_destino == "raw_data":
        json_limpio = _parsear_a_json(parser, df_limpio, nombre_json, id_archivo)
        registros   = json_limpio["datos"]

        if not registros:
            logger.warning("Sin registros limpios, se omite: %s", archivo)
            return

        logger.info("CLEAN OK — %d registros listos para carga", len(registros))

        parser.carpeta_salida = carpeta_salida
        destino    = parser._ruta_salida(archivo)
        salida_str = json.dumps(json_limpio, ensure_ascii=False, indent=4, default=str)
        destino.write_text(salida_str, encoding="utf-8")
        logger.info("SAVE OK — %s", destino)

        _, batch_id, total = upload_batch(cur, registros, nombre_json, disable_idx=True)
        logger.info("UPLOAD OK (fallback raw) — %d registros", total)

    else:
        logger.info(
            "CLEAN OK (%s) — %d filas listas para INSERT estructurado",
            tabla_destino, len(df_limpio),
        )
        total = _cargar_tabla_estructurada(cur, df_limpio, tabla_destino)

    elapsed = datetime.now(timezone.utc) - inicio
    logger.info(
        "DONE — '%s' → '%s' | %d registros en %.2fs",
        Path(archivo).name, tabla_destino, total, elapsed.total_seconds(),
    )


def run_pipeline_single_file(
    archivo: str,
    carpeta_salida: Optional[str] = None,
    unique_keys: Optional[List[str]] = None,
) -> None:
    global UNIQUE_KEYS
    UNIQUE_KEYS = unique_keys

    ruta = Path(archivo).resolve()

    if not ruta.exists():
        raise FileNotFoundError(f"El archivo no existe: {ruta}")

    ext = ruta.suffix.lstrip(".").lower()
    extensiones_soportadas = set(BaseReader._registry.keys())
    if ext not in extensiones_soportadas:
        raise ValueError(
            f"Extensión '.{ext}' no soportada. Válidas: {extensiones_soportadas}"
        )

    raiz           = Path(__file__).resolve().parents[2]
    carpeta_salida = Path(carpeta_salida) if carpeta_salida else raiz / "data2"

    parser = JsonParser.__new__(JsonParser)
    parser.carpetas_datos = [str(ruta.parent)]
    parser.carpeta_salida = carpeta_salida

    conn = _get_connection()
    psycopg2.extras.register_uuid()

    try:
        logger.info("▶ Procesando (modo COPY): %s", ruta)
        with conn:
            with conn.cursor() as cur:
                _procesar_archivo_en_memoria(cur, str(ruta), parser, carpeta_salida)
        logger.info("✔ Pipeline completado: %s", ruta.name)
    except Exception as exc:
        logger.error("✖ Pipeline fallido '%s': %s", ruta.name, exc)
        conn.rollback()
        raise
    finally:
        conn.close()


def run_pipeline(
    carpetas_datos: Optional[List[str]] = None,
    carpeta_salida: Optional[str] = None,
    unique_keys: Optional[List[str]] = None,
) -> None:
    logger.warning("run_pipeline() escanea carpetas. Producción → run_pipeline_single_file().")

    global UNIQUE_KEYS
    UNIQUE_KEYS = unique_keys

    raiz           = Path(__file__).resolve().parents[2]
    carpetas_datos = carpetas_datos or [str(raiz / "example")]
    carpeta_salida = Path(carpeta_salida) if carpeta_salida else raiz / "data2"

    archivos = BaseReader.find_files(carpetas_datos, recursive=True)
    if not archivos:
        logger.warning("No se encontraron archivos.")
        return

    parser = JsonParser.__new__(JsonParser)
    parser.carpetas_datos = carpetas_datos
    parser.carpeta_salida = carpeta_salida

    conn = _get_connection()
    psycopg2.extras.register_uuid()

    exitosos = fallidos = 0
    try:
        for n, archivo in enumerate(archivos, start=1):
            print(f"\n{'=' * 30}\nArchivo {n}/{len(archivos)}: {archivo}")
            try:
                with conn:
                    with conn.cursor() as cur:
                        _procesar_archivo_en_memoria(cur, archivo, parser, carpeta_salida)
                exitosos += 1
            except Exception as exc:
                logger.error("Error en '%s': %s", archivo, exc)
                conn.rollback()
                fallidos += 1
    finally:
        conn.close()

    logger.info("Pipeline → Exitosos: %d | Fallidos: %d", exitosos, fallidos)
