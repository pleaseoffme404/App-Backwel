from __future__ import annotations
import logging
from typing import Optional
import numpy as np
import pandas as pd
from sklearn.preprocessing import (
    StandardScaler,
    RobustScaler,
    OneHotEncoder,
    OrdinalEncoder,
)
from sklearn.feature_selection import VarianceThreshold

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("FeatureEngineer")


def _numeric_cols(df: pd.DataFrame) -> list[str]:
    return df.select_dtypes(include=[np.number]).columns.tolist()


def _categorical_cols(df: pd.DataFrame) -> list[str]:
    return df.select_dtypes(include=["object", "category", "string"]).columns.tolist()


def _has_outliers(series: pd.Series, iqr_factor: float = 1.5) -> bool:

    s = series.dropna()
    if s.empty:
        return False
    q1, q3 = s.quantile(0.25), s.quantile(0.75)
    iqr = q3 - q1
    lower, upper = q1 - iqr_factor * iqr, q3 + iqr_factor * iqr
    return bool(((s < lower) | (s > upper)).any())

class FeatureEngineer:


    def __init__(self, df: pd.DataFrame) -> None:
        if not isinstance(df, pd.DataFrame):
            raise TypeError("El parámetro 'df' debe ser un pandas DataFrame.")
        self.result: pd.DataFrame = df.copy()
        self.scalers:      dict = {}
        self.encoders:     dict = {}
        self.dropped_cols: list = []
        logger.info("FeatureEngineer inicializado. Shape: %s", self.result.shape)

    def scale_numerics(
        self,
        cols:           Optional[list[str]] = None,
        iqr_factor:     float = 1.5,
        fill_strategy:  str   = "median",
    ) -> "FeatureEngineer":

        targets = cols or _numeric_cols(self.result)
        if not targets:
            logger.warning("scale_numerics: no se encontraron columnas numéricas.")
            return self

        for col in targets:
            if col not in self.result.columns:
                logger.warning("scale_numerics: columna '%s' no existe, se omite.", col)
                continue

            series = self.result[col].astype("float64")

            if series.isna().any():
                if fill_strategy == "median":
                    fill_val = series.median()
                elif fill_strategy == "mean":
                    fill_val = series.mean()
                else:
                    fill_val = 0
                series = series.fillna(fill_val)
                logger.debug("scale_numerics: NaN en '%s' imputados con %s=%.4f",
                             col, fill_strategy, fill_val)

            scaler = RobustScaler() if _has_outliers(series, iqr_factor) else StandardScaler()
            scaled  = scaler.fit_transform(series.values.reshape(-1, 1)).flatten()

            self.result[col] = scaled
            self.scalers[col] = scaler
            logger.info("scale_numerics: '%s' → %s", col, type(scaler).__name__)

        return self

    def encode_categoricals(
        self,
        nominal_cols:     Optional[list[str]]       = None,
        ordinal_mappings: Optional[dict[str, list]] = None,
        max_cardinality:  int                        = 20,
        drop_original:    bool                       = True,
    ) -> "FeatureEngineer":

        ordinal_mappings = ordinal_mappings or {}

        for col, order in ordinal_mappings.items():
            if col not in self.result.columns:
                logger.warning("encode_categoricals: columna ordinal '%s' no existe.", col)
                continue

            filled = self.result[col].fillna(order[0])
            enc    = OrdinalEncoder(
                categories=[order],
                handle_unknown="use_encoded_value",
                unknown_value=-1,
            )
            self.result[col] = enc.fit_transform(
                filled.values.reshape(-1, 1)
            ).flatten()
            self.encoders[col] = enc
            logger.info("encode_categoricals: '%s' → OrdinalEncoder (%d niveles)", col, len(order))

        all_cat    = _categorical_cols(self.result)
        ordinal_ks = set(ordinal_mappings.keys())
        targets    = nominal_cols if nominal_cols is not None else [
            c for c in all_cat if c not in ordinal_ks
        ]

        for col in targets:
            if col not in self.result.columns:
                logger.warning("encode_categoricals: columna nominal '%s' no existe.", col)
                continue

            n_unique = self.result[col].nunique(dropna=True)
            if n_unique > max_cardinality:
                logger.warning(
                    "encode_categoricals: '%s' tiene %d categorías únicas (> %d), se omite.",
                    col, n_unique, max_cardinality,
                )
                continue

            filled = self.result[col].fillna("__missing__")
            enc    = OneHotEncoder(
                sparse_output=False,
                handle_unknown="ignore",
                dtype=np.int8,
            )
            ohe_arr = enc.fit_transform(filled.values.reshape(-1, 1))
            ohe_df  = pd.DataFrame(
                ohe_arr,
                columns=[f"{col}__{cat}" for cat in enc.categories_[0]],
                index=self.result.index,
            )

            self.result     = pd.concat([self.result, ohe_df], axis=1)
            self.encoders[col] = enc
            if drop_original:
                self.result.drop(columns=[col], inplace=True)

            logger.info(
                "encode_categoricals: '%s' → OneHotEncoder (%d dummies)", col, ohe_arr.shape[1]
            )

        return self

    def select_features(
        self,
        variance_threshold:   float = 0.0,
        correlation_threshold: float = 0.95,
        exclude:              Optional[list[str]] = None,
    ) -> "FeatureEngineer":

        exclude      = set(exclude or [])
        num_cols     = [c for c in _numeric_cols(self.result) if c not in exclude]
        self.dropped_cols = []

        if not num_cols:
            logger.warning("select_features: no hay columnas numéricas para filtrar.")
            return self

        df_num = self.result[num_cols].fillna(0)

        vt      = VarianceThreshold(threshold=variance_threshold)
        vt.fit(df_num)
        low_var = [col for col, keep in zip(num_cols, vt.get_support()) if not keep]
        if low_var:
            self.result.drop(columns=low_var, inplace=True, errors="ignore")
            self.dropped_cols.extend(low_var)
            logger.info("select_features (varianza): eliminadas %s", low_var)

        remaining = [c for c in num_cols if c not in low_var and c in self.result.columns]
        if len(remaining) > 1:
            corr_matrix = self.result[remaining].fillna(0).corr().abs()
            upper       = corr_matrix.where(
                np.triu(np.ones(corr_matrix.shape), k=1).astype(bool)
            )
            high_corr = [
                col for col in upper.columns
                if (upper[col] >= correlation_threshold).any()
            ]
            if high_corr:
                self.result.drop(columns=high_corr, inplace=True, errors="ignore")
                self.dropped_cols.extend(high_corr)
                logger.info("select_features (correlación): eliminadas %s", high_corr)

        logger.info(
            "select_features: %d columnas eliminadas. Shape final: %s",
            len(self.dropped_cols), self.result.shape,
        )
        return self

    def generate_features(
        self,
        date_cols:    Optional[list[str]]              = None,
        ratios:       Optional[list[tuple[str, str]]]  = None,
        differences:  Optional[list[tuple[str, str]]]  = None,
        flag_nulls:   Optional[list[str]]               = None,
    ) -> "FeatureEngineer":

        for col in (flag_nulls or []):
            if col not in self.result.columns:
                logger.warning("generate_features: columna '%s' no existe para flag_nulls.", col)
                continue
            new_col = f"{col}_was_null"
            self.result[new_col] = self.result[col].isna().astype(np.int8)
            logger.info("generate_features: '%s' creada.", new_col)

        for col in (date_cols or []):
            if col not in self.result.columns:
                logger.warning("generate_features: columna de fecha '%s' no existe.", col)
                continue
            try:
                parsed = pd.to_datetime(self.result[col], errors="coerce", utc=False)
                if parsed.isna().all():
                    logger.warning("generate_features: '%s' no pudo parsearse como fecha.", col)
                    continue
                self.result[f"{col}_year"]       = parsed.dt.year.astype("Int16")
                self.result[f"{col}_month"]      = parsed.dt.month.astype("Int8")
                self.result[f"{col}_day"]        = parsed.dt.day.astype("Int8")
                self.result[f"{col}_dayofweek"]  = parsed.dt.dayofweek.astype("Int8")
                self.result[f"{col}_is_weekend"] = (
                    parsed.dt.dayofweek >= 5
                ).astype(np.int8)
                logger.info("generate_features: 5 columnas extraídas de '%s'.", col)
            except Exception as exc:
                logger.error("generate_features: error procesando fecha '%s': %s", col, exc)

        for num, den in (ratios or []):
            if num not in self.result.columns or den not in self.result.columns:
                logger.warning("generate_features: ratio '%s/%s' — columna no encontrada.", num, den)
                continue
            new_col = f"{num}_div_{den}"
            denom   = self.result[den].replace(0, np.nan)
            self.result[new_col] = (self.result[num] / denom).astype(float)
            logger.info("generate_features: ratio '%s' creado.", new_col)

        for a, b in (differences or []):
            if a not in self.result.columns or b not in self.result.columns:
                logger.warning("generate_features: diferencia '%s-%s' — columna no encontrada.", a, b)
                continue
            new_col = f"{a}_minus_{b}"
            self.result[new_col] = self.result[a] - self.result[b]
            logger.info("generate_features: diferencia '%s' creada.", new_col)

        return self


    def summary(self) -> pd.DataFrame:

        info = pd.DataFrame({
            "dtype":    self.result.dtypes,
            "nulls":    self.result.isna().sum(),
            "null_pct": (self.result.isna().mean() * 100).round(2),
            "unique":   self.result.nunique(),
        })
        return info

    def __repr__(self) -> str:
        return (
            f"FeatureEngineer("
            f"shape={self.result.shape}, "
            f"scalers={list(self.scalers)}, "
            f"encoders={list(self.encoders)}, "
            f"dropped={self.dropped_cols})"
        )
