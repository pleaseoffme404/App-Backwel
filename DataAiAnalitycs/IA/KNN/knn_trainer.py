from __future__ import annotations
import os
import joblib
import logging
import warnings
from typing import Optional, Union
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.neighbors import KNeighborsClassifier, KNeighborsRegressor
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix,
    mean_squared_error,
    mean_absolute_error,
    r2_score,
)

warnings.filterwarnings("ignore")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("KNNModelTrainer")

ArrayLike = Union[np.ndarray, pd.DataFrame, pd.Series]



class KNNModelTrainer:

    _VALID_TASKS = {"classification", "regression"}

    def __init__(
        self,
        task:         str = "classification",
        random_state: int = 42,
    ) -> None:
        if task not in self._VALID_TASKS:
            raise ValueError(f"'task' debe ser uno de {self._VALID_TASKS}. Recibido: '{task}'")

        self.task         = task
        self.random_state = random_state

        # Particiones
        self.X_train: Optional[np.ndarray] = None
        self.X_test:  Optional[np.ndarray] = None
        self.y_train: Optional[np.ndarray] = None
        self.y_test:  Optional[np.ndarray] = None

        # Resultados del pipeline
        self.best_params_: Optional[dict]       = None
        self.best_score_:  Optional[float]      = None
        self.model_:       Optional[object]     = None
        self.y_pred_:      Optional[np.ndarray] = None

        # GridSearchCV
        self._grid_search: Optional[GridSearchCV] = None

        logger.info("KNNModelTrainer inicializado. task='%s', random_state=%d",
                    task, random_state)

    def split(
        self,
        X:          ArrayLike,
        y:          ArrayLike,
        test_size:  float = 0.2,
        stratify:   bool  = True,
    ) -> "KNNModelTrainer":

        X_arr = np.array(X)
        y_arr = np.array(y).ravel()

        use_stratify = (
            stratify and self.task == "classification"
        )

        self.X_train, self.X_test, self.y_train, self.y_test = train_test_split(
            X_arr,
            y_arr,
            test_size    = test_size,
            random_state = self.random_state,
            stratify     = y_arr if use_stratify else None,
        )

        logger.info(
            "split: train=%d filas | test=%d filas | stratify=%s",
            len(self.X_train), len(self.X_test), use_stratify,
        )
        return self

    def tune(
        self,
        param_grid: Optional[dict]  = None,
        cv:         int             = 5,
        scoring:    Optional[str]   = None,
        n_jobs:     int             = -1,
        verbose:    int             = 1,
    ) -> "KNNModelTrainer":

        self._check_split()

        grid = param_grid or {
            "n_neighbors": list(range(3, 16, 2)),
            "weights":     ["uniform", "distance"],
            "metric":      ["euclidean", "manhattan"],
        }

        default_scoring = {
            "classification": "f1_weighted",
            "regression":     "neg_root_mean_squared_error",
        }
        scoring = scoring or default_scoring[self.task]

        estimator = (
            KNeighborsClassifier()
            if self.task == "classification"
            else KNeighborsRegressor()
        )

        self._grid_search = GridSearchCV(
            estimator  = estimator,
            param_grid = grid,
            cv         = cv,
            scoring    = scoring,
            n_jobs     = n_jobs,
            verbose    = verbose,
            refit      = True,
        )

        logger.info(
            "tune: iniciando GridSearchCV | grid_size=%d | cv=%d | scoring='%s'",
            _grid_size(grid), cv, scoring,
        )

        self._grid_search.fit(self.X_train, self.y_train)

        self.best_params_ = self._grid_search.best_params_
        self.best_score_  = self._grid_search.best_score_

        logger.info("tune: mejores parámetros → %s", self.best_params_)
        logger.info("tune: mejor score CV     → %.4f", self.best_score_)

        return self


    def fit(
        self,
        use_best_estimator: bool = True,
    ) -> "KNNModelTrainer":

        self._check_tuned()

        if use_best_estimator:
            self.model_ = self._grid_search.best_estimator_
            logger.info("fit: usando best_estimator_ de GridSearchCV (ya ajustado).")
        else:
            klass = (
                KNeighborsClassifier
                if self.task == "classification"
                else KNeighborsRegressor
            )
            self.model_ = klass(**self.best_params_)
            self.model_.fit(self.X_train, self.y_train)
            logger.info("fit: modelo reconstruido y ajustado con params=%s", self.best_params_)

        return self

    def predict(
        self,
        X: Optional[ArrayLike] = None,
    ) -> np.ndarray:

        self._check_fitted()

        X_arr = np.array(X) if X is not None else self.X_test
        self.y_pred_ = self.model_.predict(X_arr)
        logger.info("predict: %d predicciones generadas.", len(self.y_pred_))
        return self.y_pred_

    def evaluate(
        self,
        digits:  int  = 4,
        verbose: bool = True,
    ) -> dict:

        self._check_predicted()

        if self.task == "classification":
            result = self._evaluate_classification(digits=digits, verbose=verbose)
        else:
            result = self._evaluate_regression(verbose=verbose)

        result["best_params"]    = self.best_params_
        result["best_cv_score"]  = round(float(self.best_score_), digits)
        return result

    def save_model(self, output_dir: str, filename: str = "knn_model.joblib") -> "KNNModelTrainer":

        self._check_fitted()

        os.makedirs(output_dir, exist_ok=True)

        filepath = os.path.join(output_dir, filename)

        joblib.dump(self.model_, filepath)
        logger.info("save_model: Artefacto guardado exitosamente en '%s'", filepath)

        return self

    def _evaluate_classification(self, digits: int, verbose: bool) -> dict:
        acc  = accuracy_score(self.y_test, self.y_pred_)
        prec = precision_score(self.y_test, self.y_pred_,
                               average="weighted", zero_division=0)
        rec  = recall_score(self.y_test, self.y_pred_,
                            average="weighted", zero_division=0)
        f1   = f1_score(self.y_test, self.y_pred_,
                        average="weighted", zero_division=0)
        cr   = classification_report(self.y_test, self.y_pred_, digits=digits)
        cm   = confusion_matrix(self.y_test, self.y_pred_)

        if verbose:
            _print_section("Evaluación — Clasificación KNN")
            _print_kv("Accuracy",  acc)
            _print_kv("Precision", prec)
            _print_kv("Recall",    rec)
            _print_kv("F1-Score",  f1)
            print("\nClassification Report:\n")
            print(cr)
            print("Matriz de Confusión:\n")
            print(cm)

        return {
            "accuracy":         round(acc,  4),
            "precision":        round(prec, 4),
            "recall":           round(rec,  4),
            "f1_score":         round(f1,   4),
            "report":           cr,
            "confusion_matrix": cm,
        }

    def _evaluate_regression(self, verbose: bool) -> dict:
        rmse = float(np.sqrt(mean_squared_error(self.y_test, self.y_pred_)))
        mae  = float(mean_absolute_error(self.y_test, self.y_pred_))
        r2   = float(r2_score(self.y_test, self.y_pred_))

        if verbose:
            _print_section("Evaluación — Regresión KNN")
            _print_kv("RMSE", rmse)
            _print_kv("MAE",  mae)
            _print_kv("R²",   r2)

        return {
            "rmse": round(rmse, 6),
            "mae":  round(mae,  6),
            "r2":   round(r2,   6),
        }

    def cv_results(self) -> pd.DataFrame:

        self._check_tuned()
        df = pd.DataFrame(self._grid_search.cv_results_)
        return (
            df[["params", "mean_test_score", "std_test_score", "rank_test_score"]]
            .sort_values("rank_test_score")
            .reset_index(drop=True)
        )

    def _check_split(self) -> None:
        if self.X_train is None:
            raise RuntimeError("Ejecuta split() antes de tune().")

    def _check_tuned(self) -> None:
        if self._grid_search is None:
            raise RuntimeError("Ejecuta tune() antes de fit().")

    def _check_fitted(self) -> None:
        if self.model_ is None:
            raise RuntimeError("Ejecuta fit() antes de predict().")

    def _check_predicted(self) -> None:
        if self.y_pred_ is None:
            raise RuntimeError("Ejecuta predict() antes de evaluate().")


    def __repr__(self) -> str:
        fitted  = self.model_ is not None
        tuned   = self._grid_search is not None
        return (
            f"KNNModelTrainer("
            f"task='{self.task}', "
            f"random_state={self.random_state}, "
            f"tuned={tuned}, "
            f"fitted={fitted}, "
            f"best_params={self.best_params_})"
        )

def _grid_size(grid: dict) -> int:
    size = 1
    for v in grid.values():
        size *= len(v)
    return size


def _print_section(title: str) -> None:
    print(f"\n{'=' * 50}")
    print(f"  {title}")
    print(f"{'=' * 50}")


def _print_kv(label: str, value: float) -> None:
    print(f"  {label:<14}: {value:.6f}")
