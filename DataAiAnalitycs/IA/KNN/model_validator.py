from __future__ import annotations
import json
import logging
import warnings
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional, Union

import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix,
)

warnings.filterwarnings("ignore")
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("ModelValidator")

LabelArray = Union[np.ndarray, pd.Series, list]

AverageStr = Union[str, None]

class ModelValidator:

    def __init__(
        self,
        y_true:       LabelArray,
        y_pred:       LabelArray,
        class_labels: Optional[list[str]] = None,
        model_name:   str                 = "Model",
    ) -> None:
        self.y_true       = self._validate_input(y_true, "y_true")
        self.y_pred       = self._validate_input(y_pred, "y_pred")
        self.model_name   = model_name

        # Inferir etiquetas si no se pasan
        unique_labels     = np.unique(np.concatenate([self.y_true, self.y_pred]))
        self.class_labels = (
            [str(c) for c in class_labels]
            if class_labels is not None
            else [str(c) for c in unique_labels]
        )
        self.n_classes = len(self.class_labels)

        logger.info(
            "ModelValidator inicializado → modelo='%s' | muestras=%d | clases=%d",
            model_name, len(self.y_true), self.n_classes,
        )
    def compute_metrics(
        self,
        average: AverageStr = "weighted",
    ) -> dict[str, float]:

        try:
            if average == "binary" and self.n_classes > 2:
                raise ValueError(
                    f"average='binary' no es válido para {self.n_classes} clases. "
                    "Usa 'weighted' o 'macro'."
                )

            metrics = {
                "accuracy":  round(float(accuracy_score(self.y_true, self.y_pred)), 6),
                "precision": round(float(
                    precision_score(
                        self.y_true, self.y_pred,
                        average       = average,
                        zero_division = 0,
                    )
                ), 6),
                "recall":    round(float(
                    recall_score(
                        self.y_true, self.y_pred,
                        average       = average,
                        zero_division = 0,
                    )
                ), 6),
                "f1_score":  round(float(
                    f1_score(
                        self.y_true, self.y_pred,
                        average       = average,
                        zero_division = 0,
                    )
                ), 6),
            }

            logger.info(
                "compute_metrics [%s] → accuracy=%.4f | f1=%.4f",
                average, metrics["accuracy"], metrics["f1_score"],
            )
            return metrics

        except Exception as exc:
            logger.error("compute_metrics: %s", exc)
            raise

    def compute_confusion_matrix(
        self,
        as_list:       bool          = True,
        export_plot:   bool          = False,
        plot_path:     str           = "confusion_matrix.png",
        figsize:       tuple[int, int] = (8, 6),
        colormap:      str           = "Blues",
    ) -> dict:

        try:
            cm_array = confusion_matrix(
                self.y_true, self.y_pred,
                labels = np.unique(
                    np.concatenate([self.y_true, self.y_pred])
                ),
            )

            result: dict = {
                "matrix_array": cm_array,
                "matrix_list":  cm_array.tolist() if as_list else None,
                "per_class":    self._cm_per_class(cm_array),
                "plot_path":    None,
            }

            if export_plot:
                result["plot_path"] = self._export_cm_plot(
                    cm_array, plot_path, figsize, colormap
                )

            logger.info(
                "compute_confusion_matrix: matriz %dx%d generada.",
                cm_array.shape[0], cm_array.shape[1],
            )
            return result

        except Exception as exc:
            logger.error("compute_confusion_matrix: %s", exc)
            raise

    def _cm_per_class(self, cm: np.ndarray) -> list[dict]:

        per_class = []
        n         = cm.shape[0]

        for i, label in enumerate(self.class_labels[:n]):
            tp = int(cm[i, i])
            fp = int(cm[:, i].sum() - tp)
            fn = int(cm[i, :].sum() - tp)
            tn = int(cm.sum() - tp - fp - fn)

            prec = tp / (tp + fp) if (tp + fp) > 0 else 0.0
            rec  = tp / (tp + fn) if (tp + fn) > 0 else 0.0
            f1   = (
                2 * prec * rec / (prec + rec)
                if (prec + rec) > 0 else 0.0
            )

            per_class.append({
                "label":     label,
                "TP":        tp,
                "FP":        fp,
                "FN":        fn,
                "TN":        tn,
                "precision": round(prec, 6),
                "recall":    round(rec,  6),
                "f1":        round(f1,   6),
            })

        return per_class

    def _export_cm_plot(
        self,
        cm:       np.ndarray,
        path:     str,
        figsize:  tuple[int, int],
        colormap: str,
    ) -> Optional[str]:

        try:
            import matplotlib
            matplotlib.use("Agg")           # backend sin pantalla (servidores/CI)
            import matplotlib.pyplot as plt
            import seaborn as sns

            fig, ax = plt.subplots(figsize=figsize)
            n       = cm.shape[0]
            labels  = self.class_labels[:n]

            sns.heatmap(
                cm,
                annot       = True,
                fmt         = "d",
                cmap        = colormap,
                xticklabels = labels,
                yticklabels = labels,
                ax          = ax,
                linewidths  = 0.5,
            )
            ax.set_title(f"Matriz de Confusión — {self.model_name}", fontsize=13)
            ax.set_xlabel("Predicho",  fontsize=11)
            ax.set_ylabel("Real",      fontsize=11)
            plt.tight_layout()

            dest = Path(path).resolve()
            dest.parent.mkdir(parents=True, exist_ok=True)
            fig.savefig(dest, dpi=150, bbox_inches="tight")
            plt.close(fig)

            logger.info("_export_cm_plot: figura guardada en '%s'", dest)
            return str(dest)

        except ImportError:
            logger.warning(
                "_export_cm_plot: seaborn/matplotlib no están instalados. "
                "Instálalos con: pip install seaborn matplotlib"
            )
            return None
        except Exception as exc:
            logger.error("_export_cm_plot: error al guardar la figura → %s", exc)
            return None

    def generate_validation_report(
        self,
        average:        AverageStr = "weighted",
        include_report: bool       = True,
        export_cm_plot: bool       = False,
        plot_path:      str        = "confusion_matrix.png",
    ) -> dict:

        report: dict = {
            "metadata": {
                "model_name":   self.model_name,
                "evaluated_at": datetime.now(timezone.utc).isoformat(),
                "n_samples":    int(len(self.y_true)),
                "n_classes":    int(self.n_classes),
                "class_labels": self.class_labels,
                "average":      average,
            },
            "metrics":                {},
            "confusion_matrix":       {},
            "classification_report":  None,
            "status":                 "success",
            "error_message":          None,
        }

        try:
            report["metrics"] = self.compute_metrics(average=average)

            cm_result = self.compute_confusion_matrix(
                as_list     = True,
                export_plot = export_cm_plot,
                plot_path   = plot_path,
            )
            report["confusion_matrix"] = {
                "matrix":    cm_result["matrix_list"],
                "per_class": cm_result["per_class"],
                "plot_path": cm_result["plot_path"],
            }

            if include_report:
                report["classification_report"] = classification_report(
                    self.y_true,
                    self.y_pred,
                    target_names  = self.class_labels[:self.n_classes],
                    zero_division = 0,
                    digits        = 4,
                )

            logger.info(
                "generate_validation_report: reporte generado → status='success'",
            )

        except Exception as exc:
            report["status"]        = "error"
            report["error_message"] = str(exc)
            logger.error("generate_validation_report: %s", exc)

        return report

    def to_json(
        self,
        average:  AverageStr = "weighted",
        indent:   int        = 2,
        path:     Optional[str] = None,
    ) -> str:

        report     = self.generate_validation_report(average=average)
        json_str   = json.dumps(report, indent=indent, default=str)

        if path:
            dest = Path(path).resolve()
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(json_str, encoding="utf-8")
            logger.info("to_json: reporte guardado en '%s'", dest)

        return json_str

    @staticmethod
    def compare(
        reports: dict[str, dict],
        metric:  str = "f1_score",
    ) -> pd.DataFrame:

        rows = []
        for name, rep in reports.items():
            if rep.get("status") == "error":
                logger.warning("compare: '%s' tiene status=error, se omite.", name)
                continue
            m = rep.get("metrics", {})
            rows.append({
                "model":     name,
                "accuracy":  m.get("accuracy"),
                "precision": m.get("precision"),
                "recall":    m.get("recall"),
                "f1_score":  m.get("f1_score"),
            })

        if not rows:
            return pd.DataFrame()

        df = pd.DataFrame(rows).sort_values(metric, ascending=False).reset_index(drop=True)
        return df

    @staticmethod
    def _validate_input(arr: LabelArray, name: str) -> np.ndarray:

        try:
            out = np.array(arr).ravel()
        except Exception as exc:
            raise TypeError(
                f"'{name}' debe ser convertible a numpy array. Error: {exc}"
            ) from exc

        if out.size == 0:
            raise ValueError(f"'{name}' no puede estar vacío.")

        return out

    def __repr__(self) -> str:
        return (
            f"ModelValidator("
            f"model='{self.model_name}', "
            f"n_samples={len(self.y_true)}, "
            f"n_classes={self.n_classes}, "
            f"classes={self.class_labels})"
        )
