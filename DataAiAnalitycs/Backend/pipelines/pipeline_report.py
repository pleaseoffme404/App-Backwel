from __future__ import annotations
from KNN.feature_engineer import FeatureEngineer
from KNN.knn_trainer import KNNModelTrainer
from KNN.model_validator import ModelValidator
from KNN.knn_recommender import KNNRecommender
from KNN.recommendation_engine import RecommendationEngine
from KNN.report_manager import ReportManager
import argparse
import json
import logging
import sys
import time
import traceback
import uuid
import warnings
from contextlib import contextmanager
from datetime import datetime, timezone
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any, Optional
from sklearn.decomposition import PCA
import numpy as np
import pandas as pd
from Backend.data.db_connector import Connect_BD

warnings.filterwarnings("ignore")

LOG_FILE             = "pipeline_execution.log"
LOG_MAX_BYTES        = 10 * 1024 * 1024   # 10 MB
LOG_BACKUP_COUNT     = 5
ARTIFACTS_DIR        = "./artifacts"
OUTPUT_DIR           = "./reports"
PROCESSED_DATA_TABLE = "processed_data"

_META_COLS = {
    "id", "created_at", "updated_at", "deleted_at",
    "anomaly_flag", "anomaly_rules",
}

PHASE_NAMES = {
    1: "Feature Engineering",
    2: "Entrenamiento KNN",
    3: "Validación del Modelo",
    4: "Motor de Predicción",
    5: "KNN Recommender (fit)",
    6: "Motor de Recomendación",
    7: "Generación de Reportes",
}

def _setup_logging(level_str: str = "INFO", log_dir: Path = Path(".")) -> logging.Logger:
    level = getattr(logging, level_str.upper(), logging.INFO)

    fmt_file = logging.Formatter(
        fmt     = "%(asctime)s [%(levelname)-8s] %(name)s:%(lineno)d — %(message)s",
        datefmt = "%Y-%m-%d %H:%M:%S",
    )
    fmt_console = logging.Formatter(
        fmt     = "%(asctime)s [%(levelname)-8s] %(message)s",
        datefmt = "%H:%M:%S",
    )

    logger = logging.getLogger("IA_Pipeline")
    logger.setLevel(level)
    logger.handlers.clear()

    ch = logging.StreamHandler(sys.stdout)
    ch.setLevel(level)
    ch.setFormatter(fmt_console)
    logger.addHandler(ch)

    log_dir.mkdir(parents=True, exist_ok=True)
    fh = RotatingFileHandler(
        filename    = str(log_dir / LOG_FILE),
        maxBytes    = LOG_MAX_BYTES,
        backupCount = LOG_BACKUP_COUNT,
        encoding    = "utf-8",
    )
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(fmt_file)
    logger.addHandler(fh)

    logger.info(
        "Logging activo → consola [%s] + '%s'",
        level_str, log_dir / LOG_FILE,
    )
    return logger

def _insert_analysis_result(
    analysis_type:    str,
    result_payload:   Any,
    model_name:       Optional[str]  = None,
    model_version:    Optional[str]  = "1.0",
    confidence_score: Optional[float] = None,
    input_data_id:    Optional[str]  = None,
    logger:           Optional[logging.Logger] = None,
) -> Optional[str]:

    analysis_id    = str(uuid.uuid4())
    _input_data_id = input_data_id if input_data_id else None

    sql = """
        INSERT INTO analysis_results
            (analysis_id, input_data_id, analysis_type, model_name,
             model_version, result, confidence_score)
        VALUES
            (%s, %s, %s, %s, %s, %s, %s)
    """
    params = (
        analysis_id,
        _input_data_id,
        analysis_type,
        model_name,
        model_version,
        json.dumps(result_payload, default=str),
        confidence_score,
    )

    conexion = None
    try:
        conexion = Connect_BD.crear_conexion()
        if conexion is None:
            raise RuntimeError("Connect_BD.crear_conexion() devolvió None.")

        with conexion.cursor() as cur:
            cur.execute(sql, params)
        conexion.commit()

        if logger:
            logger.debug(
                "analysis_results ← INSERT OK | analysis_id=%s | type=%s",
                analysis_id, analysis_type,
            )
        return analysis_id

    except Exception as exc:
        if logger:
            logger.error(
                "Error al insertar en analysis_results: %s: %s",
                type(exc).__name__, exc,
            )
        return None

    finally:
        if conexion:
            Connect_BD.cerrar_conexion(conexion)


def _insert_audit_log(
    process_name:   str,
    batch_id:       str,
    elapsed_s:      float,
    log_level:      str           = "INFO",
    analysis_id:    Optional[str] = None,
    error_message:  Optional[str] = None,
    metadata:       Optional[dict] = None,
    logger:         Optional[logging.Logger] = None,
) -> None:

    elapsed_interval = f"{elapsed_s:.6f} seconds"

    sql = """
        INSERT INTO audit_logs
            (process_name, log_level, batch_id, analysis_id,
             error_message, execution_time, metadata)
        VALUES
            (%s, %s, %s, %s, %s, %s::interval, %s)
    """
    params = (
        process_name,
        log_level,
        batch_id,
        analysis_id,
        error_message,
        elapsed_interval,
        json.dumps(metadata, default=str) if metadata else None,
    )

    conexion = None
    try:
        conexion = Connect_BD.crear_conexion()
        if conexion is None:
            raise RuntimeError("Connect_BD.crear_conexion() devolvió None.")

        with conexion.cursor() as cur:
            cur.execute(sql, params)
        conexion.commit()

        if logger:
            logger.debug(
                "audit_logs ← INSERT OK | batch=%s | phase='%s' | %.3fs",
                batch_id, process_name, elapsed_s,
            )

    except Exception as exc:
        if logger:
            logger.error(
                "Error al insertar en audit_logs: %s: %s",
                type(exc).__name__, exc,
            )

    finally:
        if conexion:
            Connect_BD.cerrar_conexion(conexion)


def _insert_error_log(
    trace_id:       str,
    exception_name: str,
    message:        str,
    stack_trace:    str,
    logger:         Optional[logging.Logger] = None,
) -> None:

    sql = """
        INSERT INTO error_logs
            (trace_id, exception_name, message, stack_trace)
        VALUES
            (%s, %s, %s, %s)
    """
    params = (trace_id, exception_name, message, stack_trace)

    conexion = None
    try:
        conexion = Connect_BD.crear_conexion()
        if conexion is None:
            raise RuntimeError("Connect_BD.crear_conexion() devolvió None.")

        with conexion.cursor() as cur:
            cur.execute(sql, params)
        conexion.commit()

        if logger:
            logger.debug(
                "error_logs ← INSERT OK | trace_id=%s | exc=%s",
                trace_id, exception_name,
            )

    except Exception as exc:
        if logger:
            logger.error(
                "Error al insertar en error_logs: %s: %s",
                type(exc).__name__, exc,
            )

    finally:
        if conexion:
            Connect_BD.cerrar_conexion(conexion)

@contextmanager
def _phase(
    logger:      logging.Logger,
    num:         int,
    name:        str,
    batch_id:    str,
    analysis_id: Optional[str] = None,
):

    sep = "─" * 60
    logger.info(sep)
    logger.info("▶  FASE %d — %s", num, name.upper())
    logger.info(sep)
    t0 = time.perf_counter()
    try:
        yield
        elapsed = time.perf_counter() - t0
        logger.info("✔  FASE %d completada en %.3f s — %s", num, elapsed, name)

        _insert_audit_log(
            process_name  = name,
            batch_id      = batch_id,
            elapsed_s     = elapsed,
            log_level     = "INFO",
            analysis_id   = analysis_id,
            error_message = None,
            logger        = logger,
        )

    except Exception as exc:
        elapsed = time.perf_counter() - t0
        tb_str  = traceback.format_exc()
        logger.critical(
            "✘  FASE %d FALLÓ tras %.3f s — %s\n"
            "   %s: %s\n%s",
            num, elapsed, name,
            type(exc).__name__, exc,
            "".join(tb_str.splitlines(keepends=True)),
        )

        _insert_audit_log(
            process_name  = name,
            batch_id      = batch_id,
            elapsed_s     = elapsed,
            log_level     = "ERROR",
            analysis_id   = analysis_id,
            error_message = f"{type(exc).__name__}: {exc}",
            logger        = logger,
        )

        raise PipelineError(num, name, exc) from exc

class PipelineError(RuntimeError):
    def __init__(self, phase_num: int, phase_name: str, original: Exception) -> None:
        self.phase_num  = phase_num
        self.phase_name = phase_name
        self.original   = original
        super().__init__(
            f"Pipeline detenido en Fase {phase_num} ({phase_name}): "
            f"{type(original).__name__}: {original}"
        )

def _load_dataframe_from_db(
    start_date: Optional[str],
    end_date:   Optional[str],
    logger:     logging.Logger,
) -> pd.DataFrame:
    logger.info(
        "Conectando a PostgreSQL via Connect_BD para extraer '%s' "
        "(start=%s | end=%s | solo registros sin anomalías)...",
        PROCESSED_DATA_TABLE, start_date or "—", end_date or "—",
    )

    conditions = ["1=1"]
    params: list = []

    if start_date:
        conditions.append("event_date >= %s")
        params.append(start_date)

    if end_date:
        conditions.append("event_date <= %s")
        params.append(end_date)

    where_clause = " AND ".join(conditions)
    query = f"SELECT * FROM {PROCESSED_DATA_TABLE} WHERE {where_clause}"

    conexion = Connect_BD.crear_conexion()
    if conexion is None:
        raise RuntimeError(
            "Connect_BD.crear_conexion() devolvió None. "
            "Verifica las variables de entorno en el .env y que el servidor "
            "PostgreSQL esté accesible."
        )

    try:
        df = pd.read_sql(query, conexion, params=tuple(params) or None)
    except Exception as exc:
        raise RuntimeError(
            f"Error al ejecutar la consulta sobre '{PROCESSED_DATA_TABLE}': {exc}"
        ) from exc
    finally:
        Connect_BD.cerrar_conexion(conexion)

    if df.empty:
        raise RuntimeError(
            f"La tabla '{PROCESSED_DATA_TABLE}' no devolvió filas con los "
            f"filtros aplicados (start={start_date}, end={end_date}). "
            "Verifica que la Fase 2 haya completado su ejecución."
        )

    cols_to_drop = [c for c in df.columns if c.lower() in _META_COLS]
    if cols_to_drop:
        df.drop(columns=cols_to_drop, inplace=True)
        logger.debug("Columnas de metadatos eliminadas del DataFrame: %s", cols_to_drop)

    for col in df.columns:
        if df[col].apply(lambda x: isinstance(x, (dict, list))).any():
            logger.debug("Convirtiendo columna JSON/dict a string: %s", col)
            df[col] = df[col].astype(str)

    logger.info(
        "DataFrame cargado desde PostgreSQL → %d filas, %d columnas",
        len(df), len(df.columns),
    )
    return df

class DataAIPipelineManager:

    def __init__(
        self,
        artifacts_dir: str | Path    = ARTIFACTS_DIR,
        output_dir:    str | Path    = OUTPUT_DIR,
        log_level:     str           = "INFO",
        knn_task:      str           = "classification",
        n_reco:        int           = 5,
        test_size:     float         = 0.2,
        random_state:  int           = 42,
        target_col:    Optional[str] = None,
        start_date:    Optional[str] = None,
        end_date:      Optional[str] = None,
    ) -> None:
        self.artifacts_dir = Path(artifacts_dir).resolve()
        self.output_dir    = Path(output_dir).resolve()
        self.knn_task      = knn_task
        self.n_reco        = n_reco
        self.test_size     = test_size
        self.random_state  = random_state
        self.target_col    = target_col
        self.start_date    = start_date
        self.end_date      = end_date

        self.artifacts_dir.mkdir(parents=True, exist_ok=True)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        self.logger = _setup_logging(log_level, log_dir=Path("."))

        self._state: dict[str, Any] = {
            "df":                     None,
            "X":                      None,
            "y":                      None,
            "feature_names":          None,
            "fe":                     None,
            "trainer":                None,
            "X_test":                 None,
            "y_test":                 None,
            "y_pred":                 None,
            "validation_report":      None,
            "prediction_results":     None,
            "recommender":            None,
            "recommendation_results": None,
            "report_paths":           {},
            "analysis_id_pred":       None,
            "analysis_id_reco":       None,
            "batch_id":               None,
            "input_data_id":          None,
        }

        self.logger.info(
            "DataAIPipelineManager listo\n"
            "  artifacts  → %s\n"
            "  output     → %s\n"
            "  task       = %s | n_reco = %d | random_state = %d\n"
            "  db_source  = Connect_BD → %s [%s → %s]",
            self.artifacts_dir, self.output_dir,
            knn_task, n_reco, random_state,
            PROCESSED_DATA_TABLE,
            start_date or "inicio",
            end_date   or "hoy",
        )

    def run(self, df: Optional[pd.DataFrame] = None) -> dict[str, Any]:
        t0 = time.perf_counter()

        batch_id      = str(uuid.uuid4())
        input_data_id = str(uuid.uuid4())
        self._state["batch_id"]      = batch_id
        self._state["input_data_id"] = input_data_id

        self.logger.info("=" * 60)
        self.logger.info("  INICIANDO PIPELINE COMPLETO")
        self.logger.info("  batch_id  = %s", batch_id)
        self.logger.info("  %s", datetime.now(timezone.utc).isoformat())
        self.logger.info("=" * 60)

        if df is None:
            df = _load_dataframe_from_db(
                start_date = self.start_date,
                end_date   = self.end_date,
                logger     = self.logger,
            )

        self._state["df"] = df

        with _phase(self.logger, 1, PHASE_NAMES[1], batch_id=batch_id):
            self._fase_1_feature_engineering()

        with _phase(self.logger, 2, PHASE_NAMES[2], batch_id=batch_id):
            self._fase_2_entrenamiento()

        with _phase(self.logger, 3, PHASE_NAMES[3], batch_id=batch_id):
            self._fase_3_validacion(batch_id=batch_id)

        analysis_id_pred: Optional[str] = None
        with _phase(self.logger, 4, PHASE_NAMES[4], batch_id=batch_id, analysis_id=None):
            analysis_id_pred = self._fase_4_prediccion(input_data_id=input_data_id)
            self._state["analysis_id_pred"] = analysis_id_pred

        with _phase(self.logger, 5, PHASE_NAMES[5], batch_id=batch_id):
            self._fase_5_knn_recommender()

        analysis_id_reco: Optional[str] = None
        with _phase(self.logger, 6, PHASE_NAMES[6], batch_id=batch_id, analysis_id=None):
            analysis_id_reco = self._fase_6_recomendacion(input_data_id=input_data_id)
            self._state["analysis_id_reco"] = analysis_id_reco

        with _phase(self.logger, 7, PHASE_NAMES[7], batch_id=batch_id):
            self._fase_7_reportes()

        elapsed = time.perf_counter() - t0

        self.logger.info("=" * 60)
        self.logger.info("  PIPELINE COMPLETADO en %.2f s", elapsed)
        self.logger.info("  analysis_id_pred = %s", analysis_id_pred)
        self.logger.info("  analysis_id_reco = %s", analysis_id_reco)
        self.logger.info("=" * 60)

        knn_plot_data: list[dict] = []
        try:
            X_test = self._state.get("X_test")
            y_test = self._state.get("y_test")
            y_pred = self._state.get("y_pred")

            if X_test is not None and y_test is not None and y_pred is not None:
                MAX_PLOT_SAMPLES = 5_000

                if len(X_test) > MAX_PLOT_SAMPLES:
                    rng = np.random.default_rng(self.random_state)
                    plot_indices = rng.choice(len(X_test), size=MAX_PLOT_SAMPLES, replace=False)
                    X_plot = X_test[plot_indices]
                    y_plot = y_test[plot_indices]
                    yp_plot = np.asarray(y_pred)[plot_indices]
                    self.logger.info(
                        "knn_plot_data [sampling]: %d → %d puntos para el frontend.",
                        len(X_test), MAX_PLOT_SAMPLES,
                    )
                else:
                    X_plot = X_test
                    y_plot = y_test
                    yp_plot = np.asarray(y_pred)

                pca = PCA(n_components=2, svd_solver="randomized", random_state=self.random_state)
                coords = pca.fit_transform(X_plot)

                knn_plot_data = [
                    {
                        "x": float(coords[i, 0]),
                        "y": float(coords[i, 1]),
                        "clase_real": str(y_plot[i]),
                        "prediccion": str(yp_plot[i]),
                    }
                    for i in range(len(coords))
                ]
                self.logger.info(
                    "knn_plot_data generado — %d puntos (svd_solver='randomized').",
                    len(knn_plot_data),
                )
            else:
                self.logger.warning(
                    "knn_plot_data omitido: X_test / y_test / y_pred "
                    "no disponibles en _state."
                )

        except Exception as knn_exc:
            self.logger.error("Error generando knn_plot_data: %s", knn_exc)

        #persistir grafica knn
        if knn_plot_data:
            knn_json_path = self.output_dir / "knn_plot_data.json"
            try:
                payload = {
                    "generated_at": datetime.now(timezone.utc).isoformat(),
                    "batch_id": batch_id,
                    "points": knn_plot_data,
                }
                with open(knn_json_path, "w", encoding="utf-8") as _f:
                    json.dump(knn_plot_data, _f, ensure_ascii=False, indent=2)
                self.logger.info("knn_plot_data persistido → %s", knn_json_path)
            except Exception as _json_exc:
                self.logger.error("No se pudo guardar knn_plot_data.json: %s", _json_exc)
        return {
            "validation_report":      self._state["validation_report"],
            "prediction_results":     self._state["prediction_results"],
            "recommendation_results": self._state["recommendation_results"],
            "report_paths":           self._state["report_paths"],
            "elapsed_s":              round(elapsed, 3),
            "batch_id":               batch_id,
            "analysis_id_pred":       analysis_id_pred,
            "analysis_id_reco":       analysis_id_reco,
            "knn_plot_data":          knn_plot_data,   # ← siempre presente ([] si falla)
        }

    def _fase_1_feature_engineering(self) -> None:
        df     = self._state["df"]
        target = self._resolve_target(df)

        self.logger.info("Fase 1: target detectado → '%s'", target)

        date_cols = [
            c for c in df.columns
            if any(k in c.lower() for k in ("date", "fecha", "_at", "_on"))
        ]

        fe = FeatureEngineer(df)
        fe.generate_features(date_cols=date_cols or None)
        fe.scale_numerics()
        fe.encode_categoricals()
        fe.select_features(
            variance_threshold    = 0.0,
            correlation_threshold = 0.95,
            exclude               = [target] if target else [],
        )

        result_df = fe.result

        if target and target in result_df.columns:
            self._state["y"] = result_df[target].values
            X_df = result_df.drop(columns=[target])
        else:
            num = result_df.select_dtypes(include=[np.number])
            self._state["y"] = num.iloc[:, -1].values
            X_df = num.iloc[:, :-1]

        self._state["X"]             = X_df.values.astype(float)
        self._state["feature_names"] = list(X_df.columns)
        self._state["fe"]            = fe

        self.logger.info(
            "Fase 1: X=%s | y=%s | scalers=%d | encoders=%d | dropped=%s",
            self._state["X"].shape,
            self._state["y"].shape,
            len(fe.scalers),
            len(fe.encoders),
            fe.dropped_cols,
        )

    def _fase_2_entrenamiento(self) -> None:
        X = self._state["X"]
        y = self._state["y"]

        MAX_TRAIN_SAMPLES = 15_000

        if len(X) > MAX_TRAIN_SAMPLES:
            rng = np.random.default_rng(self.random_state)
            indices = rng.choice(len(X), size=MAX_TRAIN_SAMPLES, replace=False)
            X_fit = X[indices]
            y_fit = y[indices]
            self.logger.info(
                "Fase 2 [sampling]: %d → %d filas para entrenamiento KNN.",
                len(X), MAX_TRAIN_SAMPLES,
            )
        else:
            X_fit = X
            y_fit = y

        trainer = KNNModelTrainer(task=self.knn_task, random_state=self.random_state)
        (
            trainer
            .split(X_fit, y_fit, test_size=self.test_size)
            .tune(cv=3, verbose=0)
            .fit()
            .save_model(output_dir=str(self.artifacts_dir))
            .predict()
        )

        self._state["trainer"] = trainer
        self._state["X_test"] = trainer.X_test
        self._state["y_test"] = trainer.y_test
        self._state["y_pred"] = trainer.y_pred_

        self.logger.info(
            "Fase 2: best_params=%s | CV_score=%.4f | modelo guardado en '%s'",
            trainer.best_params_,
            trainer.best_score_,
            self.artifacts_dir / "knn_model.joblib",
        )

    def _fase_3_validacion(self, batch_id: str) -> None:
        y_test = self._state["y_test"]
        y_pred = self._state["y_pred"]

        validator = ModelValidator(
            y_true     = y_test,
            y_pred     = y_pred,
            model_name = f"KNN-{self.knn_task}",
        )
        report = validator.generate_validation_report(average="weighted")
        self._state["validation_report"] = report

        m = report.get("metrics", {})
        self.logger.info(
            "Fase 3: accuracy=%.4f | f1=%.4f | status=%s",
            m.get("accuracy", 0),
            m.get("f1_score",  0),
            report.get("status", "?"),
        )

        validation_metadata = {
            "accuracy":  m.get("accuracy"),
            "f1_score":  m.get("f1_score"),
            "precision": m.get("precision"),
            "recall":    m.get("recall"),
            "status":    report.get("status"),
            "model":     f"KNN-{self.knn_task}",
        }
        _insert_audit_log(
            process_name  = f"{PHASE_NAMES[3]} — métricas",
            batch_id      = batch_id,
            elapsed_s     = 0.0,
            log_level     = "INFO",
            analysis_id   = None,
            error_message = None,
            metadata      = validation_metadata,
            logger        = self.logger,
        )

    def _fase_4_prediccion(self, input_data_id: str) -> Optional[str]:
        trainer       = self._state["trainer"]
        X_test        = self._state["X_test"]
        y_test        = self._state["y_test"]
        feature_names = self._state["feature_names"]  # noqa: F841

        model  = trainer.model_
        y_pred = model.predict(X_test)

        results           = []
        confidence_scores = []

        for i, (pred, true) in enumerate(zip(y_pred, y_test)):  # noqa: B007
            confidence = None
            if hasattr(model, "predict_proba"):
                try:
                    proba      = model.predict_proba(X_test[i].reshape(1, -1))[0]
                    classes    = [str(c) for c in model.classes_]
                    top_score  = round(float(max(proba)), 4)
                    confidence = {
                        "score":                 top_score,
                        "probability_per_class": {
                            cls: round(float(p), 4)
                            for cls, p in zip(classes, proba)
                        },
                        "method": "predict_proba",
                    }
                    confidence_scores.append(top_score)
                except Exception:
                    pass

            results.append({
                "status":     "success",
                "id":         str(i),
                "prediction": str(pred),
                "confidence": confidence,
                "meta": {
                    "task":       self.knn_task,
                    "model_type": type(model).__name__,
                },
            })

        self._state["prediction_results"] = results

        avg_confidence = (
            round(float(np.mean(confidence_scores)), 4)
            if confidence_scores else None
        )

        analysis_id = _insert_analysis_result(
            analysis_type    = "knn_prediction",
            result_payload   = results,
            model_name       = f"KNN-{self.knn_task}",
            model_version    = "1.0",
            confidence_score = avg_confidence,
            input_data_id    = None,
            logger           = self.logger,
        )

        self.logger.info(
            "Fase 4: %d predicciones generadas | confidence_avg=%.4f | "
            "analysis_id=%s",
            len(results),
            avg_confidence or 0.0,
            analysis_id,
        )
        return analysis_id

    def _fase_5_knn_recommender(self) -> None:
        X = self._state["X"]
        feature_names = self._state["feature_names"]

        MAX_RECO_SAMPLES = 15_000

        if len(X) > MAX_RECO_SAMPLES:
            rng = np.random.default_rng(self.random_state)
            indices = rng.choice(len(X), size=MAX_RECO_SAMPLES, replace=False)
            X_reco = X[indices]
            self.logger.info(
                "Fase 5 [sampling]: %d → %d filas para KNNRecommender.",
                len(X), MAX_RECO_SAMPLES,
            )
        else:
            X_reco = X

        knn_df = pd.DataFrame(X_reco, columns=feature_names)
        knn_df.insert(0, "row_id", [str(i) for i in range(len(X_reco))])

        recommender = KNNRecommender(
            n_recommendations=self.n_reco,
            metric="cosine",
        )
        recommender.fit(knn_df, id_col="row_id")
        recommender.save_model(output_dir=str(self.artifacts_dir))

        self._state["recommender"] = recommender

        self.logger.info(
            "Fase 5: KNNRecommender ajustado → %d registros | n_reco=%d | "
            "motor guardado en '%s'",
            recommender.n_samples_,
            self.n_reco,
            self.artifacts_dir / "knn_recommender.joblib",
        )

    def _fase_6_recomendacion(self, input_data_id: str) -> Optional[str]:
        raw_df        = self._state["df"]
        recommender   = self._state["recommender"]
        X             = self._state["X"]
        feature_names = self._state["feature_names"]

        items_df, products_df, processed_df = self._build_reco_tables(
            raw_df,
            X             = X,
            feature_names = feature_names,
        )

        engine = RecommendationEngine(
            items_df     = items_df,
            products_df  = products_df,
            processed_df = processed_df,
            recommender  = recommender,
        )

        cart_id = str(uuid.uuid4())
        cart_item_df = pd.DataFrame({
            "cart_id":    [cart_id],
            "variant_id": [str(items_df.iloc[0]["id"])],
            "quantity":   [1],
        })

        result = engine.get_cross_selling(
            cart_item_df = cart_item_df,
            cart_id      = cart_id,
            top_k        = self.n_reco,
        )
        self._state["recommendation_results"] = result

        analysis_id = _insert_analysis_result(
            analysis_type    = "knn_recommendation",
            result_payload   = result,
            model_name       = "KNNRecommender",
            model_version    = "1.0",
            confidence_score = None,
            input_data_id    = None,
            logger           = self.logger,
        )

        self.logger.info(
            "Fase 6: %d recomendaciones generadas | tipo='%s' | cold_start=%s | "
            "analysis_id=%s",
            result.get("count", 0),
            result.get("recommendation_type", "N/A"),
            result.get("is_cold_start", False),
            analysis_id,
        )
        return analysis_id

    def _fase_7_reportes(self) -> None:
        preds = self._state["prediction_results"] or []
        recos = self._state["recommendation_results"] or {}

        manager = ReportManager(
            prediction_payload     = preds,
            recommendation_payload = recos,
            model_name             = f"KNN-{self.knn_task}",
            db_conn                = None,
            output_dir             = str(self.output_dir),
        )
        manager.build_report()

        paths: dict[str, Optional[str]] = {}
        paths["json"] = manager.export_to_json(str(self.output_dir))
        paths["csv"]  = manager.export_to_csv(str(self.output_dir))

        try:
            paths["pdf"] = manager.export_to_pdf(
                str(self.output_dir),
                return_base64 = False,
            )
        except Exception as exc:
            self.logger.warning("Fase 7: export_to_pdf falló → %s", exc)
            paths["pdf"] = None

        self._state["report_paths"] = paths

        self.logger.info(
            "Fase 7: reportes exportados\n"
            "  JSON → %s\n"
            "  CSV  → %s\n"
            "  PDF  → %s",
            paths["json"], paths["csv"], paths["pdf"],
        )

    def _resolve_target(self, df: pd.DataFrame) -> Optional[str]:
        if self.target_col and self.target_col in df.columns:
            return self.target_col

        candidates = [
            "target", "label", "y", "clase", "class",
            "category", "categoria", "status", "churn",
            "ventas", "sales", "demand", "demanda",
        ]
        for col in candidates:
            if col in df.columns:
                return col

        return df.columns[-1] if len(df.columns) > 1 else None

    def _build_reco_tables(
        self,
        raw_df:        Optional[pd.DataFrame],
        X:             Optional[np.ndarray] = None,
        feature_names: Optional[list]       = None,
    ) -> tuple:

        import uuid as _uuid

        n = max(len(raw_df) if raw_df is not None else 10, 5)

        item_ids    = [str(_uuid.uuid4()) for _ in range(n)]
        product_ids = (
            raw_df["product_id"].astype(str).tolist()
            if raw_df is not None and "product_id" in raw_df.columns
            else [str(_uuid.uuid4()) for _ in range(n)]
        )

        if raw_df is not None and "base_price" in raw_df.columns:
            prices = raw_df["base_price"].fillna(0).astype(float).tolist()[:n]
        elif raw_df is not None and "amount" in raw_df.columns:
            prices = raw_df["amount"].fillna(0).astype(float).tolist()[:n]
        else:
            prices = np.random.uniform(10, 500, n).round(2).tolist()

        items_df = pd.DataFrame({
            "id":         item_ids,
            "product_id": product_ids[:n],
            "base_price": prices,
            "visible":    [True] * n,
            "stock":      np.random.randint(1, 100, n),
        })

        products_df = pd.DataFrame({
            "id":          product_ids[:n],
            "category_id": [str(_uuid.uuid4()) for _ in range(n)],
            "brand":       [f"Brand-{i % 4}" for i in range(n)],
            "name":        [f"Producto-{i}" for i in range(n)],
        })

        if X is not None and feature_names is not None:
            processed_df = pd.DataFrame(X[:n], columns=feature_names)
            processed_df.insert(0, "user_id",    [str(_uuid.uuid4()) for _ in range(n)])
            processed_df.insert(1, "product_id", product_ids[:n])
            processed_df["amount"]     = prices
            processed_df["event_date"] = pd.date_range("2024-01-01", periods=n, freq="min")
        else:
            self.logger.warning(
                "_build_reco_tables: X o feature_names no disponibles; "
                "processed_df tendrá solo 4 columnas → se activará cold_start."
            )
            processed_df = pd.DataFrame({
                "user_id":    [str(_uuid.uuid4()) for _ in range(n)],
                "product_id": product_ids[:n],
                "amount":     prices,
                "event_date": pd.date_range("2024-01-01", periods=n, freq="min"),
            })

        return items_df, products_df, processed_df

    def __repr__(self) -> str:
        return (
            f"DataAIPipelineManager("
            f"task='{self.knn_task}', "
            f"n_reco={self.n_reco}, "
            f"artifacts='{self.artifacts_dir}', "
            f"output='{self.output_dir}')"
        )

def _build_cli() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog            = "main_pipeline.py",
        description     = "Pipeline Orchestrator — PM/IA/ (Fases 1-7)",
        formatter_class = argparse.RawDescriptionHelpFormatter,
        epilog = """
Ejemplos:
  python main_pipeline.py
  python main_pipeline.py --run-all
  python main_pipeline.py --run-all --artifacts ./artifacts --output ./reports
  python main_pipeline.py --run-all --knn-task regression --log-level DEBUG
  python main_pipeline.py --run-all --target ventas --n-reco 10
  python main_pipeline.py --run-all --start 2024-01-01 --end 2024-12-31
        """,
    )
    parser.add_argument(
        "--run-all",
        action  = "store_true",
        default = True,
        help    = "Ejecuta todo el pipeline de inicio a fin (default: True).",
    )
    parser.add_argument(
        "--start",
        default = None,
        metavar = "YYYY-MM-DD",
        help    = "Fecha inicio para filtrar processed_data.",
    )
    parser.add_argument(
        "--end",
        default = None,
        metavar = "YYYY-MM-DD",
        help    = "Fecha fin para filtrar processed_data.",
    )
    parser.add_argument(
        "--artifacts",
        default = ARTIFACTS_DIR,
        metavar = "DIR",
        help    = f"Directorio de artefactos. Default: {ARTIFACTS_DIR}",
    )
    parser.add_argument(
        "--output",
        default = OUTPUT_DIR,
        metavar = "DIR",
        help    = f"Directorio de reportes. Default: {OUTPUT_DIR}",
    )
    parser.add_argument(
        "--log-level",
        default = "INFO",
        choices = ["DEBUG", "INFO", "WARNING", "ERROR"],
        help    = "Nivel de logging. Default: INFO",
    )
    parser.add_argument(
        "--knn-task",
        default = "classification",
        choices = ["classification", "regression"],
        help    = "Tipo de tarea KNN. Default: classification",
    )
    parser.add_argument(
        "--n-reco",
        type    = int,
        default = 5,
        metavar = "N",
        help    = "Número de recomendaciones. Default: 5",
    )
    parser.add_argument(
        "--test-size",
        type    = float,
        default = 0.2,
        metavar = "FLOAT",
        help    = "Proporción del conjunto de prueba. Default: 0.2",
    )
    parser.add_argument(
        "--random-state",
        type    = int,
        default = 42,
        metavar = "INT",
        help    = "Semilla aleatoria. Default: 42",
    )
    parser.add_argument(
        "--target",
        default = None,
        metavar = "COL",
        help    = "Nombre exacto de la columna target. None = detección automática.",
    )
    return parser

def main() -> int:
    parser = _build_cli()
    args   = parser.parse_args()

    manager = DataAIPipelineManager(
        artifacts_dir = args.artifacts,
        output_dir    = args.output,
        log_level     = args.log_level,
        knn_task      = args.knn_task,
        n_reco        = args.n_reco,
        test_size     = args.test_size,
        random_state  = args.random_state,
        target_col    = args.target,
        start_date    = args.start,
        end_date      = args.end,
    )

    try:
        result = manager.run()

        manager.logger.info(
            "Pipeline finalizado exitosamente en %.2f s\n"
            "  Reportes     → %s\n"
            "  batch_id     = %s\n"
            "  analysis_ids → pred=%s | reco=%s\n"
            "  knn_plot_data → %d puntos",
            result["elapsed_s"],
            result["report_paths"],
            result.get("batch_id"),
            result.get("analysis_id_pred"),
            result.get("analysis_id_reco"),
            len(result.get("knn_plot_data", [])),
        )
        return 0

    except PipelineError as exc:
        manager.logger.critical(
            "PIPELINE DETENIDO en Fase %d (%s)\n"
            "  Causa: %s: %s\n"
            "  Revisa '%s' para el traceback completo.",
            exc.phase_num, exc.phase_name,
            type(exc.original).__name__, exc.original,
            LOG_FILE,
        )

        batch_id = manager._state.get("batch_id") or str(uuid.uuid4())
        _insert_error_log(
            trace_id       = batch_id,
            exception_name = type(exc.original).__name__,
            message        = str(exc.original),
            stack_trace    = traceback.format_exc(),
            logger         = manager.logger,
        )
        return 1

    except KeyboardInterrupt:
        manager.logger.warning("Pipeline interrumpido por el usuario (Ctrl+C).")
        return 1

    except Exception as exc:
        manager.logger.critical(
            "Error inesperado: %s: %s",
            type(exc).__name__, exc, exc_info=True,
        )

        batch_id = manager._state.get("batch_id") or str(uuid.uuid4())
        _insert_error_log(
            trace_id       = batch_id,
            exception_name = type(exc).__name__,
            message        = str(exc),
            stack_trace    = traceback.format_exc(),
            logger         = manager.logger,
        )
        return 1

if __name__ == "__main__":
    sys.exit(main())