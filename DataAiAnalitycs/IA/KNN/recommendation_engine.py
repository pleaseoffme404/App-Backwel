from __future__ import annotations
try:
    from Backend_IA.phase_4.knn_recommender import KNNRecommender
    from Backend_IA.phase_2.feature_engineer import FeatureEngineer
except ImportError:
    KNNRecommender  = None
    FeatureEngineer = None

import json
import logging
import time
import uuid
import warnings
from datetime import datetime, timezone
from typing import Any, Optional, Union

import numpy as np
import pandas as pd

warnings.filterwarnings("ignore")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("RecommendationEngine")

class RecoType:
    CROSS_SELLING   = "cross_selling"
    WISHLIST_PROMO  = "wishlist_promo"
    COLD_START      = "cold_start"
    EMPTY           = "empty"


class StatusCode:
    OK            = 200
    EMPTY         = 200
    BAD_REQUEST   = 400
    NOT_FOUND     = 404
    SERVER_ERROR  = 500



class RecommendationEngine:


    def __init__(
        self,
        items_df:         pd.DataFrame,
        products_df:      pd.DataFrame,
        processed_df:     pd.DataFrame,
        recommender:      Optional[Any]          = None,
        feature_engineer: Optional[Any]          = None,
        knn_matrix_df:    Optional[pd.DataFrame] = None,
        n_cold_start:     int                    = 10,
    ) -> None:
        self._items     = self._validate_df(items_df,    "items_df",    ["id", "product_id", "base_price", "visible"])
        self._products  = self._validate_df(products_df, "products_df", ["id"])
        self._processed = self._validate_df(processed_df,"processed_df",["user_id", "product_id"])

        self.n_cold_start = n_cold_start

        self._fe = feature_engineer

        self._rec = self._init_recommender(recommender, knn_matrix_df)

        self._item_index: dict[str, int] = {
            str(iid): i for i, iid in enumerate(self._items["id"].astype(str))
        }

        logger.info(
            "RecommendationEngine listo → items=%d | products=%d | processed=%d | knn=%s",
            len(self._items), len(self._products), len(self._processed),
            type(self._rec).__name__ if self._rec else "None",
        )



    def get_cross_selling(
        self,
        cart_item_df: pd.DataFrame,
        cart_id:      str,
        top_k:        int = 5,
    ) -> dict:

        t0 = time.perf_counter()

        try:
            _validate_uuid(cart_id, "cart_id")


            cart_rows = cart_item_df[
                cart_item_df["cart_id"].astype(str) == cart_id
            ]
            if cart_rows.empty:
                return self._empty_response(
                    RecoType.CROSS_SELLING, cart_id,
                    reason=f"cart_id '{cart_id}' no encontrado en cart_item.",
                )


            cart_items = cart_rows.merge(
                self._items.rename(columns={"id": "item_id"}),
                left_on  = "variant_id",
                right_on = "item_id",
                how      = "left",
            )

            cart_items = cart_items.merge(
                self._products,
                left_on  = "product_id",
                right_on = "id",
                how      = "left",
                suffixes = ("", "_prod"),
            )

            already_in_cart = set(cart_items["variant_id"].astype(str).tolist())


            query_vector = self._build_cart_vector(cart_items)
            if query_vector is None:
                return self._cold_start(top_k, RecoType.CROSS_SELLING, cart_id)


            raw_recos = self._query_knn(query_vector, top_k * 3)


            filtered = self._filter_visible(raw_recos, exclude_ids=already_in_cart)
            top      = filtered[:top_k]

            if not top:
                return self._cold_start(top_k, RecoType.CROSS_SELLING, cart_id)

            return self._build_response(
                recommendation_type = RecoType.CROSS_SELLING,
                entity_id           = cart_id,
                recommendations     = top,
                elapsed_ms          = _elapsed(t0),
            )

        except _UUIDError as exc:
            return self._error_response(StatusCode.BAD_REQUEST, str(exc), RecoType.CROSS_SELLING)
        except Exception as exc:
            logger.error("get_cross_selling: %s", exc, exc_info=True)
            return self._error_response(StatusCode.SERVER_ERROR, str(exc), RecoType.CROSS_SELLING)



    def get_wishlist_promos(
        self,
        wish_list_df:    pd.DataFrame,
        wish_item_df:    pd.DataFrame,
        discount_df:     pd.DataFrame,
        discount_tgt_df: pd.DataFrame,
        user_id:         str,
        top_k:           int = 3,
    ) -> dict:

        t0 = time.perf_counter()

        try:
            _validate_uuid(user_id, "user_id")


            user_lists = wish_list_df[
                wish_list_df["user_id"].astype(str) == user_id
            ]
            if user_lists.empty:
                logger.info("get_wishlist_promos: user_id '%s' sin wishlist → cold start.", user_id)
                return self._cold_start(top_k, RecoType.WISHLIST_PROMO, user_id)


            wish_items = user_lists.merge(
                wish_item_df,
                left_on  = "id",
                right_on = "wish_list_id",
                how      = "inner",
            )
            if wish_items.empty:
                return self._cold_start(top_k, RecoType.WISHLIST_PROMO, user_id)

            wish_item_ids = set(wish_items["item_id"].astype(str).tolist())


            wish_with_features = wish_items.merge(
                self._items.rename(columns={"id": "item_id_feat"}),
                left_on  = "item_id",
                right_on = "item_id_feat",
                how      = "left",
            )


            active_discounts = self._get_active_discounts(discount_df, discount_tgt_df)
            discounted_item_ids = set(active_discounts["item_id"].astype(str).tolist())


            query_vector = self._build_cart_vector(wish_with_features)
            if query_vector is None:
                return self._cold_start(top_k, RecoType.WISHLIST_PROMO, user_id)


            raw_recos = self._query_knn(query_vector, top_k * 5)


            filtered = self._filter_visible(raw_recos, exclude_ids=wish_item_ids)
            on_sale  = [r for r in filtered if r["item_id"] in discounted_item_ids]


            pct_map = dict(
                zip(active_discounts["item_id"].astype(str),
                    active_discounts["pct_off"])
            )
            for r in on_sale:
                r["pct_off"] = float(pct_map.get(r["item_id"], 0.0))

            top = on_sale[:top_k]

            if not top:

                top = filtered[:top_k]
                for r in top:
                    r["pct_off"] = 0.0

            if not top:
                return self._cold_start(top_k, RecoType.WISHLIST_PROMO, user_id)

            return self._build_response(
                recommendation_type = RecoType.WISHLIST_PROMO,
                entity_id           = user_id,
                recommendations     = top,
                elapsed_ms          = _elapsed(t0),
            )

        except _UUIDError as exc:
            return self._error_response(StatusCode.BAD_REQUEST, str(exc), RecoType.WISHLIST_PROMO)
        except Exception as exc:
            logger.error("get_wishlist_promos: %s", exc, exc_info=True)
            return self._error_response(StatusCode.SERVER_ERROR, str(exc), RecoType.WISHLIST_PROMO)



    def _filter_visible(
        self,
        recommendations: list[dict],
        exclude_ids:     Optional[set[str]] = None,
    ) -> list[dict]:

        exclude = exclude_ids or set()
        visible_ids = set(
            self._items.loc[
                self._items["visible"].astype(bool) == True, "id"
            ].astype(str).tolist()
        )

        result = []
        for rec in recommendations:
            iid = rec["item_id"]
            if iid in exclude:
                continue
            if iid not in visible_ids:
                logger.debug("_filter_visible: item '%s' excluido (visible=False).", iid)
                continue
            result.append(rec)

        return result

    def _cold_start(
        self,
        top_k:               int,
        recommendation_type: str,
        entity_id:           str,
    ) -> dict:

        t0 = time.perf_counter()
        logger.info("_cold_start: activado para entity_id='%s'.", entity_id)

        try:
            # Popularidad por product_id
            if not self._processed.empty:
                popularity = (
                    self._processed.groupby("product_id")
                    .size()
                    .reset_index(name="popularity")
                )
            else:
                popularity = pd.DataFrame(columns=["product_id", "popularity"])

            visible_items = self._items[
                self._items["visible"].astype(bool) == True
            ].copy()

            if not popularity.empty:
                ranked = visible_items.merge(
                    popularity,
                    on  = "product_id",
                    how = "left",
                ).fillna({"popularity": 0}).sort_values(
                    "popularity", ascending=False
                )
            else:
                ranked = visible_items.sort_values("base_price", ascending=False)

            top = ranked.head(top_k)

            recs = []
            for _, row in top.iterrows():
                recs.append({
                    "item_id":          str(row["id"]),
                    "base_price":       _safe_float(row.get("base_price", 0.0)),
                    "confidence_score": 0.0,   # cold start no tiene score de modelo
                    "pct_off":          0.0,
                })

            return self._build_response(
                recommendation_type = RecoType.COLD_START,
                entity_id           = entity_id,
                recommendations     = recs,
                elapsed_ms          = _elapsed(t0),
                is_cold_start       = True,
            )

        except Exception as exc:
            logger.error("_cold_start: %s", exc, exc_info=True)
            return self._empty_response(
                recommendation_type, entity_id, reason=f"cold start falló: {exc}"
            )



    def _build_response(
        self,
        recommendation_type: str,
        entity_id:           str,
        recommendations:     list[dict],
        elapsed_ms:          float,
        is_cold_start:       bool = False,
    ) -> dict:

        clean_recos = []
        for r in recommendations:
            clean_recos.append({
                "item_id":          str(r["item_id"]),
                "base_price":       _safe_float(r.get("base_price", 0.0)),
                "confidence_score": _safe_float(r.get("confidence_score", 0.0)),
                "pct_off":          _safe_float(r.get("pct_off", 0.0)),
            })

        return {
            "status_code":         StatusCode.OK,
            "status":              "success",
            "error":               None,
            "recommendation_type": recommendation_type,
            "entity_id":           str(entity_id),
            "is_cold_start":       bool(is_cold_start),
            "recommendations":     clean_recos,
            "count":               len(clean_recos),
            "meta": {
                "generated_at": datetime.now(timezone.utc).isoformat(),
                "latency_ms":   round(float(elapsed_ms), 2),
            },
        }

    @staticmethod
    def _error_response(status_code: int, message: str, reco_type: str) -> dict:

        logger.error("[%d] %s → %s", status_code, reco_type, message)
        return {
            "status_code":         status_code,
            "status":              "error",
            "error":               {"message": message},
            "recommendation_type": reco_type,
            "entity_id":           None,
            "is_cold_start":       False,
            "recommendations":     [],
            "count":               0,
            "meta": {
                "generated_at": datetime.now(timezone.utc).isoformat(),
                "latency_ms":   0.0,
            },
        }

    @staticmethod
    def _empty_response(
        recommendation_type: str,
        entity_id:           str,
        reason:              str = "",
    ) -> dict:

        if reason:
            logger.info("_empty_response [%s]: %s", recommendation_type, reason)
        return {
            "status_code":         StatusCode.EMPTY,
            "status":              "success",
            "error":               None,
            "recommendation_type": recommendation_type,
            "entity_id":           str(entity_id),
            "is_cold_start":       False,
            "recommendations":     [],
            "count":               0,
            "meta": {
                "generated_at": datetime.now(timezone.utc).isoformat(),
                "latency_ms":   0.0,
                "reason":       reason,
            },
        }



    def _init_recommender(
        self,
        recommender:   Optional[Any],
        knn_matrix_df: Optional[pd.DataFrame],
    ) -> Optional[Any]:

        if recommender is not None:
            logger.info("_init_recommender: usando instancia pre-ajustada.")
            return recommender

        if knn_matrix_df is None or knn_matrix_df.empty:
            logger.warning(
                "_init_recommender: sin recommender ni knn_matrix_df. "
                "Las recomendaciones usarán Cold Start."
            )
            return None

        if KNNRecommender is None:
            logger.warning(
                "_init_recommender: KNNRecommender no importado (Backend_IA no disponible). "
                "Usando KNNRecommender local de la demo."
            )
            return None

        try:
            rec = KNNRecommender(n_recommendations=20, metric="cosine")
            rec.fit(knn_matrix_df, id_col="id" if "id" in knn_matrix_df.columns else None)
            logger.info("_init_recommender: KNNRecommender ajustado con %d ítems.", len(knn_matrix_df))
            return rec
        except Exception as exc:
            logger.error("_init_recommender: no se pudo ajustar KNNRecommender → %s", exc)
            return None

    def _build_cart_vector(self, df: pd.DataFrame) -> Optional[np.ndarray]:

        num_cols = df.select_dtypes(include=[np.number]).columns.tolist()

        # Excluir columnas de metadata que no son features útiles para KNN
        exclude = {"quantity", "cart_id", "wish_list_id"}
        feature_cols = [c for c in num_cols if c not in exclude]

        if not feature_cols:
            logger.warning("_build_cart_vector: sin columnas numéricas para construir el vector.")
            return None

        try:
            matrix = df[feature_cols].fillna(0).values.astype(float)
            if matrix.shape[0] == 0:
                return None
            vec = matrix.mean(axis=0)

            # Alinear dimensión al espacio KNN si el modelo está ajustado
            if self._rec is not None and hasattr(self._rec, "_mat") and self._rec._mat is not None:
                n_knn = self._rec._mat.shape[1]
                if len(vec) < n_knn:
                    vec = np.pad(vec, (0, n_knn - len(vec)))
                elif len(vec) > n_knn:
                    vec = vec[:n_knn]

            return vec
        except Exception as exc:
            logger.warning("_build_cart_vector: error al construir vector → %s", exc)
            return None

    def _query_knn(
        self,
        query_vector: np.ndarray,
        n_results:    int,
    ) -> list[dict]:

        if self._rec is None:
            return []

        try:
            knn_results = self._rec.get_recommendations(
                query_vector = query_vector,
                as_dataframe = False,
            )
        except Exception as exc:
            logger.warning("_query_knn: KNN falló → %s. Devolviendo lista vacía.", exc)
            return []


        item_price_map = dict(
            zip(self._items["id"].astype(str),
                self._items["base_price"].apply(_safe_float))
        )

        results = []
        for r in knn_results[:n_results]:
            iid = str(r.get("id", r.get("index", "")))
            results.append({
                "item_id":          iid,
                "base_price":       item_price_map.get(iid, 0.0),
                "confidence_score": _safe_float(r.get("similarity", 0.0) or 0.0),
                "pct_off":          0.0,
            })

        return results

    @staticmethod
    def _get_active_discounts(
        discount_df:     pd.DataFrame,
        discount_tgt_df: pd.DataFrame,
    ) -> pd.DataFrame:

        now = pd.Timestamp.now(tz="UTC")

        active = discount_df[discount_df["active"].astype(bool) == True].copy()


        for col in ("valid_from", "valid_to"):
            if col in active.columns and active[col].dtype == object:
                active[col] = pd.to_datetime(active[col], utc=True, errors="coerce")

        if "valid_from" in active.columns and "valid_to" in active.columns:
            active = active[
                (active["valid_from"] <= now) & (active["valid_to"] >= now)
            ]

        if active.empty:
            return pd.DataFrame(columns=["item_id", "pct_off"])

        merged = active.merge(
            discount_tgt_df,
            left_on  = "id",
            right_on = "discount_id",
            how      = "inner",
        )

        return merged[["item_id", "pct_off"]].drop_duplicates("item_id")

    @staticmethod
    def _validate_df(df: pd.DataFrame, name: str, required_cols: list[str]) -> pd.DataFrame:

        if not isinstance(df, pd.DataFrame):
            raise TypeError(f"'{name}' debe ser un pandas DataFrame.")
        missing = [c for c in required_cols if c not in df.columns]
        if missing:
            raise ValueError(f"'{name}' le faltan columnas: {missing}")
        return df

    def __repr__(self) -> str:
        return (
            f"RecommendationEngine("
            f"items={len(self._items)}, "
            f"products={len(self._products)}, "
            f"processed={len(self._processed)}, "
            f"knn={type(self._rec).__name__ if self._rec else 'None'})"
        )




class _UUIDError(ValueError):
    pass


def _validate_uuid(value: str, field: str) -> None:

    try:
        uuid.UUID(str(value))
    except (ValueError, AttributeError):
        raise _UUIDError(
            f"'{field}' no es un UUID válido: '{value}'. "
            "Se esperaba formato xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx."
        )


def _safe_float(value: Any) -> float:

    try:
        return round(float(value), 6)
    except (TypeError, ValueError):
        return 0.0


def _elapsed(t0: float) -> float:

    return round((time.perf_counter() - t0) * 1000, 2)
