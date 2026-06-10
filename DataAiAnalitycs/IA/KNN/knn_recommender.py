from __future__ import annotations
import logging
import warnings
from typing import Optional, Union
import os
import joblib
import numpy as np
import pandas as pd
from sklearn.neighbors import NearestNeighbors
from sklearn.preprocessing import normalize

warnings.filterwarnings("ignore")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("KNNRecommender")

ArrayLike = Union[np.ndarray, pd.DataFrame, pd.Series, list]


class KNNRecommender:

    _SUPPORTED_METRICS = {"cosine", "euclidean", "manhattan", "minkowski"}

    def __init__(
        self,
        n_recommendations: int = 5,
        metric:            str = "cosine",
        algorithm:         str = "brute",
        n_jobs:            int = -1,
    ) -> None:
        if metric not in self._SUPPORTED_METRICS:
            raise ValueError(
                f"'metric' debe ser uno de {self._SUPPORTED_METRICS}. "
                f"Recibido: '{metric}'"
            )
        if metric == "cosine" and algorithm not in ("brute", "auto"):
            logger.warning(
                "metric='cosine' solo es compatible con algorithm='brute'. "
                "Se fuerza algorithm='brute'."
            )
            algorithm = "brute"

        self.n_recommendations = n_recommendations
        self.metric            = metric
        self.algorithm         = algorithm
        self.n_jobs            = n_jobs

        self._model:          Optional[NearestNeighbors] = None
        self.matrix_:         Optional[np.ndarray]       = None
        self.feature_names_:  Optional[list[str]]        = None
        self.ids_:            Optional[np.ndarray]       = None
        self.n_samples_:      int = 0
        self.n_features_:     int = 0

        logger.info(
            "KNNRecommender inicializado → n=%d | metric='%s' | algorithm='%s'",
            n_recommendations, metric, algorithm,
        )

    def fit(
        self,
        data:   ArrayLike,
        id_col: Optional[str] = None,
    ) -> "KNNRecommender":

        if isinstance(data, pd.DataFrame):
            self.feature_names_ = [
                c for c in data.columns if c != id_col
            ]
            if id_col and id_col in data.columns:
                self.ids_ = data[id_col].values
                matrix    = data[self.feature_names_].values
            else:
                self.ids_ = np.arange(len(data))
                matrix    = data.values
        else:
            matrix              = np.array(data)
            self.ids_           = np.arange(len(matrix))
            self.feature_names_ = None

        if matrix.ndim != 2:
            raise ValueError(
                f"Se esperaba una matriz 2D (n_samples, n_features). "
                f"Shape recibida: {matrix.shape}"
            )
        if len(matrix) < 2:
            raise ValueError("Se necesitan al menos 2 registros para ajustar el motor.")

        if np.isnan(matrix).any():
            n_nan = int(np.isnan(matrix).sum())
            logger.warning(
                "fit: se encontraron %d NaN → reemplazados con 0.", n_nan
            )
            matrix = np.nan_to_num(matrix, nan=0.0)

        if self.metric == "cosine":
            matrix = normalize(matrix, norm="l2")
            logger.debug("fit: normalización L2 aplicada para metric='cosine'.")

        self.matrix_    = matrix
        self.n_samples_ = matrix.shape[0]
        self.n_features_ = matrix.shape[1]

        n_fit = min(self.n_recommendations + 1, self.n_samples_)

        self._model = NearestNeighbors(
            n_neighbors = n_fit,
            metric      = self.metric,
            algorithm   = self.algorithm,
            n_jobs      = self.n_jobs,
        )
        self._model.fit(self.matrix_)

        logger.info(
            "fit: motor ajustado → %d registros | %d features | metric='%s'",
            self.n_samples_, self.n_features_, self.metric,
        )
        return self

    def get_recommendations(
        self,
        query_vector: Optional[ArrayLike] = None,
        index:        Optional[int]       = None,
        as_dataframe: bool                = False,
    ) -> Union[list[dict], pd.DataFrame]:

        self._check_fitted()

        if query_vector is None and index is None:
            raise ValueError("Debes pasar 'query_vector' o 'index'. Ninguno recibido.")
        if query_vector is not None and index is not None:
            raise ValueError("Pasa 'query_vector' o 'index', no ambos simultáneamente.")

        is_index_query = index is not None

        if is_index_query:
            if not (0 <= index < self.n_samples_):
                raise ValueError(
                    f"'index' {index} fuera de rango [0, {self.n_samples_ - 1}]."
                )
            vec = self.matrix_[index].reshape(1, -1)
        else:
            vec = _to_2d(query_vector)

            if self.metric == "cosine":
                vec = normalize(vec, norm="l2")

        if vec.shape[1] != self.n_features_:
            raise ValueError(
                f"El vector tiene {vec.shape[1]} features; "
                f"el modelo fue ajustado con {self.n_features_}."
            )

        distances, indices = self._model.kneighbors(vec)
        distances = distances.flatten()
        indices   = indices.flatten()

        if is_index_query:
            mask      = indices != index
            distances = distances[mask]
            indices   = indices[mask]

        distances = distances[: self.n_recommendations]
        indices   = indices[: self.n_recommendations]

        results = []
        for rank, (idx, dist) in enumerate(zip(indices, distances), start=1):
            similarity = _compute_similarity(dist, self.metric)
            results.append({
                "rank":       rank,
                "id":         _serialize(self.ids_[idx]),
                "index":      int(idx),
                "distance":   round(float(dist), 6),
                "similarity": round(float(similarity), 6) if similarity is not None else None,
            })

        logger.info(
            "get_recommendations: %d resultados devueltos para query=%s",
            len(results),
            f"index={index}" if is_index_query else "vector externo",
        )

        if as_dataframe:
            return pd.DataFrame(results)
        return results


    def similarity_between(self, index_a: int, index_b: int) -> dict:

        self._check_fitted()
        for idx in (index_a, index_b):
            if not (0 <= idx < self.n_samples_):
                raise ValueError(
                    f"Índice {idx} fuera de rango [0, {self.n_samples_ - 1}]."
                )

        va = self.matrix_[index_a].reshape(1, -1)
        vb = self.matrix_[index_b].reshape(1, -1)

        dist_arr, _ = self._model.kneighbors(va)
        from sklearn.metrics.pairwise import (
            cosine_distances, euclidean_distances, manhattan_distances
        )
        dispatch = {
            "cosine":     cosine_distances,
            "euclidean":  euclidean_distances,
            "manhattan":  manhattan_distances,
        }
        dist_fn = dispatch.get(self.metric, euclidean_distances)
        dist    = float(dist_fn(va, vb)[0][0])
        sim     = _compute_similarity(dist, self.metric)

        return {
            "id_a":       _serialize(self.ids_[index_a]),
            "id_b":       _serialize(self.ids_[index_b]),
            "distance":   round(dist, 6),
            "similarity": round(float(sim), 6) if sim is not None else None,
            "metric":     self.metric,
        }

    def get_item_vector(self, index: int) -> dict:

        self._check_fitted()
        if not (0 <= index < self.n_samples_):
            raise ValueError(f"Índice {index} fuera de rango [0, {self.n_samples_ - 1}].")

        return {
            "id":            _serialize(self.ids_[index]),
            "index":         index,
            "vector":        self.matrix_[index].tolist(),
            "feature_names": self.feature_names_,
        }

    def save_model(self, output_dir: str, filename: str = "knn_recommender.joblib") -> "KNNRecommender":

        self._check_fitted()
        os.makedirs(output_dir, exist_ok=True)
        filepath = os.path.join(output_dir, filename)

        joblib.dump(self, filepath)
        logger.info("save_model: Motor guardado exitosamente en '%s'", filepath)

        return self

    def load_model(self, filepath: str) -> "KNNRecommender":

        if not os.path.exists(filepath):
            raise FileNotFoundError(f"No se encontró el artefacto en: '{filepath}'")

        loaded_instance = joblib.load(filepath)
        self.__dict__.update(loaded_instance.__dict__)

        logger.info(
                "load_model: Motor cargado → %d registros | metric='%s'",
                self.n_samples_, self.metric
            )

        return self

    def _check_fitted(self) -> None:
        if self._model is None:
            raise RuntimeError("Ejecuta fit() antes de llamar a este método.")


    def __repr__(self) -> str:
        fitted = self._model is not None
        return (
            f"KNNRecommender("
            f"n_recommendations={self.n_recommendations}, "
            f"metric='{self.metric}', "
            f"fitted={fitted}, "
            f"n_samples={self.n_samples_})"
        )

def _to_2d(arr: ArrayLike) -> np.ndarray:

    a = np.array(arr, dtype=float)
    if a.ndim == 1:
        return a.reshape(1, -1)
    if a.ndim == 2:
        if a.shape[0] != 1:
            raise ValueError(
                f"query_vector debe ser un único registro. "
                f"Shape recibida: {a.shape}. "
                f"Para múltiples consultas llama get_recommendations() en un loop."
            )
        return a
    raise ValueError(f"Dimensión no soportada: {a.ndim}D. Se esperaba 1D o 2D.")


def _compute_similarity(distance: float, metric: str) -> Optional[float]:

    if metric == "cosine":
        return max(0.0, 1.0 - distance)
    if metric == "euclidean":
        return 1.0 / (1.0 + distance)
    return None


def _serialize(value) -> Union[str, int, float]:
    if isinstance(value, (np.integer,)):
        return int(value)
    if isinstance(value, (np.floating,)):
        return float(value)
    return value
