from __future__ import annotations
import json
import logging
import time
import warnings
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Optional, Union
import joblib
import numpy as np
import pandas as pd

warnings.filterwarnings("ignore")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("PredictionEngine")

Record      = dict[str, Any]
Batch       = list[Record]
InputData   = Union[Record, Batch, pd.DataFrame, np.ndarray]
TaskType    = Union[str]


class ErrorCode:
    OK                  = "OK"
    LOAD_ERROR          = "E001_LOAD_ERROR"
    MISSING_COLUMNS     = "E002_MISSING_COLUMNS"
    TRANSFORM_ERROR     = "E003_TRANSFORM_ERROR"
    PREDICTION_ERROR    = "E004_PREDICTION_ERROR"
    INVALID_INPUT       = "E005_INVALID_INPUT"
    PROBA_UNAVAILABLE   = "E006_PROBA_UNAVAILABLE"


class PredictionEngine:

    _VALID_TASKS = {"classification", "regression"}

    def __init__(
        self,
        artifacts_dir:       Union[str, Path] = "artifacts",
        task:                TaskType         = "classification",
        model_file:          str              = "model.joblib",
        id_field:            str              = "id",
        confidence_decimals: int              = 4,
    ) -> None:
        if task not in self._VALID_TASKS:
            raise ValueError(
                f"'task' debe ser uno de {self._VALID_TASKS}. Recibido: '{task}'"
            )

        self.artifacts_dir       = Path(artifacts_dir).resolve()
        self.task                = task
        self.id_field            = id_field
        self.confidence_decimals = confidence_decimals

        # Artefactos cargados
        self.model_:         Any                     = None
        self.scalers_:       dict[str, Any]          = {}
        self.encoders_:      dict[str, Any]          = {}
        self.feature_names_: list[str]               = []
        self.label_classes_: Optional[list[str]]     = None

        # Estadsticas de uso
        self._total_predictions: int   = 0
        self._total_errors:      int   = 0
        self._loaded_at:         str   = ""

        self._load_all_artifacts(model_file)


    def _load_all_artifacts(self, model_file: str) -> None:

        if not self.artifacts_dir.exists():
            raise FileNotFoundError(
                f"Directorio de artefactos no encontrado: {self.artifacts_dir}"
            )

        self.model_ = self._load_artifact(
            self.artifacts_dir / model_file,
            required=True,
            label="modelo principal",
        )

        for path in sorted(self.artifacts_dir.glob("scaler_*.joblib")):
            col = path.stem.replace("scaler_", "", 1)
            scaler = self._load_artifact(path, required=False, label=f"scaler[{col}]")
            if scaler is not None:
                self.scalers_[col] = scaler

        for path in sorted(self.artifacts_dir.glob("encoder_*.joblib")):
            col = path.stem.replace("encoder_", "", 1)
            encoder = self._load_artifact(path, required=False, label=f"encoder[{col}]")
            if encoder is not None:
                self.encoders_[col] = encoder

        fn_path = self.artifacts_dir / "feature_names.json"
        if fn_path.exists():
            self.feature_names_ = json.loads(fn_path.read_text(encoding="utf-8"))
            logger.info("Cargado feature_names.json → %d columnas", len(self.feature_names_))
        else:
            logger.warning(
                "feature_names.json no encontrado. "
                "El orden de columnas se inferirá del input."
            )

        lc_path = self.artifacts_dir / "label_classes.json"
        if lc_path.exists():
            self.label_classes_ = json.loads(lc_path.read_text(encoding="utf-8"))
            logger.info("Cargado label_classes.json → %s", self.label_classes_)

        self._loaded_at = datetime.now(timezone.utc).isoformat()
        logger.info(
            "PredictionEngine listo → task='%s' | scalers=%d | encoders=%d",
            self.task, len(self.scalers_), len(self.encoders_),
        )

    @staticmethod
    def _load_artifact(
        path:     Path,
        required: bool = True,
        label:    str  = "artefacto",
    ) -> Optional[Any]:

        if not path.exists():
            if required:
                raise FileNotFoundError(f"Artefacto requerido no encontrado: {path}")
            logger.debug("Artefacto opcional no encontrado (se omite): %s", path)
            return None

        try:
            obj = joblib.load(path)
            logger.info("Cargado %s ← %s", label, path.name)
            return obj
        except Exception as exc:
            raise RuntimeError(
                f"No se pudo deserializar {label} desde '{path}': {exc}"
            ) from exc

    def _transform(self, df: pd.DataFrame) -> np.ndarray:

        df = df.copy()

        for col, enc in self.encoders_.items():
            if col not in df.columns:
                logger.warning("_transform: columna '%s' del encoder ausente → se rellena con None.", col)
                df[col] = None

            try:
                class_name = type(enc).__name__
                val = df[[col]].fillna("__missing__")

                if class_name == "OneHotEncoder":
                    ohe_arr   = enc.transform(val)
                    ohe_cols  = [f"{col}__{cat}" for cat in enc.categories_[0]]
                    ohe_df    = pd.DataFrame(ohe_arr, columns=ohe_cols, index=df.index)
                    df        = pd.concat([df.drop(columns=[col]), ohe_df], axis=1)
                else:
                    df[col] = enc.transform(val).flatten()

            except Exception as exc:
                logger.warning("_transform: encoder '%s' falló → %s. Se imputa con 0.", col, exc)
                df[col] = 0

        for col, scaler in self.scalers_.items():
            if col not in df.columns:
                logger.warning("_transform: columna '%s' del scaler ausente → se rellena con 0.", col)
                df[col] = 0.0
                continue
            try:
                df[col] = scaler.transform(
                    df[[col]].fillna(0).values.astype(float)
                ).flatten()
            except Exception as exc:
                logger.warning("_transform: scaler '%s' falló → %s. Se imputa con 0.", col, exc)
                df[col] = 0.0

        if self.feature_names_:
            missing = [c for c in self.feature_names_ if c not in df.columns]
            if missing:
                raise ValueError(
                    f"Columnas requeridas por el modelo ausentes en el input: {missing}"
                )
            df = df[self.feature_names_]

        n_nan = int(df.isna().sum().sum())
        if n_nan:
            logger.warning("_transform: %d NaN residuales imputados con 0.", n_nan)
            df = df.fillna(0)

        return df.values.astype(float)

    def predict(
        self,
        data:         InputData,
        return_proba: bool = True,
    ) -> Union[dict, list[dict]]:

        is_single = isinstance(data, dict)

        try:
            records = self._normalize_input(data)
        except Exception as exc:
            err = self._error_response(
                ErrorCode.INVALID_INPUT,
                f"Input no válido: {exc}",
                record_id="batch",
            )
            self._total_errors += 1
            return err if is_single else [err]

        responses = []
        for record in records:
            responses.append(
                self._predict_one(record, return_proba=return_proba)
            )

        self._total_predictions += len(responses)
        return responses[0] if is_single else responses

    def _predict_one(
        self,
        record:       Record,
        return_proba: bool,
    ) -> dict:

        t0 = time.perf_counter()
        record_id = record.get(self.id_field, _generate_id())

        try:
            df_raw = pd.DataFrame([{k: v for k, v in record.items()
                                     if k != self.id_field}])
            try:
                X = self._transform(df_raw)
            except ValueError as exc:
                return self._error_response(
                    ErrorCode.MISSING_COLUMNS, str(exc), record_id
                )
            except Exception as exc:
                return self._error_response(
                    ErrorCode.TRANSFORM_ERROR, str(exc), record_id
                )

            try:
                raw_pred = self.model_.predict(X)[0]
            except Exception as exc:
                return self._error_response(
                    ErrorCode.PREDICTION_ERROR, str(exc), record_id
                )

            proba_result = self._compute_confidence(
                X, raw_pred, return_proba
            )

            elapsed_ms = round((time.perf_counter() - t0) * 1000, 2)
            return self._build_response(
                record_id   = record_id,
                raw_pred    = raw_pred,
                proba       = proba_result,
                elapsed_ms  = elapsed_ms,
            )

        except Exception as exc:
            self._total_errors += 1
            return self._error_response(
                ErrorCode.PREDICTION_ERROR,
                f"Error inesperado: {exc}",
                record_id,
            )

    def _compute_confidence(
        self,
        X:           np.ndarray,
        raw_pred:    Any,
        return_proba: bool,
    ) -> dict:

        if self.task == "regression":
            return {
                "confidence_score":     None,
                "probability_per_class": None,
                "method":               "not_applicable_for_regression",
            }

        if not return_proba:
            return {
                "confidence_score":     None,
                "probability_per_class": None,
                "method":               "disabled_by_caller",
            }

        has_proba = hasattr(self.model_, "predict_proba")
        if not has_proba:
            return {
                "confidence_score":     None,
                "probability_per_class": None,
                "method":               ErrorCode.PROBA_UNAVAILABLE,
            }

        try:
            proba_arr = self.model_.predict_proba(X)[0]
            classes   = (
                self.label_classes_
                or [str(c) for c in self.model_.classes_]
            )
            proba_per_class = {
                cls: round(float(p), self.confidence_decimals)
                for cls, p in zip(classes, proba_arr)
            }
            pred_str = str(raw_pred)
            confidence = proba_per_class.get(pred_str, float(max(proba_arr)))

            return {
                "confidence_score":     round(confidence, self.confidence_decimals),
                "probability_per_class": proba_per_class,
                "method":               "predict_proba",
            }

        except Exception as exc:
            logger.warning("_compute_confidence: predict_proba falló → %s", exc)
            return {
                "confidence_score":     None,
                "probability_per_class": None,
                "method":               f"error: {exc}",
            }

    def _build_response(
        self,
        record_id:  Any,
        raw_pred:   Any,
        proba:      dict,
        elapsed_ms: float,
    ) -> dict:

        prediction = (
            float(raw_pred)
            if isinstance(raw_pred, (np.floating, np.integer, float, int))
            else str(raw_pred)
        )

        return {
            "status":     "success",
            "error":      None,
            "id":         _serialize(record_id),
            "prediction": prediction,
            "confidence": {
                "score":                 proba["confidence_score"],
                "probability_per_class": proba["probability_per_class"],
                "method":                proba["method"],
            },
            "meta": {
                "task":         self.task,
                "model_type":   type(self.model_).__name__,
                "predicted_at": datetime.now(timezone.utc).isoformat(),
                "latency_ms":   elapsed_ms,
            },
        }

    @staticmethod
    def _error_response(
        error_code: str,
        message:    str,
        record_id:  Any = None,
    ) -> dict:

        logger.error("[%s] id=%s → %s", error_code, record_id, message)
        return {
            "status":     "error",
            "error":      {
                "code":    error_code,
                "message": message,
            },
            "id":         _serialize(record_id),
            "prediction": None,
            "confidence": None,
            "meta": {
                "predicted_at": datetime.now(timezone.utc).isoformat(),
            },
        }

    def _normalize_input(self, data: InputData) -> list[Record]:

        if isinstance(data, dict):
            return [data]

        if isinstance(data, list):
            if not data:
                raise ValueError("La lista de registros está vacía.")
            if not all(isinstance(r, dict) for r in data):
                raise ValueError(
                    "Todos los elementos de la lista deben ser dicts."
                )
            return data

        if isinstance(data, pd.DataFrame):
            return data.to_dict(orient="records")

        if isinstance(data, np.ndarray):
            arr = data if data.ndim == 2 else data.reshape(1, -1)
            if not self.feature_names_:
                raise ValueError(
                    "No se puede usar ndarray sin feature_names_. "
                    "Carga feature_names.json o pasa un DataFrame/dict."
                )
            return [
                dict(zip(self.feature_names_, row.tolist()))
                for row in arr
            ]

        raise ValueError(
            f"Tipo de input no admitido: {type(data).__name__}. "
            "Usa dict, list[dict], pd.DataFrame o np.ndarray."
        )

    @staticmethod
    def save_artifacts(
        model:         Any,
        artifacts_dir: Union[str, Path],
        scalers:       Optional[dict] = None,
        encoders:      Optional[dict] = None,
        feature_names: Optional[list[str]] = None,
        label_classes: Optional[list[str]] = None,
        model_file:    str = "model.joblib",
    ) -> Path:

        dest = Path(artifacts_dir).resolve()
        dest.mkdir(parents=True, exist_ok=True)

        joblib.dump(model, dest / model_file)
        logger.info("save_artifacts: modelo guardado → %s", dest / model_file)

        for col, scaler in (scalers or {}).items():
            joblib.dump(scaler, dest / f"scaler_{col}.joblib")

        for col, encoder in (encoders or {}).items():
            joblib.dump(encoder, dest / f"encoder_{col}.joblib")

        if feature_names:
            (dest / "feature_names.json").write_text(
                json.dumps(feature_names, ensure_ascii=False), encoding="utf-8"
            )

        if label_classes:
            (dest / "label_classes.json").write_text(
                json.dumps(label_classes, ensure_ascii=False), encoding="utf-8"
            )

        logger.info(
            "save_artifacts: artefactos guardados en '%s' "
            "(scalers=%d, encoders=%d)",
            dest, len(scalers or {}), len(encoders or {}),
        )
        return dest

    def health(self) -> dict:

        return {
            "status":            "healthy",
            "model_type":        type(self.model_).__name__ if self.model_ else None,
            "task":              self.task,
            "n_scalers":         len(self.scalers_),
            "n_encoders":        len(self.encoders_),
            "feature_count":     len(self.feature_names_),
            "label_classes":     self.label_classes_,
            "loaded_at":         self._loaded_at,
            "total_predictions": self._total_predictions,
            "total_errors":      self._total_errors,
        }

    def __repr__(self) -> str:
        return (
            f"PredictionEngine("
            f"task='{self.task}', "
            f"model={type(self.model_).__name__ if self.model_ else None}, "
            f"scalers={list(self.scalers_)}, "
            f"encoders={list(self.encoders_)}, "
            f"features={len(self.feature_names_)})"
        )

def _generate_id() -> str:
    import uuid
    return str(uuid.uuid4())


def _serialize(value: Any) -> Any:
    if isinstance(value, (np.integer,)):  return int(value)
    if isinstance(value, (np.floating,)): return float(value)
    if isinstance(value, np.ndarray):     return value.tolist()
    return value
