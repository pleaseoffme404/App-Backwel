import logging
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Optional
import pandas as pd
import psycopg2
import psycopg2.extras
from dotenv import load_dotenv
import sys
sys.path.append(str(Path(__file__).resolve().parents[2]))
from data.query_raw_data import RawDataPipeline
from Backend.data.db_connector import Connect_BD

BASE_DIR = Path(__file__).resolve().parents[3]
load_dotenv(BASE_DIR / ".env")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("anomaly_detection")

SEPARADOR = "=" * 60

SCORE_UMBRAL        = 0.85
STD_MULTIPLIER      = 3.0
DIAS_PENDIENTE_MAX  = 7
RATIO_TOLERANCIA    = 1.1


def _get_connection():
    conn = Connect_BD.crear_conexion()
    if conn is None:
        raise ConnectionError("No se pudo conectar a la base de datos.")
    return conn


def _marcar_anomalo(cur, record_id: str):
    cur.execute(
        """
        UPDATE processed_data
        SET status     = 'anomalo',
            updated_at = now()
        WHERE record_id = %s
        """,
        (record_id,),
    )


def _registrar_audit_log(
    cur,
    record_id:    str,
    regla:        str,
    descripcion:  str,
    metadata:     dict,
):
    cur.execute(
        """
        INSERT INTO audit_logs
            (process_name, log_level, record_id, error_message, metadata)
        VALUES (%s, 'WARNING', %s, %s, %s)
        """,
        (
            f"anomaly_detection:{regla}",
            record_id,
            descripcion,
            psycopg2.extras.Json(metadata),
        ),
    )


def _preparar_df(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    for col in ["amount", "quantity", "value", "score"]:
        df[col] = pd.to_numeric(df[col], errors="coerce")
    df["event_date"] = pd.to_datetime(df["event_date"], errors="coerce", utc=True)
    return df


def r01_precio_cero(df: pd.DataFrame) -> pd.DataFrame:

    mask = (
        (df["amount"].fillna(0) == 0) | (df["value"].fillna(0) == 0)
    ) & (
        ~df["status"].isin(["Cancelada", "anomalo"])
    ) & (
        df["type"] != "Devolución"
    )
    resultado = df[mask].copy()
    resultado["regla"]       = "R01"
    resultado["descripcion"] = "Precio o monto en cero en transacción activa"
    logger.info("R01 precio_cero — %d anomalías.", len(resultado))
    return resultado


def r02_amount_anormal(df: pd.DataFrame) -> pd.DataFrame:

    serie = df["amount"].dropna()
    if len(serie) < 3:
        return pd.DataFrame()

    media = serie.mean()
    std   = serie.std()
    umbral = media + STD_MULTIPLIER * std

    mask = df["amount"] > umbral
    resultado = df[mask].copy()
    resultado["regla"]       = "R02"
    resultado["descripcion"] = (
        f"Amount anormalmente alto (>{umbral:,.2f} = media+{STD_MULTIPLIER}σ)"
    )
    logger.info("R02 amount_anormal — %d anomalías (umbral: %.2f).", len(resultado), umbral)
    return resultado


def r03_cantidad_negativa(df: pd.DataFrame) -> pd.DataFrame:

    mask = (df["quantity"] < 0) & (df["type"] != "Devolución")
    resultado = df[mask].copy()
    resultado["regla"]       = "R03"
    resultado["descripcion"] = "Cantidad negativa en transacción que no es Devolución"
    logger.info("R03 cantidad_negativa — %d anomalías.", len(resultado))
    return resultado


def r04_score_sospechoso(df: pd.DataFrame) -> pd.DataFrame:

    mask = df["score"].fillna(0) > SCORE_UMBRAL
    resultado = df[mask].copy()
    resultado["regla"]       = "R04"
    resultado["descripcion"] = f"Score de riesgo alto (>{SCORE_UMBRAL})"
    logger.info("R04 score_sospechoso — %d anomalías.", len(resultado))
    return resultado


def r05_ratio_inconsistente(df: pd.DataFrame) -> pd.DataFrame:

    df_valido = df[(df["value"] > 0) & (df["quantity"] > 0)].copy()
    df_valido["amount_esperado"] = df_valido["value"] * df_valido["quantity"]
    mask = df_valido["amount"] > df_valido["amount_esperado"] * RATIO_TOLERANCIA

    resultado = df_valido[mask].copy()
    resultado["regla"]       = "R05"
    resultado["descripcion"] = (
        f"Amount ({RATIO_TOLERANCIA}x) inconsistente con value × quantity"
    )
    logger.info("R05 ratio_inconsistente — %d anomalías.", len(resultado))
    return resultado


def r06_pendiente_viejo(df: pd.DataFrame) -> pd.DataFrame:

    ahora  = datetime.now(tz=timezone.utc)
    limite = ahora - timedelta(days=DIAS_PENDIENTE_MAX)

    mask = (
        (df["status"] == "Pendiente") &
        (df["event_date"].notna()) &
        (df["event_date"] < limite)
    )
    resultado = df[mask].copy()
    resultado["regla"]       = "R06"
    resultado["descripcion"] = (
        f"Transacción en Pendiente por más de {DIAS_PENDIENTE_MAX} días"
    )
    logger.info("R06 pendiente_viejo — %d anomalías.", len(resultado))
    return resultado


def r07_duplicados(df: pd.DataFrame) -> pd.DataFrame:

    df_work = df.copy()

    if "data" in df_work.columns:
        for campo in ("user_id", "product_id"):
            mask_nulo = df_work[campo].isna()
            if mask_nulo.any():
                df_work.loc[mask_nulo, campo] = df_work.loc[mask_nulo, "data"].apply(
                    lambda d: d.get(campo) if isinstance(d, dict) else None
                )

    cols_dup = ["user_id", "product_id", "event_date"]
    if not all(c in df_work.columns for c in cols_dup):
        return pd.DataFrame()

    df_check = df_work.dropna(subset=cols_dup)
    duplicados = df_check[df_check.duplicated(subset=cols_dup, keep=False)]

    resultado = duplicados.copy()
    resultado["regla"]       = "R07"
    resultado["descripcion"] = "Registro duplicado: mismo user_id + product_id + event_date"
    logger.info("R07 duplicados — %d anomalías.", len(resultado))
    return resultado

class AnomalyDetector:

    REGLAS = [
        ("R01", r01_precio_cero),
        ("R02", r02_amount_anormal),
        ("R03", r03_cantidad_negativa),
        ("R04", r04_score_sospechoso),
        ("R05", r05_ratio_inconsistente),
        ("R06", r06_pendiente_viejo),
        ("R07", r07_duplicados),
    ]

    def __init__(
        self,
        start_date: Optional[str] = None,
        end_date:   Optional[str] = None,
    ):
        self.start_date = start_date
        self.end_date   = end_date
        self.resultados: dict = {}

    def run(self) -> dict:
        inicio = datetime.now(timezone.utc)
        logger.info("=" * 55)
        logger.info("Iniciando Anomaly Detection...")

        with RawDataPipeline() as pipeline:
            datasets = pipeline.run(
                query="processed_data",
                start_date=self.start_date,
                end_date=self.end_date,
            )

        df_raw = datasets["processed_data"]

        if df_raw.empty:
            logger.warning("No hay datos en processed_data para analizar.")
            return {}

        df = _preparar_df(df_raw)

        frames_anomalias = []
        resumen_por_regla = {}

        for codigo, fn_regla in self.REGLAS:
            try:
                df_anomalias = fn_regla(df)
                if df_anomalias is not None and not df_anomalias.empty:
                    frames_anomalias.append(df_anomalias)
                    resumen_por_regla[codigo] = len(df_anomalias)
                else:
                    resumen_por_regla[codigo] = 0
            except Exception as e:
                logger.error("Error en regla %s: %s", codigo, e)
                resumen_por_regla[codigo] = 0

        if not frames_anomalias:
            logger.info("No se detectaron anomalías.")
            self.resultados = {
                "anomalias_total": 0,
                "por_regla": resumen_por_regla,
                "detalle": pd.DataFrame(),
            }
            return self.resultados

        df_todas = pd.concat(frames_anomalias, ignore_index=True)

        conn = _get_connection()
        try:
            with conn:
                with conn.cursor() as cur:
                    psycopg2.extras.register_uuid()

                    procesados = set()
                    for _, fila in df_todas.iterrows():
                        record_id   = str(fila["record_id"])
                        regla       = str(fila.get("regla", "UNKNOWN"))
                        descripcion = str(fila.get("descripcion", "Anomalía detectada"))

                        metadata = {
                            "regla":      regla,
                            "amount":     float(fila["amount"]) if pd.notna(fila.get("amount")) else None,
                            "quantity":   float(fila["quantity"]) if pd.notna(fila.get("quantity")) else None,
                            "value":      float(fila["value"]) if pd.notna(fila.get("value")) else None,
                            "score":      float(fila["score"]) if pd.notna(fila.get("score")) else None,
                            "status":     str(fila.get("status", "")),
                            "type":       str(fila.get("type", "")),
                            "category":   str(fila.get("category", "")),
                            "event_date": str(fila.get("event_date", "")),
                        }

                        if record_id not in procesados:
                            _marcar_anomalo(cur, record_id)
                            procesados.add(record_id)

                        _registrar_audit_log(cur, record_id, regla, descripcion, metadata)

            logger.info(
                "%d anomalías procesadas (%d registros únicos marcados).",
                len(df_todas), len(procesados),
            )

        finally:
            Connect_BD.cerrar_conexion(conn)

        self.resultados = {
            "anomalias_total":   len(df_todas),
            "registros_unicos":  len(procesados),
            "por_regla":         resumen_por_regla,
            "detalle":           df_todas,
        }

        elapsed = (datetime.now(timezone.utc) - inicio).total_seconds()
        logger.info("Anomaly Detection completado en %.2fs.", elapsed)
        logger.info("=" * 55)

        self._imprimir_resumen()
        return self.resultados

    def _imprimir_resumen(self):
        print(f"\n{SEPARADOR}")
        print("  DETECCIÓN DE ANOMALÍAS")
        print(SEPARADOR)

        total    = self.resultados.get("anomalias_total", 0)
        unicos   = self.resultados.get("registros_unicos", 0)
        por_regla = self.resultados.get("por_regla", {})

        print(f"\n  Total anomalías detectadas:  {total}")
        print(f"  Registros únicos marcados:   {unicos}")
        print("\n  Por regla:")

        descripciones = {
            "R01": "Precio / monto en cero",
            "R02": "Amount estadísticamente anormal",
            "R03": "Cantidad negativa (no devolución)",
            "R04": "Score de riesgo alto",
            "R05": "Amount inconsistente con value×qty",
            "R06": "Pendiente por más de N días",
            "R07": "Registros duplicados",
        }

        for codigo, cantidad in por_regla.items():
            desc = descripciones.get(codigo, "")
            flag = "⚠" if cantidad > 0 else "✓"
            print(f"  {flag} {codigo} — {desc:<38} {cantidad} registros")

        # Detalle de anomalías encontradas
        df = self.resultados.get("detalle")
        if df is not None and not df.empty:
            print(f"\n  Detalle:")
            cols = ["record_id", "regla", "type", "status", "amount", "value", "quantity", "score", "event_date"]
            cols_disponibles = [c for c in cols if c in df.columns]
            print(df[cols_disponibles].to_string(index=False))

        print(f"\n{SEPARADOR}\n")


