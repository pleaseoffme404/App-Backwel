import csv
import json
import logging
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

import pandas as pd
from dotenv import load_dotenv
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError



def _find_dotenv() -> Path:
    current = Path(__file__).resolve().parent
    for _ in range(6):
        candidate = current / ".env"
        if candidate.exists():
            return candidate
        current = current.parent
    return Path(__file__).resolve().parents[3] / ".env"


load_dotenv(override=False)

host = os.getenv("host")
database = os.getenv("database")
user = os.getenv("user")
password = os.getenv("password")
port = os.getenv("port", "5432")

DB_URI = f"postgresql://{user}:{password}@{host}:{port}/{database}"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("reports")

SEPARADOR = "=" * 60
REPORTS_DIR = Path(__file__).resolve().parent / "reportes"
REPORTS_DIR.mkdir(parents=True, exist_ok=True)

ANALYSIS_TYPES = [
    "ventas_por_dia",
    "ventas_por_mes",
    "ventas_por_categoria",
    "ventas_por_tipo",
    "top_productos",
    "clientes_frecuentes",
    "ticket_promedio",
    "resumen_ingesta",
    "kpi_ventas",
    "kpi_productos",
    "kpi_clientes",
    "kpi_facturacion",
]


def _timestamp() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")


def _leer_analysis_result(engine, analysis_type: str, periodo: str = None) -> Optional[dict]:
    if periodo and periodo != "todos":
        query = text("""
                     SELECT result, model_name, created_at
                     FROM analysis_results
                     WHERE analysis_type = :tipo
                       AND result ->>'periodo' = :periodo
                     ORDER BY created_at DESC
                         LIMIT 1
                     """)
        params = {"tipo": analysis_type, "periodo": periodo}
    else:
        query = text("""
                     SELECT result, model_name, created_at
                     FROM analysis_results
                     WHERE analysis_type = :tipo
                     ORDER BY created_at DESC LIMIT 1
                     """)
        params = {"tipo": analysis_type}

    with engine.connect() as conn:
        row = conn.execute(query, params).fetchone()

    if row is None:
        logger.warning("No se encontró '%s' (periodo: %s) en analysis_results.", analysis_type, periodo)
        return None

    return {
        "analysis_type": analysis_type,
        "model_name": row[1],
        "created_at": str(row[2]),
        "result": row[0],
    }


def _result_to_df(raw: dict) -> pd.DataFrame:
    datos = raw.get("result", {}).get("datos", [])
    if not datos:
        datos = raw.get("result", {})
        if isinstance(datos, dict):
            return pd.DataFrame([datos])
    return pd.DataFrame(datos) if datos else pd.DataFrame()


def _nombre_archivo(analysis_type: str, extension: str, periodo: str = None) -> Path:
    prefijo = f"{analysis_type}_{periodo}" if periodo and periodo != "todos" else analysis_type
    return REPORTS_DIR / f"{prefijo}_{_timestamp()}.{extension}"

def generar_csv(df: pd.DataFrame, analysis_type: str, periodo: str = None) -> Path:
    ruta = _nombre_archivo(analysis_type, "csv", periodo)
    df.to_csv(ruta, index=False, encoding="utf-8-sig")
    logger.info("CSV generado → %s", ruta)
    return ruta


def generar_json(raw: dict, analysis_type: str, periodo: str = None) -> Path:
    ruta = _nombre_archivo(analysis_type, "json", periodo)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(raw, f, ensure_ascii=False, indent=2, default=str)
    logger.info("JSON generado → %s", ruta)
    return ruta


def generar_pdf(df: pd.DataFrame, analysis_type: str, metadata: dict, periodo: str = None) -> Path:
    try:
        from reportlab.lib import colors
        from reportlab.lib.pagesizes import A4, landscape
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
        from reportlab.lib.units import cm
        from reportlab.platypus import (
            SimpleDocTemplate, Table, TableStyle,
            Paragraph, Spacer, HRFlowable,
        )
    except ImportError:
        logger.warning("reportlab no encontrado. Instalando...")
        import subprocess, sys
        subprocess.check_call([sys.executable, "-m", "pip", "install", "reportlab", "-q"])
        from reportlab.lib import colors
        from reportlab.lib.pagesizes import A4, landscape
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
        from reportlab.lib.units import cm
        from reportlab.platypus import (
            SimpleDocTemplate, Table, TableStyle,
            Paragraph, Spacer, HRFlowable,
        )

    ruta = _nombre_archivo(analysis_type, "pdf", periodo)

    pagesize = landscape(A4) if len(df.columns) > 6 else A4
    doc = SimpleDocTemplate(
        str(ruta),
        pagesize=pagesize,
        leftMargin=1.5 * cm,
        rightMargin=1.5 * cm,
        topMargin=2 * cm,
        bottomMargin=2 * cm,
    )

    styles = getSampleStyleSheet()
    titulo_style = ParagraphStyle(
        "Titulo",
        parent=styles["Title"],
        fontSize=16,
        textColor=colors.HexColor("#1a1a2e"),
        spaceAfter=6,
    )
    subtitulo_style = ParagraphStyle(
        "Subtitulo",
        parent=styles["Normal"],
        fontSize=9,
        textColor=colors.HexColor("#555555"),
        spaceAfter=12,
    )
    pie_style = ParagraphStyle(
        "Pie",
        parent=styles["Normal"],
        fontSize=7,
        textColor=colors.HexColor("#888888"),
    )

    elements = []

    titulo_texto = f"Reporte: {analysis_type.replace('_', ' ').title()}"
    if periodo and periodo != "todos":
        titulo_texto += f" ({periodo.upper()})"

    elements.append(Paragraph(titulo_texto, titulo_style))
    elements.append(Paragraph(
        f"Generado: {metadata.get('created_at', 'N/A')}  |  "
        f"Modelo: {metadata.get('model_name', 'N/A')}  |  "
        f"Total registros: {len(df)}",
        subtitulo_style,
    ))
    elements.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#cccccc")))
    elements.append(Spacer(1, 0.4 * cm))

    if df.empty:
        elements.append(Paragraph("No hay datos disponibles para este reporte.", styles["Normal"]))
    else:
        cols = list(df.columns)
        header = [str(c).replace("_", " ").title() for c in cols]

        def fmt(val):
            try:
                if pd.isna(val): return ""
            except (TypeError, ValueError):
                pass
            if isinstance(val, float): return f"{val:,.2f}"
            if isinstance(val, (list, dict)): return str(val)[:60] + "..." if len(str(val)) > 60 else str(val)
            return str(val)

        data_rows = [[fmt(row[c]) for c in cols] for _, row in df.iterrows()]
        table_data = [header] + data_rows

        page_w = (landscape(A4)[0] if len(cols) > 6 else A4[0]) - 3 * cm
        col_w = page_w / len(cols)

        tabla = Table(table_data, colWidths=[col_w] * len(cols), repeatRows=1)
        tabla.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1a1a2e")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
            ("FONTSIZE", (0, 0), (-1, 0), 8),
            ("ALIGN", (0, 0), (-1, 0), "CENTER"),
            ("BOTTOMPADDING", (0, 0), (-1, 0), 6),
            ("TOPPADDING", (0, 0), (-1, 0), 6),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f5f5f5")]),
            ("FONTSIZE", (0, 1), (-1, -1), 7),
            ("ALIGN", (0, 1), (-1, -1), "CENTER"),
            ("TOPPADDING", (0, 1), (-1, -1), 4),
            ("BOTTOMPADDING", (0, 1), (-1, -1), 4),
            ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#dddddd")),
            ("BOX", (0, 0), (-1, -1), 0.8, colors.HexColor("#aaaaaa")),
        ]))
        elements.append(tabla)

    elements.append(Spacer(1, 0.5 * cm))
    elements.append(HRFlowable(width="100%", thickness=0.5, color=colors.HexColor("#eeeeee")))
    elements.append(Paragraph(
        f"Fase 2 — Análisis de Datos  |  {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}",
        pie_style,
    ))

    doc.build(elements)
    logger.info("PDF generado → %s", ruta)
    return ruta


class ReportsEngine:
    FORMATOS_DISPONIBLES = ("pdf", "csv", "json")

    def __init__(self, db_uri: str = DB_URI):
        self.engine = create_engine(db_uri)
        self.generados: list[dict] = []

    def run(
            self,
            tipos: Optional[list[str]] = None,
            formatos: Optional[list[str]] = None,
            periodo: Optional[str] = None,
    ) -> list[dict]:

        inicio = datetime.now(timezone.utc)
        tipos = tipos or self._tipos_disponibles()
        formatos = formatos or list(self.FORMATOS_DISPONIBLES)
        periodo = periodo or "todos"

        formatos_invalidos = [f for f in formatos if f not in self.FORMATOS_DISPONIBLES]
        if formatos_invalidos:
            raise ValueError(f"Formatos no soportados: {formatos_invalidos}")

        logger.info(SEPARADOR)
        logger.info("Iniciando Reports Engine...")
        logger.info("Tipos: %s", tipos)
        logger.info("Formatos: %s", formatos)
        logger.info("Periodo: %s", periodo)

        self.generados = []

        for analysis_type in tipos:
            raw = _leer_analysis_result(self.engine, analysis_type, periodo)
            if raw is None:
                logger.warning("Saltando '%s' — sin datos.", analysis_type)
                continue

            df = _result_to_df(raw)
            metadata = {
                "model_name": raw["model_name"],
                "created_at": raw["created_at"],
            }

            for fmt in formatos:
                try:
                    ruta = self._generar(fmt, df, raw, analysis_type, metadata, periodo)
                    self.generados.append({
                        "analysis_type": analysis_type,
                        "formato": fmt,
                        "ruta": str(ruta),
                    })
                except Exception as e:
                    logger.error("Error generando %s para '%s': %s", fmt, analysis_type, e)

        elapsed = (datetime.now(timezone.utc) - inicio).total_seconds()
        logger.info("Reports Engine completado en %.2fs.", elapsed)
        logger.info(SEPARADOR)
        self._imprimir_resumen()
        return self.generados

    def reporte_unico(
            self,
            analysis_type: str,
            formato: str = "pdf",
            periodo: str = None
    ) -> Optional[Path]:

        if formato not in self.FORMATOS_DISPONIBLES:
            raise ValueError(f"Formato '{formato}' no soportado.")

        raw = _leer_analysis_result(self.engine, analysis_type, periodo)
        if raw is None:
            return None

        df = _result_to_df(raw)
        metadata = {"model_name": raw["model_name"], "created_at": raw["created_at"]}
        return self._generar(formato, df, raw, analysis_type, metadata, periodo)

    def _tipos_disponibles(self) -> list[str]:
        query = text("""
                     SELECT DISTINCT analysis_type
                     FROM analysis_results
                     ORDER BY analysis_type
                     """)
        try:
            with self.engine.connect() as conn:
                return [row[0] for row in conn.execute(query).fetchall()]
        except SQLAlchemyError as e:
            logger.error("Error consultando tipos disponibles: %s", e)
            return ANALYSIS_TYPES

    def _generar(
            self,
            formato: str,
            df: pd.DataFrame,
            raw: dict,
            analysis_type: str,
            metadata: dict,
            periodo: str
    ) -> Path:
        if formato == "csv":
            return generar_csv(df, analysis_type, periodo)
        elif formato == "json":
            return generar_json(raw, analysis_type, periodo)
        elif formato == "pdf":
            return generar_pdf(df, analysis_type, metadata, periodo)
        raise ValueError(f"Formato desconocido: {formato}")

    def _imprimir_resumen(self):
        print(f"\n{SEPARADOR}")
        print("  REPORTES GENERADOS")
        print(SEPARADOR)
        if not self.generados:
            print("  (ninguno)")
        for item in self.generados:
            icon = {"pdf": "📄", "csv": "📊", "json": "🗂"}.get(item["formato"], "•")
            print(f"  {icon} [{item['formato'].upper()}] {item['analysis_type']}")
            print(f"       → {item['ruta']}")
        print(f"\n  Total archivos: {len(self.generados)}")
        print(f"  Carpeta:        {REPORTS_DIR}")
        print(f"{SEPARADOR}\n")

