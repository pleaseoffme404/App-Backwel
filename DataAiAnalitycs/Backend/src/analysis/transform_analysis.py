import json
import logging
import math
import traceback
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Any, Optional

import pandas as pd
import psycopg2
import psycopg2.extras
from dotenv import load_dotenv

import sys
sys.path.append(str(Path(__file__).resolve().parents[2]))
from data.query_raw_data import RawDataPipeline
from Backend.data.db_connector import Connect_BD


# ── Rango de periodo relativo al máximo dato real en BD ──────────────────────
def _calcular_rango_periodo(
    periodo: str,
    conn=None,
) -> tuple[Optional[str], Optional[str]]:
    """
    Calcula start/end relativo a la fecha máxima real en processed_data,
    no a datetime.now(). Así funciona aunque los datos sean históricos.
    Si no se puede obtener la fecha máxima, cae back a datetime.now().
    """
    if periodo == "todos":
        return None, None

    # Obtener fecha máxima real de los datos
    fecha_max = None
    if conn is not None:
        try:
            with conn.cursor() as cur:
                cur.execute("SELECT MAX(event_date) FROM processed_data")
                row = cur.fetchone()
                if row and row[0]:
                    fecha_max = row[0]
        except Exception as e:
            logger.warning("No se pudo obtener MAX(event_date): %s", e)

    if fecha_max is None:
        fecha_max = datetime.now(timezone.utc)
        logger.warning(
            "_calcular_rango_periodo: usando datetime.now() como fallback para '%s'.", periodo
        )
    else:
        logger.info(
            "_calcular_rango_periodo: fecha_max real en BD = %s (periodo=%s)", fecha_max, periodo
        )

    # Asegurar timezone
    if fecha_max.tzinfo is None:
        fecha_max = fecha_max.replace(tzinfo=timezone.utc)

    fin = fecha_max.replace(hour=23, minute=59, second=59, microsecond=999999)

    deltas = {
        "dia":    timedelta(days=1),
        "semana": timedelta(weeks=1),
        "mes":    timedelta(days=30),
        "año":    timedelta(days=365),
    }

    if periodo not in deltas:
        logger.warning("periodo '%s' no reconocido, usando 'mes' como fallback.", periodo)
        periodo = "mes"

    inicio = fecha_max - deltas[periodo]
    fmt = "%Y-%m-%dT%H:%M:%S+00:00"
    return inicio.strftime(fmt), fin.strftime(fmt)


BASE_DIR = Path(__file__).resolve().parents[3]
load_dotenv(BASE_DIR / ".env")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("transform_analysis")

MODEL_NAME    = "transform_analysis"
MODEL_VERSION = "1.0"

_TEXT_COLUMNS = [
    "category", "type", "status", "product_id", "user_id",
    "name", "description", "label", "product_name", "client_name",
]


def _get_connection():
    conn = Connect_BD.crear_conexion()
    if conn is None:
        raise ConnectionError("No se pudo conectar a la base de datos.")
    return conn


def _sanitizar_texto(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    object_cols = df.select_dtypes(include=["object"]).columns.tolist()
    cols_a_limpiar = set(_TEXT_COLUMNS) | set(object_cols)

    for col in cols_a_limpiar:
        if col not in df.columns:
            continue
        try:
            df[col] = (
                df[col]
                .astype(str)
                .str.encode("utf-8", errors="ignore")
                .str.decode("utf-8")
                .replace("None", None)
                .replace("nan",  None)
            )
        except Exception as e:
            logger.warning("No se pudo sanitizar columna '%s': %s", col, e)

    logger.debug("_sanitizar_texto aplicado — %d columnas procesadas.", len(cols_a_limpiar))
    return df


def _limpiar_payload(obj: Any) -> Any:
    if isinstance(obj, dict):
        return {k: _limpiar_payload(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_limpiar_payload(item) for item in obj]
    if isinstance(obj, float):
        if math.isnan(obj) or math.isinf(obj):
            return None
        return obj
    return obj


def _guardar_resultado(
    cur,
    input_data_id: str,
    analysis_type: str,
    result: dict,
    confidence_score: Optional[float] = None,
    periodo: str = "todos",
):
    lista_datos_limpia = _limpiar_payload(result.get("datos", []))

    payload = {
        "periodo": periodo,
        "datos":   lista_datos_limpia,
    }

    model_version_periodo = f"1.0-{periodo}"

    cur.execute(
        """
        INSERT INTO analysis_results
            (input_data_id, analysis_type, model_name, model_version,
             result, confidence_score)
        VALUES (%s, %s, %s, %s, %s, %s)
        ON CONFLICT (analysis_type, model_name, model_version)
        DO UPDATE SET
            result           = EXCLUDED.result,
            confidence_score = EXCLUDED.confidence_score,
            created_at       = now()
        """,
        (
            input_data_id,
            analysis_type,
            MODEL_NAME,
            model_version_periodo,
            psycopg2.extras.Json(payload),
            confidence_score,
        ),
    )


def _df_to_json(df: pd.DataFrame) -> dict:
    return json.loads(df.to_json(orient="records", date_format="iso", force_ascii=False))


def _preparar_df(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()

    for col in ["amount", "base_price", "value"]:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce").astype("float32")

    if "score" in df.columns:
        df["score"] = pd.to_numeric(df["score"], errors="coerce").astype("float32")

    if "quantity" in df.columns:
        qty = pd.to_numeric(df["quantity"], errors="coerce")
        q_min, q_max = qty.min(), qty.max()
        target_int = (
            "Int16"
            if (pd.notna(q_min) and pd.notna(q_max) and q_min >= -32_768 and q_max <= 32_767)
            else "Int32"
        )
        df["quantity"] = qty.round().astype(target_int)
        logger.debug("quantity → %s  (min=%s, max=%s)", target_int, q_min, q_max)

    df["event_date"] = pd.to_datetime(df["event_date"], errors="coerce", utc=True)
    df["fecha"]      = df["event_date"].dt.date
    df["anio"]       = df["event_date"].dt.year.astype("Int16")
    df["mes"]        = df["event_date"].dt.month.astype("Int8")
    df["mes_nombre"] = df["event_date"].dt.strftime("%Y-%m")

    _CATEGORICAL_COLS = [
        "category", "type", "status", "device",
        "region", "source", "moneda", "mes_nombre",
    ]
    for col in _CATEGORICAL_COLS:
        if col not in df.columns:
            continue
        n_unique = df[col].nunique(dropna=True)
        if n_unique / max(len(df), 1) < 0.50:
            df[col] = df[col].astype("category")
            logger.debug("%-15s → category  (%d valores únicos)", col, n_unique)

    df = _sanitizar_texto(df)

    mem_mb = df.memory_usage(deep=True).sum() / 1024 ** 2
    logger.info(
        "DataFrame preparado — %d filas · %d cols · %.1f MB en RAM.",
        *df.shape, mem_mb,
    )
    return df


# ── Funciones de transformación ───────────────────────────────────────────────

def ventas_por_dia(df: pd.DataFrame) -> pd.DataFrame:
    df_v = df[df["status"] != "Cancelada"].copy()
    r = (
        df_v.groupby("fecha")
        .agg(total_amount=("amount", "sum"), total_quantity=("quantity", "sum"),
             num_transacciones=("record_id", "count"))
        .reset_index().sort_values("fecha")
    )
    r["fecha"] = r["fecha"].astype(str)
    logger.info("ventas_por_dia — %d días.", len(r))
    return r


def ventas_por_mes(df: pd.DataFrame) -> pd.DataFrame:
    df_v = df[df["status"] != "Cancelada"].copy()
    r = (
        df_v.groupby("mes_nombre")
        .agg(total_amount=("amount", "sum"), total_quantity=("quantity", "sum"),
             num_transacciones=("record_id", "count"))
        .reset_index().sort_values("mes_nombre")
    )
    logger.info("ventas_por_mes — %d meses.", len(r))
    return r


def ventas_por_categoria(df: pd.DataFrame) -> pd.DataFrame:
    df_v = df[df["status"] != "Cancelada"].copy()
    r = (
        df_v.groupby("category")
        .agg(total_amount=("amount", "sum"), total_quantity=("quantity", "sum"),
             num_transacciones=("record_id", "count"), ticket_promedio=("amount", "mean"))
        .reset_index().sort_values("total_amount", ascending=False)
    )
    logger.info("ventas_por_categoria — %d categorías.", len(r))
    return r


def ventas_por_tipo(df: pd.DataFrame) -> pd.DataFrame:
    r = (
        df.groupby(["type", "status"])
        .agg(total_amount=("amount", "sum"), total_quantity=("quantity", "sum"),
             num_transacciones=("record_id", "count"))
        .reset_index().sort_values("total_amount", ascending=False)
    )
    logger.info("ventas_por_tipo — %d combinaciones.", len(r))
    return r


def top_productos(df: pd.DataFrame, top_n: int = 10) -> pd.DataFrame:
    df_v = df[df["status"] != "Cancelada"].copy()
    if "data" in df_v.columns:
        mask = df_v["product_id"].isna()
        if mask.any():
            df_v.loc[mask, "product_id"] = df_v.loc[mask, "data"].apply(
                lambda d: d.get("product_id") if isinstance(d, dict) else None
            )
    if "product_id" in df_v.columns:
        df_v = _sanitizar_texto(df_v)
    r = (
        df_v[df_v["product_id"].notna()]
        .groupby("product_id")
        .agg(total_amount=("amount", "sum"), total_quantity=("quantity", "sum"),
             num_transacciones=("record_id", "count"), precio_promedio=("value", "mean"))
        .reset_index().sort_values("total_amount", ascending=False).head(top_n)
    )
    logger.info("top_productos — top %d.", len(r))
    return r


def clientes_frecuentes(df: pd.DataFrame, top_n: int = 10) -> pd.DataFrame:
    df_v = df[df["status"] != "Cancelada"].copy()
    if "data" in df_v.columns:
        mask = df_v["user_id"].isna()
        if mask.any():
            df_v.loc[mask, "user_id"] = df_v.loc[mask, "data"].apply(
                lambda d: d.get("user_id") if isinstance(d, dict) else None
            )
    if "user_id" in df_v.columns:
        df_v = _sanitizar_texto(df_v)
    r = (
        df_v[df_v["user_id"].notna()]
        .groupby("user_id")
        .agg(num_transacciones=("record_id", "count"), total_amount=("amount", "sum"),
             total_quantity=("quantity", "sum"), primera_compra=("event_date", "min"),
             ultima_compra=("event_date", "max"))
        .reset_index().sort_values("num_transacciones", ascending=False).head(top_n)
    )
    r["primera_compra"] = r["primera_compra"].astype(str)
    r["ultima_compra"]  = r["ultima_compra"].astype(str)
    logger.info("clientes_frecuentes — top %d.", len(r))
    return r


def ticket_promedio(df: pd.DataFrame) -> pd.DataFrame:
    df_v = df[(df["status"] != "Cancelada") & (df["amount"] > 0)].copy()
    por_cat = (
        df_v.groupby("category")
        .agg(ticket_promedio=("amount", "mean"), total_transacciones=("record_id", "count"),
             amount_total=("amount", "sum"))
        .reset_index().sort_values("ticket_promedio", ascending=False)
    )
    global_row = pd.DataFrame([{
        "category":            "GLOBAL",
        "ticket_promedio":     df_v["amount"].mean(),
        "total_transacciones": len(df_v),
        "amount_total":        df_v["amount"].sum(),
    }])
    r = pd.concat([global_row, por_cat], ignore_index=True)
    logger.info("ticket_promedio — calculado.")
    return r


def resumen_ingesta(df_ingestions: pd.DataFrame) -> pd.DataFrame:
    r = (
        df_ingestions.groupby("status")
        .agg(total_batches=("batch_id", "count"), total_registros=("record_count", "sum"))
        .reset_index()
    )
    logger.info("resumen_ingesta — %d estados.", len(r))
    return r


# ── Clase principal ───────────────────────────────────────────────────────────

class TransformAnalysis:

    TRANSFORMACIONES = [
        "ventas_por_dia", "ventas_por_mes", "ventas_por_categoria",
        "ventas_por_tipo", "top_productos", "clientes_frecuentes",
        "ticket_promedio", "resumen_ingesta",
    ]

    def __init__(
        self,
        start_date: Optional[str] = None,
        end_date:   Optional[str] = None,
        periodo:    str           = "todos",
    ):
        self.start_date = start_date
        self.end_date   = end_date
        self.periodo    = (periodo or "todos").lower().strip()
        self.resultados: dict[str, pd.DataFrame] = {}

    def run(self) -> dict[str, pd.DataFrame]:
        inicio = datetime.now(timezone.utc)
        logger.info("=" * 55)
        logger.info("Iniciando Transform Analysis (periodo=%s)...", self.periodo)

        start = self.start_date
        end   = self.end_date

        # ── Calcular rango relativo al máximo dato real en BD ─────────────
        if start is None and end is None:
            # Abrimos conexión temporal solo para obtener MAX(event_date)
            conn_temp = _get_connection()
            try:
                start, end = _calcular_rango_periodo(self.periodo, conn=conn_temp)
            finally:
                Connect_BD.cerrar_conexion(conn_temp)
            logger.info(
                "Rango calculado para '%s': %s → %s",
                self.periodo, start, end or "sin límite superior",
            )

        # ── Extracción ────────────────────────────────────────────────────
        with RawDataPipeline() as pipeline:
            datasets = pipeline.run(
                query="all",
                start_date=start,
                end_date=end,
            )

        df_raw        = datasets["processed_data"]
        df_ingestions = datasets["ingestions"]

        # ── Log de filas obtenidas (clave para diagnosticar periodos vacíos)
        logger.info(
            "processed_data filtrado (periodo=%s): %d filas | start=%s | end=%s",
            self.periodo, len(df_raw), start, end,
        )

        if df_raw.empty:
            logger.warning(
                "No hay datos en processed_data para el periodo '%s' "
                "(start=%s | end=%s). No se generarán transformaciones.",
                self.periodo, start, end,
            )
            return {}

        input_data_id = str(df_raw["record_id"].iloc[0])
        df = _preparar_df(df_raw)

        transformaciones_map = {
            "ventas_por_dia":      lambda: ventas_por_dia(df),
            "ventas_por_mes":      lambda: ventas_por_mes(df),
            "ventas_por_categoria":lambda: ventas_por_categoria(df),
            "ventas_por_tipo":     lambda: ventas_por_tipo(df),
            "top_productos":       lambda: top_productos(df),
            "clientes_frecuentes": lambda: clientes_frecuentes(df),
            "ticket_promedio":     lambda: ticket_promedio(df),
            "resumen_ingesta":     lambda: resumen_ingesta(df_ingestions),
        }

        conn = _get_connection()
        try:
            with conn:
                with conn.cursor() as cur:
                    psycopg2.extras.register_uuid()
                    for nombre, fn in transformaciones_map.items():
                        # Savepoint por transformación: si una falla,
                        # las anteriores no se pierden
                        cur.execute("SAVEPOINT sp_transform")
                        try:
                            df_resultado = fn()
                            self.resultados[nombre] = df_resultado
                            _guardar_resultado(
                                cur=cur,
                                input_data_id=input_data_id,
                                analysis_type=nombre,
                                result={"datos": _df_to_json(df_resultado)},
                                periodo=self.periodo,
                            )
                            logger.info("  ✓ %-25s → %d filas.", nombre, len(df_resultado))
                        except Exception as e:
                            # Revertir solo esta transformación, no todas
                            cur.execute("ROLLBACK TO SAVEPOINT sp_transform")
                            logger.error(
                                "  ✗ Error en '%s': %s\n%s",
                                nombre, e, traceback.format_exc(),
                            )
        finally:
            Connect_BD.cerrar_conexion(conn)

        elapsed = (datetime.now(timezone.utc) - inicio).total_seconds()
        logger.info("Transform Analysis completado en %.2fs.", elapsed)
        logger.info("=" * 55)
        return self.resultados

