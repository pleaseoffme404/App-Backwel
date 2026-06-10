import argparse
import logging
import os
import sys
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Optional
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.append(str(BASE_DIR))
load_dotenv(BASE_DIR / ".env")

host     = os.getenv("host")
database = os.getenv("database")
user     = os.getenv("user")
password = os.getenv("password")
port     = os.getenv("port", "5432")

DB_URI = f"postgresql://{user}:{password}@{host}:{port}/{database}"

from data.query_raw_data import RawDataPipeline
from Backend.src.analysis.anomaly_detection    import AnomalyDetector
from Backend.src.analysis.transform_analysis   import TransformAnalysis
from Backend.src.analysis.kpis                 import KpisEngine
from Backend.src.analysis.satistics            import Analysis, DB_URI   # noqa
from Backend.src.analysis.reports              import ReportsEngine

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger   = logging.getLogger("pipeline_analysis")
SEPARADOR = "=" * 65

_BLOQUES_VALIDOS = {"dia", "semana", "mes", "año", "todos"}

def _calcular_fechas(periodo: str) -> tuple[Optional[str], Optional[str]]:
    periodo = (periodo or "mes").lower().strip()
    if periodo not in _BLOQUES_VALIDOS:
        logger.warning("periodo '%s' no reconocido, usando 'mes' como fallback.", periodo)
        periodo = "mes"

    if periodo == "todos":
        return None, None

    ahora = datetime.now(timezone.utc)
    fin_hoy = ahora.replace(hour=23, minute=59, second=59, microsecond=999999)

    deltas = {
        "dia":    timedelta(days=1),
        "semana": timedelta(weeks=1),
        "mes":    timedelta(days=30),
        "año":    timedelta(days=365),
    }
    inicio = ahora - deltas[periodo]
    fmt = "%Y-%m-%dT%H:%M:%S+00:00"
    return inicio.strftime(fmt), fin_hoy.strftime(fmt)


class PipelineAnalysis:

    def __init__(
        self,
        periodo:         Optional[str]       = "mes",
        start_date:      Optional[str]       = None,
        end_date:        Optional[str]       = None,
        formatos:        Optional[list[str]] = None,
        tipos_reporte:   Optional[list[str]] = None,
        skip_extract:    bool = False,
        skip_anomalies:  bool = False,
        skip_transform:  bool = False,
        skip_kpis:       bool = False,
        skip_stats:      bool = False,
        skip_reports:    bool = False,
        db_uri:          str  = DB_URI,
    ):
        self.periodo = (periodo or "mes").lower().strip()
        if self.periodo in _BLOQUES_VALIDOS:
            _start, _end = _calcular_fechas(self.periodo)
            self.start_date = _start if _start else start_date
            self.end_date   = _end   if _end   else end_date
        else:
            self.start_date = start_date
            self.end_date   = end_date

        self.formatos        = formatos or ["json"]
        self.tipos_reporte   = tipos_reporte
        self.skip_extract    = skip_extract
        self.skip_anomalies  = skip_anomalies
        self.skip_transform  = skip_transform
        self.skip_kpis       = skip_kpis
        self.skip_stats      = skip_stats
        self.skip_reports    = skip_reports
        self.db_uri          = db_uri

        self.resultados: dict = {
            "datasets":   {},
            "anomalias":  {},
            "transforms": {},
            "kpis":       {},
            "reportes":   [],
        }

    def run(self) -> dict:
        inicio = datetime.now(timezone.utc)
        self._banner("INICIANDO PIPELINE ANALYSIS")
        logger.info(
            "  Periodo: %s | start_date: %s | end_date: %s",
            self.periodo, self.start_date, self.end_date,
        )

        if not self.skip_extract:    self._run_extraccion()
        else:                        logger.info("Fase 1 (Extracción) omitida.")

        if not self.skip_anomalies:  self._run_anomalias()
        else:                        logger.info("Fase 2 (Anomalías) omitida.")

        if not self.skip_transform:  self._run_transformacion()
        else:                        logger.info("Fase 3 (Transformación) omitida.")

        if not self.skip_kpis:       self._run_kpis()
        else:                        logger.info("Fase 4 (KPIs) omitida.")

        if not self.skip_stats:      self._run_estadisticas()
        else:                        logger.info("Fase 5 (Estadísticas) omitida.")

        if not self.skip_reports:    self._run_reportes()
        else:                        logger.info("Fase 6 (Reportes) omitida.")

        elapsed = (datetime.now(timezone.utc) - inicio).total_seconds()
        self._banner(f"PIPELINE COMPLETADO en {elapsed:.2f}s")
        self._imprimir_resumen_final()

        return self.resultados

    def _run_extraccion(self):
        self._banner("FASE 1 — EXTRACCIÓN (RawDataPipeline)", nivel=2)
        try:
            with RawDataPipeline() as pipeline:
                self.resultados["datasets"] = pipeline.run(
                    query="all",
                    start_date=self.start_date,
                    end_date=self.end_date,
                )
            logger.info("Extracción completada — %d datasets.", len(self.resultados["datasets"]))
        except Exception as e:
            logger.error("Error en Fase 1: %s", e); raise

    def _run_anomalias(self):
        self._banner("FASE 2 — DETECCIÓN DE ANOMALÍAS (AnomalyDetector)", nivel=2)
        try:
            detector = AnomalyDetector(
                start_date=self.start_date,
                end_date=self.end_date,
            )
            self.resultados["anomalias"] = detector.run()
            logger.info(
                "Detección completada — %d anomalías (%d únicos).",
                self.resultados["anomalias"].get("anomalias_total", 0),
                self.resultados["anomalias"].get("registros_unicos", 0),
            )
        except Exception as e:
            logger.error("Error en Fase 2: %s", e); raise

    def _run_transformacion(self):
        self._banner("FASE 3 — TRANSFORMACIÓN (TransformAnalysis)", nivel=2)
        try:
            ta = TransformAnalysis(
                start_date=self.start_date,
                end_date=self.end_date,
                periodo=self.periodo,           # ← nuevo parámetro
            )
            self.resultados["transforms"] = ta.run()
            logger.info("Transformación completada — %d agregaciones.", len(self.resultados["transforms"]))
        except Exception as e:
            logger.error("Error en Fase 3: %s", e); raise

    def _run_kpis(self):
        self._banner("FASE 4 — KPIs (KpisEngine)", nivel=2)
        try:
            ke = KpisEngine(periodo=self.periodo)   # ← nuevo parámetro
            self.resultados["kpis"] = ke.run()
            logger.info("KPIs calculados — %d indicadores.", len(self.resultados["kpis"]))
        except Exception as e:
            logger.error("Error en Fase 4: %s", e); raise

    def _run_estadisticas(self):
        self._banner("FASE 5 — ESTADÍSTICAS (Analysis)", nivel=2)
        try:
            transforms = self.resultados.get("transforms", {})
            if transforms:
                logger.info("Estadísticas desde DataFrames en memoria (%d datasets).", len(transforms))
                for nombre, df in transforms.items():
                    try:
                        Analysis.desde_dataframe(df, nombre)
                    except Exception as e:
                        logger.error("Error en estadísticas de '%s': %s", nombre, e)
            else:
                logger.info("Sin transforms en memoria; leyendo analysis_results.")
                Analysis.analizar_todos(self.db_uri)
        except Exception as e:
            logger.error("Error en Fase 5: %s", e); raise

    def _run_reportes(self):
        self._banner("FASE 6 — REPORTES (ReportsEngine)", nivel=2)
        try:
            re = ReportsEngine(db_uri=self.db_uri)
            self.resultados["reportes"] = re.run(
                tipos=self.tipos_reporte,
                formatos=self.formatos,
            )
            logger.info("Reportes generados — %d archivos.", len(self.resultados["reportes"]))
        except Exception as e:
            logger.error("Error en Fase 6: %s", e); raise

    def _banner(self, texto: str, nivel: int = 1):
        if nivel == 1:
            logger.info(SEPARADOR); logger.info("  %s", texto); logger.info(SEPARADOR)
        else:
            logger.info("─" * 65); logger.info("  %s", texto); logger.info("─" * 65)

    def _imprimir_resumen_final(self):
        print(f"\n{SEPARADOR}")
        print("  RESUMEN FINAL DEL PIPELINE")
        print(SEPARADOR)

        datasets = self.resultados.get("datasets", {})
        if datasets:
            print("\n  Datasets extraídos:")
            for nombre, df in datasets.items():
                print(f"     • {nombre:<25} {len(df):>6} filas")

        anomalias = self.resultados.get("anomalias", {})
        if anomalias:
            print("\n  Detección de anomalías:")
            print(f"     • Total anomalías:  {anomalias.get('anomalias_total',0):>12,}")
            print(f"     • Registros únicos: {anomalias.get('registros_unicos',0):>12,}")
            por_regla = anomalias.get("por_regla", {})
            descripciones = {
                "R01":"Precio / monto en cero","R02":"Amount estadísticamente anormal",
                "R03":"Cantidad negativa (no devolución)","R04":"Score de riesgo alto",
                "R05":"Amount inconsistente con value×qty","R06":"Pendiente por más de N días",
                "R07":"Registros duplicados",
            }
            for codigo, cantidad in por_regla.items():
                if cantidad > 0:
                    print(f"{codigo} {descripciones.get(codigo,''):<38} {cantidad}")

        transforms = self.resultados.get("transforms", {})
        if transforms:
            print("\n  Transformaciones:")
            for nombre, df in transforms.items():
                print(f"     • {nombre:<25} {len(df):>6} filas")

        kpis = self.resultados.get("kpis", {})
        if kpis:
            print("\n  KPIs calculados:")
            v = kpis.get("kpi_ventas", {})
            c = kpis.get("kpi_clientes", {})
            p = kpis.get("kpi_productos", {})
            f = kpis.get("kpi_facturacion", {})
            if v:
                print(f"     • Total ventas:    ${v.get('total_ventas',0):>12,.2f}")
                print(f"     • Transacciones:   {v.get('total_transacciones',0):>12,}")
                print(f"     • Ticket promedio: ${v.get('ticket_promedio_global',0):>12,.2f}")
            if c:
                print(f"     • Clientes únicos: {c.get('total_clientes_unicos',0):>12,}")
                print(f"     • Tasa recurrencia:{c.get('tasa_recurrencia','N/A'):>12}")
            if p:
                print(f"     • Productos dist.: {p.get('total_productos_distintos',0):>12,}")
            if f:
                print(f"     • Emitidas:        {f.get('total_emitidas',0):>12,}")
                print(f"     • Canceladas:      {f.get('total_canceladas',0):>12,}")

        reportes = self.resultados.get("reportes", [])
        if reportes:
            print(f"\n Reportes generados ({len(reportes)} archivos):")
            for r in reportes:
                icon = {"json": "🗂"}.get(r["formato"])
                print(f"     {icon} [{r['formato'].upper()}] {r['analysis_type']}")
                print(f"          → {r['ruta']}")

        print(f"\n{SEPARADOR}\n")

def parse_args():
    parser = argparse.ArgumentParser(
        description="Pipeline Analysis — Orquestador completo de la Fase 2.",
        formatter_class=argparse.RawTextHelpFormatter,
    )
    # NUEVO
    parser.add_argument(
        "--periodo",
        default="mes",
        choices=list(_BLOQUES_VALIDOS),
        help="Bloque de tiempo: dia | semana | mes | año | todos  (default: mes).",
    )
    # Originales
    parser.add_argument("--start", default=None, metavar="YYYY-MM-DD")
    parser.add_argument("--end",   default=None, metavar="YYYY-MM-DD")
    parser.add_argument("--formatos",     nargs="+", default=["json"], choices=["json"])
    parser.add_argument("--tipos-reporte",nargs="+", default=None)
    parser.add_argument("--skip-extract",   action="store_true")
    parser.add_argument("--skip-anomalies", action="store_true")
    parser.add_argument("--skip-transform", action="store_true")
    parser.add_argument("--skip-kpis",      action="store_true")
    parser.add_argument("--skip-stats",     action="store_true")
    parser.add_argument("--skip-reports",   action="store_true")
    parser.add_argument("--db-uri", default=DB_URI)
    return parser.parse_args()


def main():
    args = parse_args()
    pipeline = PipelineAnalysis(
        periodo        = args.periodo,
        start_date     = args.start,
        end_date       = args.end,
        formatos       = args.formatos,
        tipos_reporte  = args.tipos_reporte,
        skip_extract   = args.skip_extract,
        skip_anomalies = args.skip_anomalies,
        skip_transform = args.skip_transform,
        skip_kpis      = args.skip_kpis,
        skip_stats     = args.skip_stats,
        skip_reports   = args.skip_reports,
        db_uri         = args.db_uri,
    )
    return pipeline.run()
