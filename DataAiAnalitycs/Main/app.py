"""
Main/app.py
"""
import logging
import threading
import traceback
import sys
from pathlib import Path
from flask import Flask, request, jsonify, send_from_directory, send_file, Blueprint

# ── Configuración dinámica de rutas ──────────────────────────────
ROOT_DIR    = Path(__file__).resolve().parent.parent
REPORTS_DIR = Path(__file__).resolve().parent / "reports"
sys.path.append(str(ROOT_DIR))
sys.path.append(str(ROOT_DIR / "IA"))

# ── Importar pipelines ───────────────────────────────────────────
try:
    from Backend.pipelines.pipeline_raw import run_pipeline_single_file
    from Backend.pipelines.pipeline_analysis import PipelineAnalysis
    from Backend.pipelines.pipeline_report import DataAIPipelineManager
    from Backend.pipelines.pipeline_forecast import SalesForecastPipeline, ErpRepository
except ImportError as e:
    print(f"⚠️ ERROR CRÍTICO DE IMPORTACIÓN EN PIPELINES: {e}")

    def run_pipeline_single_file(ruta):
        pass

    class PipelineAnalysis:
        def __init__(self, **kwargs): pass
        def run(self): pass

    class DataAIPipelineManager:
        def __init__(self, **kwargs): pass
        def run(self, df=None): return {"elapsed_s": 0.0, "report_paths": {}}

    class SalesForecastPipeline:
        def __init__(self, **kwargs): pass
        def run(self): return {}

try:
    from Backend.pipelines.customer_analytics_pipeline import (
        CustomerAnalyticsPipeline,
        ErpRepository as CustomerErpRepository,
    )
    _CUSTOMER_PIPELINE_AVAILABLE = True
except ImportError as e:
    _CUSTOMER_PIPELINE_AVAILABLE = False
    print(f"⚠️ CustomerAnalyticsPipeline no disponible: {e}")

    class CustomerAnalyticsPipeline:
        def __init__(self, **kwargs): pass
        def run(self, **kwargs): return {}

    class CustomerErpRepository:
        def fetch_last_result(self, analysis_type): return None

# ── ReportsEngine para el endpoint de descarga ───────────────────────
try:
    from Backend.src.analysis.reports import ReportsEngine
    _REPORTS_AVAILABLE = True
except ImportError as e:
    _REPORTS_AVAILABLE = False
    print(f"⚠️ ReportsEngine no disponible. Error real: {e}")

# ── Estadísticas para el dashboard ───────────────────────────────────
try:
    from Backend.src.analysis.satistics import calcular_estadisticas_dataframe
    _STATS_AVAILABLE = True
except ImportError as e:
    _STATS_AVAILABLE = False
    print(f"⚠️ satistics.py no disponible. Error real: {e}")

# ── DB ───────────────────────────────────────────────────────────────────────
try:
    from sqlalchemy import create_engine, text as sa_text
    import os
    from dotenv import load_dotenv

    _env_path = ROOT_DIR / ".env"
    load_dotenv(_env_path)
    _DB_URI = (
        f"postgresql://{os.getenv('user')}:{os.getenv('password')}"
        f"@{os.getenv('host')}:{os.getenv('port', 5432)}/{os.getenv('database')}"
    )
    _engine = create_engine(_DB_URI, pool_pre_ping=True)
    _DB_AVAILABLE = True
except Exception as _db_err:
    _DB_AVAILABLE = False
    print(f"⚠️ DB no disponible para dashboard: {_db_err}")


# Configuración de la App

class Config:
    BASE_DIR = Path(__file__).parent
    UPLOAD_FOLDER = ROOT_DIR / "example"
    ALLOWED_EXTENSIONS = {".csv", ".xlsx", ".json", ".txt", ".xml"}


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("mi_app_backend")

_pipeline_lock = threading.Lock()

_PERIODOS_STANDARD = ["dia", "semana", "mes", "año", "historico"]


# Lógica de negocio

def _allowed(filename: str, allowed_exts: set) -> bool:
    return Path(filename).suffix.lower() in allowed_exts


def _run_analysis_todos_periodos(skip_extract: bool = False) -> None:

    for periodo in _PERIODOS_STANDARD:
        logger.info("PipelineAnalysis (periodo=%s | skip_extract=%s)...", periodo, skip_extract)
        try:

            analizador = PipelineAnalysis(periodo=periodo, skip_extract=False)
            analizador.run()
            logger.info("PipelineAnalysis completado (periodo=%s).", periodo)
        except Exception as exc:
            logger.error(
                "Error en PipelineAnalysis para periodo '%s': %s\n%s",
                periodo, exc, traceback.format_exc(),
            )


def _trigger_pipelines_batch(rutas_archivos: list, periodo: str = "mes") -> None:

    def _run():
        if not _pipeline_lock.acquire(blocking=False):
            logger.warning("Pipeline ya en ejecución; se descartó la solicitud duplicada.")
            return
        try:
            for ruta in rutas_archivos:
                logger.info("Pipeline Fase 1 (Raw) → %s", ruta)
                try:
                    run_pipeline_single_file(ruta)
                    logger.info("Pipeline Fase 1 completado → %s", ruta)
                except Exception as exc_raw:
                    logger.error("Error en Fase 1 para '%s': %s", ruta, exc_raw)

            _run_analysis_todos_periodos(skip_extract=False)

        except Exception as exc:
            logger.error("Error general en lote: %s", exc)
            logger.error(traceback.format_exc())
        finally:
            _pipeline_lock.release()

    threading.Thread(target=_run, daemon=True).start()


def _trigger_recalculate_background(periodos: list) -> None:

    def _run():
        if not _pipeline_lock.acquire(blocking=False):
            logger.warning("Pipeline ya en ejecución; recálculo descartado.")
            return
        try:
            for periodo in periodos:
                logger.info("Recálculo (periodo=%s)...", periodo)
                try:
                    analizador = PipelineAnalysis(periodo=periodo, skip_extract=False)
                    analizador.run()
                    logger.info("Recálculo completado (periodo=%s).", periodo)
                except Exception as exc:
                    logger.error(
                        "Error en recálculo para '%s': %s\n%s",
                        periodo, exc, traceback.format_exc(),
                    )
        finally:
            _pipeline_lock.release()

    threading.Thread(target=_run, daemon=True).start()


# Blueprints


ui_bp  = Blueprint("ui",  __name__)
api_bp = Blueprint("api", __name__)


# ── Rutas UI ───────────────────────────────────────────────────

@ui_bp.route("/")
@ui_bp.route("/upload.html")
def index():
    return send_from_directory(Config.BASE_DIR, "upload.html")


@ui_bp.route("/reports.html")
def reports_page():
    return send_from_directory(Config.BASE_DIR, "reports.html")


@ui_bp.route("/assistant")
@ui_bp.route("/assistant.html")
def assistant_page():
    return send_from_directory(Config.BASE_DIR, "assistant.html")


# ── /upload ─────────────────────────
@api_bp.route("/upload", methods=["POST"])
def upload():
    files   = request.files.getlist("files")
    periodo = request.args.get("periodo", "mes").lower().strip()

    if not files:
        return jsonify({"error": "No se recibieron archivos"}), 400

    results          = []
    rutas_a_procesar = []

    for f in files:
        name = f.filename
        if not name:
            continue
        if not _allowed(name, Config.ALLOWED_EXTENSIONS):
            results.append({"file": name, "status": "error", "msg": "Extensión no permitida"})
            continue

        dest = Config.UPLOAD_FOLDER / Path(name).name
        f.save(dest)
        logger.info("Archivo guardado: %s", dest)
        rutas_a_procesar.append(str(dest.resolve()))
        results.append({"file": name, "status": "ok", "msg": "Guardado y encolado."})

    if rutas_a_procesar:
        _trigger_pipelines_batch(rutas_a_procesar, periodo=periodo)

    return jsonify({
        "status":  "success",
        "message": "Procesando en segundo plano",
        "periodo": periodo,
        "results": results,
    }), 200


# ── /api/recalculate ─────────────────────────────────────────────
@api_bp.route("/api/recalculate", methods=["POST"])
def recalculate_period():

    import unicodedata
    raw     = request.args.get("periodo", "mes")
    try:
        raw = raw.encode("latin-1").decode("utf-8")
    except Exception:
        pass
    periodo = unicodedata.normalize("NFC", raw).lower().strip()
    if periodo in ("ano", "anio"):
        periodo = "año"

    if not _pipeline_lock.acquire(blocking=False):
        return jsonify({
            "error": "El pipeline ya está procesando datos. Intenta en unos segundos.",
        }), 429
    _pipeline_lock.release()

    logger.info("Recálculo solicitado para periodo: %s", periodo)
    _trigger_recalculate_background([periodo])

    return jsonify({
        "status":  "accepted",
        "message": f"Recálculo para '{periodo}' iniciado en segundo plano.",
        "periodo": periodo,
    }), 202


# ── /api/recalculate-all ──────────────────────────────────────────
@api_bp.route("/api/recalculate-all", methods=["POST"])
def recalculate_all_periods():
    if not _pipeline_lock.acquire(blocking=False):
        return jsonify({
            "error": "El pipeline ya está procesando datos. Intenta en unos segundos.",
        }), 429
    _pipeline_lock.release()

    logger.info("Recálculo de TODOS los periodos solicitado.")
    _trigger_recalculate_background(_PERIODOS_STANDARD)

    return jsonify({
        "status":  "accepted",
        "message": "Recálculo de todos los periodos iniciado en segundo plano.",
        "periodos": _PERIODOS_STANDARD,
    }), 202


# ── /api/run-ia-pipeline ──────────────────────────────────────
@api_bp.route("/api/run-ia-pipeline", methods=["POST"])
def run_ia_pipeline_endpoint():
    try:
        pipeline_ia  = DataAIPipelineManager(
            knn_task      = "classification",
            artifacts_dir = str(REPORTS_DIR.parent / "artifacts"),
            output_dir    = str(REPORTS_DIR),
        )
        resultado_ia = pipeline_ia.run(df=None)
        return jsonify({
            "status":        "success",
            "elapsed_s":     resultado_ia.get("elapsed_s"),
            "report_paths":  resultado_ia.get("report_paths"),
            "knn_plot_data": resultado_ia.get("knn_plot_data", []),
        }), 200
    except Exception as e:
        logger.error("Error ejecutando IA manual: %s", str(e))
        return jsonify({"status": "error", "error": str(e)}), 500


# ── /api/ia/plot-data ─────────────────────────────────────────────
@api_bp.route("/api/ia/plot-data", methods=["GET"])
def get_ia_plot_data():
    import json as _json

    knn_file = REPORTS_DIR / "knn_plot_data.json"
    logger.info("get_ia_plot_data → buscando en: %s | existe: %s", knn_file, knn_file.exists())

    if not knn_file.exists():
        return jsonify({"error": "no data"}), 404

    try:
        with open(knn_file, "r", encoding="utf-8") as f:
            data = _json.load(f)
        points = data.get("points", data) if isinstance(data, dict) else data
        logger.info("get_ia_plot_data → sirviendo %d puntos", len(points))
        return jsonify(data), 200
    except Exception as exc:
        logger.error("Error leyendo knn_plot_data.json: %s", exc)
        return jsonify({"error": "read error", "detail": str(exc)}), 500


# ── /api/pipeline_forecast ────────────────────────────────────────
@api_bp.route("/api/pipeline_forecast", methods=["POST"])
def trigger_forecast_pipeline():
    try:
        logger.info("Iniciando ejecución del SalesForecastPipeline...")
        pipeline_forecast = SalesForecastPipeline()
        resultados = pipeline_forecast.run()
        logger.info("SalesForecastPipeline ejecutado con éxito.")
        return jsonify({
            "status":  "success",
            "message": "Pipeline de forecast ejecutado correctamente",
        }), 200
    except Exception as e:
        logger.error("Error ejecutando pipeline de forecast: %s", str(e))
        logger.error(traceback.format_exc())
        return jsonify({"status": "error", "error": str(e)}), 500


# ── /api/forecast/last_result ─────────────────────────────────────
@api_bp.route("/api/forecast/last_result", methods=["GET"])
def get_last_forecast():
    repo   = ErpRepository()
    result = repo.fetch_last_result("linear_sales_forecast_ALL")
    if result is None:
        return jsonify({"error": "Sin resultados"}), 404
    return jsonify(result), 200


# ── /api/pipeline_customers  (ejecuta RFM en background) ─────────
@api_bp.route("/api/pipeline_customers", methods=["POST"])
def trigger_customer_pipeline():
    if not _CUSTOMER_PIPELINE_AVAILABLE:
        return jsonify({
            "status": "error",
            "error":  "CustomerAnalyticsPipeline no está disponible en este servidor.",
        }), 503

    body           = request.get_json(silent=True) or {}
    since_date     = body.get("since_date")
    n_quantiles    = int(body.get("n_quantiles", 5))
    reference_date = body.get("reference_date")

    def _run_customer_pipeline():
        try:
            logger.info(
                "CustomerAnalyticsPipeline iniciado | since=%s | quantiles=%d",
                since_date, n_quantiles,
            )
            pipeline = CustomerAnalyticsPipeline(
                n_quantiles    = n_quantiles,
                reference_date = reference_date,
                model_version  = "3B-v1.0",
            )
            pipeline.run(
                since_date    = since_date,
                persist       = True,
                print_summary = True,
            )
            logger.info("CustomerAnalyticsPipeline completado exitosamente.")
        except Exception as exc:
            logger.error("Error en CustomerAnalyticsPipeline: %s", exc)
            logger.error(traceback.format_exc())

    threading.Thread(target=_run_customer_pipeline, daemon=True).start()

    return jsonify({
        "status":  "accepted",
        "message": "Pipeline RFM iniciado en segundo plano. "
                   "Consulta /api/customers/last_result cuando termine.",
        "params": {
            "since_date":     since_date,
            "n_quantiles":    n_quantiles,
            "reference_date": reference_date,
        },
    }), 202


# ── /api/customers/last_result  (lee el último resultado RFM) ────
@api_bp.route("/api/customers/last_result", methods=["GET"])
def get_last_customer_result():
    if not _CUSTOMER_PIPELINE_AVAILABLE:
        return jsonify({"error": "CustomerAnalyticsPipeline no disponible."}), 503

    segment_filter = request.args.get("segment", "").strip() or None

    try:
        repo   = CustomerErpRepository()
        result = repo.fetch_last_result("customer_rfm_segmentation")

        if result is None:
            return jsonify({"error": "Sin resultados. Ejecuta primero el pipeline RFM."}), 404

        if segment_filter:
            segments = result.get("segments", {})
            if segment_filter not in segments:
                return jsonify({
                    "error":     f"Segmento '{segment_filter}' no encontrado.",
                    "available": list(segments.keys()),
                }), 404
            return jsonify({
                "generated_at":   result.get("generated_at"),
                "segment_filter": segment_filter,
                "segment_data":   segments[segment_filter],
                "summary":        result.get("summary"),
            }), 200

        return jsonify(result), 200

    except Exception as exc:
        logger.error("Error leyendo resultado RFM: %s", exc)
        return jsonify({"error": "Error interno al consultar el resultado RFM."}), 500


# ── /api/download/report ──────────────────────────────────────────────
@api_bp.route("/api/download/report", methods=["GET"])
def download_report():
    if not _REPORTS_AVAILABLE:
        return jsonify({"error": "ReportsEngine no disponible en el servidor."}), 503

    formato = request.args.get("formato", "").lower().strip()
    periodo = request.args.get("periodo", "mes").lower().strip()

    FORMATOS_VALIDOS = ("pdf", "csv", "json")
    if formato not in FORMATOS_VALIDOS:
        return jsonify({"error": f"Formato '{formato}' no válido."}), 400

    analysis_type = "ventas_por_dia" if periodo in ("dia", "semana") else "ventas_por_mes"
    logger.info(
        "Descarga solicitada → formato=%s | periodo=%s | tipo=%s",
        formato, periodo, analysis_type,
    )

    try:
        re   = ReportsEngine(db_uri=_DB_URI if _DB_AVAILABLE else None)
        ruta = re.reporte_unico(analysis_type=analysis_type, formato=formato, periodo=periodo)

        if ruta is None:
            return jsonify({
                "error": f"No hay datos disponibles para '{analysis_type}' en este periodo.",
            }), 404

        mime_map = {"pdf": "application/pdf", "csv": "text/csv", "json": "application/json"}
        return send_file(
            str(ruta),
            mimetype=mime_map[formato],
            as_attachment=True,
            download_name=ruta.name,
        )
    except Exception as exc:
        logger.error("Error generando reporte para descarga: %s", exc)
        logger.error(traceback.format_exc())
        return jsonify({"error": "Error interno al generar el reporte."}), 500


# ── /api/dashboard/stats ───────────────────────────
@api_bp.route("/api/dashboard/stats", methods=["GET"])
def get_dashboard_stats():
    import json as _json
    import unicodedata
    import pandas as pd

    raw = request.args.get("periodo", "mes")
    try:
        raw = raw.encode("latin-1").decode("utf-8")
    except Exception:
        pass
    periodo    = unicodedata.normalize("NFC", raw).lower().strip()
    if periodo in ("ano", "anio"):
        periodo = "año"
    periodo_db = "todos" if periodo == "todos" else periodo
    logger.info("Dashboard stats → periodo='%s' | periodo_db='%s'", periodo, periodo_db)

    if not _DB_AVAILABLE:
        return jsonify({"error": "Base de datos no disponible", "periodo": periodo}), 503

    try:
        with _engine.connect() as conn:

            def _leer_result(analysis_type: str) -> list:
                if periodo == "todos":
                    q = sa_text("""
                        SELECT result -> 'datos' AS datos
                        FROM analysis_results
                        WHERE analysis_type = :tipo
                        ORDER BY created_at DESC
                        LIMIT 1
                    """)
                    row = conn.execute(q, {"tipo": analysis_type}).fetchone()
                else:
                    q = sa_text("""
                        SELECT result -> 'datos' AS datos
                        FROM analysis_results
                        WHERE analysis_type = :tipo
                          AND model_version  = :version
                        ORDER BY created_at DESC
                        LIMIT 1
                    """)
                    row = conn.execute(q, {
                        "tipo":    analysis_type,
                        "version": f"1.0-{periodo_db}",
                    }).fetchone()

                logger.info(
                    "_leer_result(%s | 1.0-%s) → %s",
                    analysis_type, periodo,
                    "OK" if row and row[0] else "VACÍO",
                )

                if row is None or row[0] is None:
                    return []
                datos = row[0]
                if isinstance(datos, str):
                    datos = _json.loads(datos)
                return datos if isinstance(datos, list) else []

            mejores_clientes       = _leer_result("clientes_frecuentes")
            productos_mas_vendidos = _leer_result("top_productos")
            analysis_ventas        = "ventas_por_dia" if periodo in ("dia", "semana") else "ventas_por_mes"
            mas_ventas             = _leer_result(analysis_ventas)

            q_fmt = sa_text("""
                SELECT LOWER(
                    CASE
                        WHEN POSITION('.' IN REVERSE(file_name)) > 0
                            THEN SUBSTRING(file_name FROM LENGTH(file_name) -
                                           POSITION('.' IN REVERSE(file_name)) + 2)
                        ELSE 'desconocido'
                    END
                ) AS extension,
                COUNT(*) AS total
                FROM data_sources
                WHERE file_name IS NOT NULL AND file_name != ''
                GROUP BY extension
                ORDER BY total DESC
            """)
            rows_fmt = conn.execute(q_fmt).fetchall()

            distribucion_formatos = {}
            for row in rows_fmt:
                ext = (row[0] or "desconocido").strip().lstrip(".")
                if not ext:
                    ext = "desconocido"
                distribucion_formatos[ext] = distribucion_formatos.get(ext, 0) + int(row[1])

            recomendaciones_ia = None
            try:
                q_reco   = sa_text("""
                    SELECT result FROM analysis_results
                    WHERE analysis_type = 'knn_recommendation'
                    ORDER BY created_at DESC LIMIT 1
                """)
                row_reco = conn.execute(q_reco).fetchone()
                if row_reco and row_reco[0]:
                    payload = row_reco[0]
                    if isinstance(payload, str):
                        payload = _json.loads(payload)
                    if isinstance(payload, dict):
                        recomendaciones_ia = (
                            payload.get("datos")
                            or payload.get("recomendaciones")
                            or payload
                        )
                    elif isinstance(payload, list):
                        recomendaciones_ia = payload
            except Exception as reco_exc:
                logger.warning("No se pudieron obtener recomendaciones IA: %s", reco_exc)

        estadisticas = {}
        if _STATS_AVAILABLE and mas_ventas:
            try:
                df_ventas    = pd.DataFrame(mas_ventas)
                estadisticas = calcular_estadisticas_dataframe(
                    df_ventas, nombre=f"ventas_{periodo}",
                )
            except Exception as stats_exc:
                logger.warning("No se pudieron calcular estadísticas: %s", stats_exc)

        return jsonify({
            "periodo": periodo,
            "grafico_principal": {
                "mejores_clientes":       mejores_clientes,
                "productos_mas_vendidos": productos_mas_vendidos,
                "mas_ventas":             mas_ventas,
                "tendencia_ventas":       mas_ventas,
            },
            "distribucion_formatos": distribucion_formatos,
            "estadisticas":          estadisticas,
            "recomendaciones_ia":    recomendaciones_ia,
        }), 200

    except Exception as exc:
        logger.error("Error en /api/dashboard/stats: %s", exc)
        logger.error(traceback.format_exc())
        return jsonify({"error": "Error interno al consultar el dashboard"}), 500


# ── /api/limpiar-ia ───────────────────────────────────────────
@api_bp.route("/api/limpiar-ia", methods=["POST"])
def limpiar_cache_ia():
    import os
    try:
        knn_file = REPORTS_DIR / "knn_plot_data.json"

        if knn_file.exists():
            os.remove(knn_file)
            logger.info("Archivo caché knn_plot_data.json eliminado.")

            return jsonify({
                "status": "success",
                "message": "Caché de IA limpiada. La gráfica desaparecerá."
            }), 200
        else:
            return jsonify({
                "status": "success",
                "message": "No había ninguna gráfica guardada."
            }), 200

    except Exception as e:
        logger.error("Error al borrar el JSON de IA: %s", str(e))
        return jsonify({"status": "error", "error": str(e)}), 500

# ── /api/assistant/chat ───────────────────────────────────────
@api_bp.route("/api/assistant/chat", methods=["POST"])
def assistant_chat():
    try:
        data = request.get_json()
        if not data or "message" not in data:
            return jsonify({"error": "Mensaje no proporcionado"}), 400
        user_message = data["message"]
        logger.info("Asistente IA recibió: %s", user_message)
        respuesta_ia = (
            f"He recibido tu mensaje: '{user_message}'. "
            "¡Soy tu asistente de datos local! ¿En qué te ayudo con los pipelines hoy?"
        )
        return jsonify({"status": "success", "reply": respuesta_ia}), 200
    except Exception as e:
        logger.error("Error en el Asistente de IA: %s", str(e))
        return jsonify({"error": "Ocurrió un error interno."}), 500


# App Factory

def create_app():
    app = Flask(__name__, static_folder=None)
    app.config.from_object(Config)
    app.config["UPLOAD_FOLDER"].mkdir(parents=True, exist_ok=True)
    app.register_blueprint(ui_bp)
    app.register_blueprint(api_bp)
    return app


if __name__ == "__main__":
    app_instance = create_app()
    logger.info("Servidor iniciado → http://0.0.0.0:7777")
    app_instance.run(host="0.0.0.0", port=7777, debug=False)