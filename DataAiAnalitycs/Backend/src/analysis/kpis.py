import json
import logging
import math
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Optional

import pandas as pd
import psycopg2
import psycopg2.extras
from dotenv import load_dotenv
from sqlalchemy import create_engine, text

def _find_dotenv() -> Path:
    current = Path(__file__).resolve().parent
    for _ in range(6):
        candidate = current / ".env"
        if candidate.exists():
            return candidate
        current = current.parent
    return Path(__file__).resolve().parents[3] / ".env"

load_dotenv(_find_dotenv())

host     = os.getenv("host")
database = os.getenv("database")
user     = os.getenv("user")
password = os.getenv("password")
port     = os.getenv("port", "5432")

DB_URI = f"postgresql://{user}:{password}@{host}:{port}/{database}"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger        = logging.getLogger("kpis_engine")
MODEL_NAME    = "kpis_engine"
MODEL_VERSION = "1.0"
SEPARADOR     = "=" * 60


def _get_connection():
    from Backend.data.db_connector import Connect_BD
    conn = Connect_BD.crear_conexion()
    if conn is None:
        raise ConnectionError("No se pudo conectar a la base de datos.")
    return conn


def _leer_analysis_result(
    engine,
    analysis_type: str,
    periodo: str = "todos",
) -> Optional[pd.DataFrame]:

    model_version_periodo = f"1.0-{periodo}"
    query = text("""
        SELECT result
        FROM   analysis_results
        WHERE  analysis_type  = :tipo
          AND  model_name     = 'transform_analysis'
          AND  model_version  = :version
        ORDER  BY created_at DESC
        LIMIT  1
    """)
    with engine.connect() as conn:
        row = conn.execute(query, {"tipo": analysis_type, "version": model_version_periodo}).fetchone()
    if row is None:
        logger.warning(
            "No se encontró '%s' (version=%s) en analysis_results.",
            analysis_type, model_version_periodo,
        )
        return None
    datos = row[0].get("datos", [])
    return pd.DataFrame(datos) if datos else None


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


def _guardar_kpi(
    cur,
    input_data_id: str,
    kpi_type: str,
    result: dict,
    periodo: str = "todos",
):

    result_limpio = _limpiar_payload(result)

    payload = {"periodo": periodo, **result_limpio}

    model_version_periodo = f"1.0-{periodo}"

    cur.execute(
        """
        INSERT INTO analysis_results
            (input_data_id, analysis_type, model_name, model_version, result)
        VALUES (%s, %s, %s, %s, %s)
        ON CONFLICT (analysis_type, model_name, model_version)
        DO UPDATE SET
            result     = EXCLUDED.result,
            created_at = now()
        """,
        (
            input_data_id,
            kpi_type,
            MODEL_NAME,
            model_version_periodo,
            psycopg2.extras.Json(payload),
        ),
    )


def _safe_float(valor) -> Optional[float]:
    try:
        result = round(float(valor), 2) if valor is not None else None
        if result is not None and (math.isnan(result) or math.isinf(result)):
            return None
        return result
    except (ValueError, TypeError):
        return None


def _crecimiento(anterior, actual) -> Optional[str]:
    try:
        a, b = float(anterior), float(actual)
        if a == 0:
            return "N/A (base cero)"
        pct   = ((b - a) / abs(a)) * 100
        signo = "+" if pct >= 0 else ""
        return f"{signo}{pct:.1f}%"
    except (TypeError, ValueError):
        return None

def calcular_kpi_ventas(engine, periodo: str = "todos") -> dict:
    df_mes = _leer_analysis_result(engine, "ventas_por_mes",    periodo)
    df_dia = _leer_analysis_result(engine, "ventas_por_dia",    periodo)
    df_tk  = _leer_analysis_result(engine, "ticket_promedio",   periodo)
    kpi    = {}

    if df_mes is not None and not df_mes.empty:
        df_mes["total_amount"]      = pd.to_numeric(df_mes["total_amount"],      errors="coerce")
        df_mes["num_transacciones"] = pd.to_numeric(df_mes["num_transacciones"], errors="coerce")
        kpi["total_ventas"]         = _safe_float(df_mes["total_amount"].sum())
        kpi["total_transacciones"]  = int(df_mes["num_transacciones"].sum())
        idx_mejor = df_mes["total_amount"].idxmax()
        idx_peor  = df_mes["total_amount"].idxmin()
        kpi["mejor_mes"] = {
            "periodo": str(df_mes.loc[idx_mejor, "mes_nombre"]),
            "amount":  _safe_float(df_mes.loc[idx_mejor, "total_amount"]),
        }
        kpi["peor_mes"] = {
            "periodo": str(df_mes.loc[idx_peor, "mes_nombre"]),
            "amount":  _safe_float(df_mes.loc[idx_peor, "total_amount"]),
        }
        if len(df_mes) >= 2:
            df_s = df_mes.sort_values("mes_nombre")
            kpi["crecimiento_mensual"] = _crecimiento(
                df_s["total_amount"].iloc[0],
                df_s["total_amount"].iloc[-1],
            )
        kpi["ventas_por_mes"] = df_mes.to_dict(orient="records")

    if df_dia is not None and not df_dia.empty:
        df_dia["total_amount"]        = pd.to_numeric(df_dia["total_amount"], errors="coerce")
        kpi["promedio_ventas_diario"] = _safe_float(df_dia["total_amount"].mean())
        kpi["dia_pico"] = {
            "fecha":  str(df_dia.loc[df_dia["total_amount"].idxmax(), "fecha"]),
            "amount": _safe_float(df_dia["total_amount"].max()),
        }

    if df_tk is not None and not df_tk.empty:
        df_tk["ticket_promedio"] = pd.to_numeric(df_tk["ticket_promedio"], errors="coerce")
        global_row = df_tk[df_tk["category"] == "GLOBAL"]
        if not global_row.empty:
            kpi["ticket_promedio_global"] = _safe_float(global_row["ticket_promedio"].iloc[0])

    logger.info("kpi_ventas calculado.")
    return kpi


def calcular_kpi_productos(engine, periodo: str = "todos") -> dict:
    df  = _leer_analysis_result(engine, "top_productos", periodo)
    kpi = {}
    if df is not None and not df.empty:
        df["total_amount"]      = pd.to_numeric(df["total_amount"],      errors="coerce")
        df["total_quantity"]    = pd.to_numeric(df["total_quantity"],    errors="coerce")
        df["num_transacciones"] = pd.to_numeric(df["num_transacciones"], errors="coerce")
        kpi["total_productos_distintos"] = len(df)
        idx_a = df["total_amount"].idxmax()
        kpi["producto_mas_vendido_amount"] = {
            "product_id":     str(df.loc[idx_a, "product_id"]),
            "total_amount":   _safe_float(df.loc[idx_a, "total_amount"]),
            "total_quantity": _safe_float(df.loc[idx_a, "total_quantity"]),
        }
        idx_q = df["total_quantity"].idxmax()
        kpi["producto_mas_vendido_cantidad"] = {
            "product_id":    str(df.loc[idx_q, "product_id"]),
            "total_quantity":_safe_float(df.loc[idx_q, "total_quantity"]),
        }
        idx_min = df["total_amount"].idxmin()
        kpi["producto_menos_vendido"] = {
            "product_id":  str(df.loc[idx_min, "product_id"]),
            "total_amount":_safe_float(df.loc[idx_min, "total_amount"]),
        }
        kpi["rotacion_promedio_cantidad"] = _safe_float(df["total_quantity"].mean())
        kpi["detalle_productos"] = df.to_dict(orient="records")
    logger.info("kpi_productos calculado.")
    return kpi


def calcular_kpi_clientes(engine, periodo: str = "todos") -> dict:
    df  = _leer_analysis_result(engine, "clientes_frecuentes", periodo)
    kpi = {}
    if df is not None and not df.empty:
        df["num_transacciones"] = pd.to_numeric(df["num_transacciones"], errors="coerce")
        df["total_amount"]      = pd.to_numeric(df["total_amount"],      errors="coerce")
        total       = len(df)
        recurrentes = len(df[df["num_transacciones"] > 1])
        kpi["total_clientes_unicos"]      = total
        kpi["clientes_recurrentes"]       = recurrentes
        kpi["clientes_nuevos"]            = total - recurrentes
        kpi["tasa_recurrencia"]           = f"{(recurrentes / total * 100):.1f}%" if total > 0 else "0%"
        kpi["frecuencia_compra_promedio"] = _safe_float(df["num_transacciones"].mean())
        idx_top = df["total_amount"].idxmax()
        kpi["cliente_top"] = {
            "user_id":           str(df.loc[idx_top, "user_id"]),
            "total_amount":      _safe_float(df.loc[idx_top, "total_amount"]),
            "num_transacciones": int(df.loc[idx_top, "num_transacciones"]),
        }
        kpi["detalle_clientes"] = df.to_dict(orient="records")
    logger.info("kpi_clientes calculado.")
    return kpi


def calcular_kpi_facturacion(engine, periodo: str = "todos") -> dict:
    df  = _leer_analysis_result(engine, "ventas_por_tipo", periodo)
    kpi = {}
    if df is not None and not df.empty:
        df["total_amount"]      = pd.to_numeric(df["total_amount"],      errors="coerce")
        df["num_transacciones"] = pd.to_numeric(df["num_transacciones"], errors="coerce")
        if "status" in df.columns:
            for status in ["Emitida", "Cancelada", "Pendiente"]:
                filas = df[df["status"] == status]
                clave = status.lower()
                kpi[f"total_{clave}s"]  = int(filas["num_transacciones"].sum()) if not filas.empty else 0
                kpi[f"ingreso_{clave}"] = _safe_float(filas["total_amount"].sum()) if not filas.empty else 0.0
        if "type" in df.columns:
            por_tipo = (
                df.groupby("type")["total_amount"].sum().reset_index()
                .rename(columns={"total_amount": "ingreso_total"})
            )
            kpi["ingreso_por_tipo"] = por_tipo.to_dict(orient="records")
        kpi["detalle_facturacion"] = df.to_dict(orient="records")
    logger.info("kpi_facturacion calculado.")
    return kpi


class KpisEngine:

    KPI_TYPES = ["kpi_ventas", "kpi_productos", "kpi_clientes", "kpi_facturacion"]

    def __init__(self, periodo: str = "todos"):
        self.resultados: dict = {}
        self.engine  = create_engine(DB_URI)
        self.periodo = (periodo or "todos").lower().strip()

    def run(self) -> dict:
        inicio = datetime.now(timezone.utc)
        logger.info("=" * 55)
        logger.info("Iniciando KPIs Engine (periodo=%s)...", self.periodo)

        with self.engine.connect() as conn:
            row = conn.execute(text("SELECT record_id FROM processed_data LIMIT 1")).fetchone()

        # Si no hay datos en la tabla, detenemos la ejecución de los KPIs
        if not row:
            logger.warning("No hay registros en processed_data. Saltando cálculo de KPIs.")
            return self.resultados

        # Si sí hay datos, extraemos el UUID real
        input_data_id = str(row[0])
        calculadores = {
            "kpi_ventas": lambda: calcular_kpi_ventas(self.engine, self.periodo),
            "kpi_productos": lambda: calcular_kpi_productos(self.engine, self.periodo),
            "kpi_clientes": lambda: calcular_kpi_clientes(self.engine, self.periodo),
            "kpi_facturacion": lambda: calcular_kpi_facturacion(self.engine, self.periodo),
        }

        conn_pg = _get_connection()
        try:
            with conn_pg:
                with conn_pg.cursor() as cur:
                    psycopg2.extras.register_uuid()
                    for kpi_type, fn in calculadores.items():
                        try:
                            resultado = fn()
                            self.resultados[kpi_type] = resultado
                            _guardar_kpi(cur, input_data_id, kpi_type,
                                         resultado, periodo=self.periodo)
                            logger.info("  ✓ %-20s guardado.", kpi_type)
                        except Exception as e:
                            cur.execute("ROLLBACK TO SAVEPOINT sp_kpi")  # ← blindaje: limpia la transacción abortada
                            logger.error("  ✗ Error en '%s': %s", kpi_type, e)
        finally:
            from Backend.data.db_connector import Connect_BD
            Connect_BD.cerrar_conexion(conn_pg)

        elapsed = (datetime.now(timezone.utc) - inicio).total_seconds()
        logger.info("KPIs Engine completado en %.2fs.", elapsed)
        logger.info("=" * 55)
        self._imprimir_resumen()
        return self.resultados

    def _imprimir_resumen(self):
        print(f"\n{SEPARADOR}")
        print("  RESUMEN DE KPIs")
        print(SEPARADOR)
        v = self.resultados.get("kpi_ventas", {})
        if v:
            print("\n  VENTAS")
            print(f"  • Total ventas:        ${v.get('total_ventas') or 0:,.2f}")
            print(f"  • Total transacciones: {v.get('total_transacciones', 0)}")
            print(f"  • Ticket promedio:     ${v.get('ticket_promedio_global') or 0:,.2f}")
            print(f"  • Promedio diario:     ${v.get('promedio_ventas_diario') or 0:,.2f}")
            if "mejor_mes" in v:
                print(f"  • Mejor mes:           {v['mejor_mes']['periodo']} (${v['mejor_mes']['amount'] or 0:,.2f})")
            if "crecimiento_mensual" in v:
                print(f"  • Crecimiento mensual: {v['crecimiento_mensual']}")
            if "dia_pico" in v:
                print(f"  • Día pico:            {v['dia_pico']['fecha']} (${v['dia_pico']['amount'] or 0:,.2f})")
        p = self.resultados.get("kpi_productos", {})
        if p:
            print("\n  PRODUCTOS")
            print(f"  • Productos distintos: {p.get('total_productos_distintos', 0)}")
            if "producto_mas_vendido_amount" in p:
                pm = p["producto_mas_vendido_amount"]
                print(f"  • Más vendido (amt):   {pm['product_id']} (${pm['total_amount'] or 0:,.2f})")
            print(f"  • Rotación promedio:   {p.get('rotacion_promedio_cantidad') or 0:.2f} uds")
        c = self.resultados.get("kpi_clientes", {})
        if c:
            print("\n  CLIENTES")
            print(f"  • Total únicos:        {c.get('total_clientes_unicos', 0)}")
            print(f"  • Recurrentes:         {c.get('clientes_recurrentes', 0)}")
            print(f"  • Tasa recurrencia:    {c.get('tasa_recurrencia', '0%')}")
        f = self.resultados.get("kpi_facturacion", {})
        if f:
            print("\n  FACTURACIÓN")
            print(f"  • Emitidas:   {f.get('total_emitidas', 0)} txns (${f.get('ingreso_emitida') or 0:,.2f})")
            print(f"  • Canceladas: {f.get('total_canceladas', 0)} txns (${f.get('ingreso_cancelada') or 0:,.2f})")
        print(f"\n{SEPARADOR}\n")


