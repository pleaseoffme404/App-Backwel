from __future__ import annotations
import json
import warnings
from dataclasses import dataclass, field
from typing import Any
import numpy as np
import pandas as pd
import statsmodels.api as sm
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.preprocessing import StandardScaler
from Backend.data.db_connector import Connect_BD
from sqlalchemy import create_engine
from sqlalchemy import text
import psycopg2

warnings.filterwarnings("ignore")

@dataclass
class ValidationMetrics:
    mae: float
    mse: float
    rmse: float
    r2: float

    def to_dict(self) -> dict[str, float]:
        return {
            "MAE":  round(self.mae,  4),
            "MSE":  round(self.mse,  4),
            "RMSE": round(self.rmse, 4),
            "R2":   round(self.r2,   4),
        }


@dataclass
class ForecastResult:
    predictions: list[dict[str, Any]]
    model_coefficients: dict[str, float]
    validation_metrics: ValidationMetrics
    feature_names: list[str]
    training_period: dict[str, str]
    confidence_intervals: list[dict[str, Any]] = field(default_factory=list)

class LinearSalesForecaster:

    def __init__(
        self,
        target_col: str = "revenue",
        test_size: float = 0.20,
        lags: list[int] | None = None,
        rolling_windows: list[int] | None = None,
        scale_features: bool = True,
        random_state: int = 42,
    ) -> None:
        self.target_col      = target_col
        self.test_size       = test_size
        self.lags            = lags or [1, 7, 14, 28]
        self.rolling_windows = rolling_windows or [7, 14, 30]
        self.scale_features  = scale_features
        self.random_state    = random_state

        self._model: LinearRegression | None = None
        self._sm_model: Any = None
        self._scaler: StandardScaler | None  = None
        self._feature_names: list[str]       = []
        self._metrics: ValidationMetrics | None = None
        self._daily_df: pd.DataFrame | None  = None

    def build_features(
        self,
        raw_df: pd.DataFrame,
        date_col: str   = "date",
        qty_col: str    = "quantity",
        price_col: str  = "unit_price",
        item_col: str | None = None,
    ) -> pd.DataFrame:

        df = raw_df.copy()

        required = {date_col, qty_col, price_col}
        missing   = required - set(df.columns)
        if missing:
            raise ValueError(f"Columnas faltantes en el DataFrame: {missing}")

        df[date_col] = pd.to_datetime(df[date_col], errors="coerce")
        df.dropna(subset=[date_col], inplace=True)

        df[qty_col]   = pd.to_numeric(df[qty_col],   errors="coerce").fillna(0)
        df[price_col] = pd.to_numeric(df[price_col], errors="coerce").fillna(0)
        df["_line_revenue"] = df[qty_col] * df[price_col]

        agg_dict: dict[str, Any] = {
            qty_col:          ("quantity",      "sum"),
            "_line_revenue":  ("revenue",       "sum"),
            price_col:        ("avg_unit_price", "mean"),
        }

        if "active_discount" in df.columns:
            agg_dict["active_discount"] = ("active_discount", "max")

        group_cols = [date_col] + ([item_col] if item_col else [])

        group_cols = [date_col] + ([item_col] if item_col else [])
        daily = (
            df.groupby(group_cols, as_index=False)
              .agg(**{v[0]: pd.NamedAgg(column=k, aggfunc=v[1])
                      for k, v in agg_dict.items()})
        )
        daily.rename(columns={date_col: "date"}, inplace=True)
        daily.set_index("date", inplace=True)
        daily.sort_index(inplace=True)

        full_range = pd.date_range(daily.index.min(), daily.index.max(), freq="D")
        daily = daily.reindex(full_range)
        daily.index.name = "date"

        daily["quantity"]      = daily["quantity"].fillna(0)
        daily["revenue"]       = daily["revenue"].fillna(0)
        daily["avg_unit_price"] = daily["avg_unit_price"].interpolate(
            method="linear", limit_direction="both"
        ).fillna(0)

        if "active_discount" in daily.columns:
            daily["active_discount"] = daily["active_discount"].fillna(0)

        daily["day"]         = daily.index.day
        daily["month"]       = daily.index.month
        daily["year"]        = daily.index.year
        daily["week"]        = daily.index.isocalendar().week.astype(int)
        daily["day_of_week"] = daily.index.dayofweek          # 0=Lun … 6=Dom
        daily["quarter"]     = daily.index.quarter
        daily["is_weekend"]  = (daily["day_of_week"] >= 5).astype(int)
        daily["day_of_year"] = daily.index.dayofyear
        daily["trend"]       = np.arange(len(daily))           # índice lineal

        month_dummies     = pd.get_dummies(
            daily["month"], prefix="month", drop_first=True
        ).astype(int)
        dow_dummies       = pd.get_dummies(
            daily["day_of_week"], prefix="dow", drop_first=True
        ).astype(int)
        quarter_dummies   = pd.get_dummies(
            daily["quarter"], prefix="q", drop_first=True
        ).astype(int)

        daily = pd.concat([daily, month_dummies, dow_dummies, quarter_dummies],
                          axis=1)

        for lag in self.lags:
            daily[f"lag_{lag}d"] = daily[self.target_col].shift(lag)

        for window in self.rolling_windows:
            daily[f"rolling_mean_{window}d"] = (
                daily[self.target_col]
                .shift(1)                        # evita data leakage
                .rolling(window=window, min_periods=1)
                .mean()
            )
            daily[f"rolling_std_{window}d"] = (
                daily[self.target_col]
                .shift(1)
                .rolling(window=window, min_periods=1)
                .std()
                .fillna(0)
            )

        n_before = len(daily)
        daily.dropna(inplace=True)
        n_after  = len(daily)
        removed  = n_before - n_after
        if removed:
            print(f"[build_features]   {removed} filas eliminadas por NaN "
                  f"en lag/rolling (esperado). Registros finales: {n_after}.")

        if daily.empty:
            raise ValueError(
                "El DataFrame quedó vacío tras aplicar lags. "
                "Provea más datos históricos (mínimo max(lags)+1 días)."
            )

        self._daily_df = daily
        print(f"[build_features]  Features generadas. Shape: {daily.shape}")
        return daily

    def train_and_validate(
        self,
        feature_df: pd.DataFrame,
        extra_exclude_cols: list[str] | None = None,
    ) -> ValidationMetrics:

        if self.target_col not in feature_df.columns:
            raise ValueError(
                f"Columna objetivo '{self.target_col}' no encontrada. "
                f"Columnas disponibles: {list(feature_df.columns)}"
            )

        base_exclude = {"quantity", "revenue", "avg_unit_price",
                        "day", "month", "year", "week", "day_of_week",
                        "quarter"}
        if extra_exclude_cols:
            base_exclude.update(extra_exclude_cols)

        base_exclude.add(self.target_col)

        feature_cols = [c for c in feature_df.columns if c not in base_exclude]
        self._feature_names = feature_cols

        X = feature_df[feature_cols].values
        y = feature_df[self.target_col].values

        split_idx = int(len(X) * (1 - self.test_size))
        X_train, X_test = X[:split_idx], X[split_idx:]
        y_train, y_test = y[:split_idx], y[split_idx:]

        train_dates = feature_df.index[:split_idx]
        test_dates  = feature_df.index[split_idx:]

        print(f"[train_and_validate] Train: {train_dates[0].date()} → "
              f"{train_dates[-1].date()} ({len(X_train)} días)")
        print(f"[train_and_validate] Test : {test_dates[0].date()} → "
              f"{test_dates[-1].date()} ({len(X_test)} días)")

        if self.scale_features:
            self._scaler = StandardScaler()
            X_train = self._scaler.fit_transform(X_train)
            X_test  = self._scaler.transform(X_test)

        self._model = LinearRegression()
        self._model.fit(X_train, y_train)

        X_train_sm = sm.add_constant(X_train, has_constant="add")
        self._sm_model = sm.OLS(y_train, X_train_sm).fit()

        y_pred = self._model.predict(X_test)
        y_pred = np.maximum(y_pred, 0)

        mae  = float(mean_absolute_error(y_test, y_pred))
        mse  = float(mean_squared_error(y_test, y_pred))
        rmse = float(np.sqrt(mse))
        r2   = float(r2_score(y_test, y_pred))

        self._metrics = ValidationMetrics(mae=mae, mse=mse, rmse=rmse, r2=r2)

        print(f"[train_and_validate] ✅ Entrenamiento completado.")
        print(f"   MAE={mae:.2f} | MSE={mse:.2f} | RMSE={rmse:.2f} | R²={r2:.4f}")
        return self._metrics



    def forecast(
        self,
        horizon: int = 30,
        last_known_df: pd.DataFrame | None = None,
    ) -> pd.DataFrame:

        if self._model is None:
            raise RuntimeError(
                "Modelo no entrenado. Llame a train_and_validate() primero."
            )

        base_df = last_known_df if last_known_df is not None else self._daily_df
        if base_df is None:
            raise RuntimeError(
                "No hay datos de referencia. Ejecute build_features() primero."
            )

        max_lookback = max(self.lags + self.rolling_windows)
        history = list(base_df[self.target_col].values[-max(max_lookback, 60):])
        last_date = base_df.index[-1]

        rmse_proxy = self._metrics.rmse if self._metrics else 0.0

        future_records: list[dict[str, Any]] = []

        for step in range(1, horizon + 1):
            future_date = last_date + pd.Timedelta(days=step)
            row: dict[str, Any] = {}

            row["trend"]       = len(base_df) + step
            row["day_of_year"] = future_date.dayofyear
            row["is_weekend"]  = int(future_date.dayofweek >= 5)

            for m in range(2, 13):
                row[f"month_{m}"] = int(future_date.month == m)

            for d in range(1, 7):
                row[f"dow_{d}"] = int(future_date.dayofweek == d)

            for q in range(2, 5):
                row[f"q_{q}"] = int(future_date.quarter == q)

            for lag in self.lags:
                idx = -(lag)
                row[f"lag_{lag}d"] = history[idx] if len(history) >= lag else 0.0

            for window in self.rolling_windows:
                recent = history[-window:] if len(history) >= window else history
                row[f"rolling_mean_{window}d"] = float(np.mean(recent))
                row[f"rolling_std_{window}d"]  = float(np.std(recent)) if len(recent) > 1 else 0.0

            x_vec = np.array(
                [row.get(f, 0.0) for f in self._feature_names],
                dtype=float
            ).reshape(1, -1)

            if self.scale_features and self._scaler is not None:
                x_vec = self._scaler.transform(x_vec)

            pred = float(self._model.predict(x_vec)[0])
            pred = max(pred, 0.0)

            ci_factor = 1.96
            lower = max(0.0, pred - ci_factor * rmse_proxy)
            upper = pred + ci_factor * rmse_proxy

            future_records.append({
                "date":              future_date.strftime("%Y-%m-%d"),
                "predicted_revenue": round(pred, 2),
                "lower_bound":       round(lower, 2),
                "upper_bound":       round(upper, 2),
            })

            history.append(pred)

        forecast_df = pd.DataFrame(future_records)
        print(f"[forecast] {horizon} días proyectados desde "
              f"{future_records[0]['date']} hasta {future_records[-1]['date']}.")
        return forecast_df

    def build_report(
        self,
        forecast_df: pd.DataFrame,
        model_version: str = "3B-v1.0",
        item_id: str | None = None,
    ) -> dict[str, Any]:

        if self._model is None or self._metrics is None:
            raise RuntimeError(
                "Ejecute train_and_validate() antes de build_report()."
            )

        sm_summary  = self._sm_model.summary2().tables[1]
        coeff_names = ["_intercept"] + self._feature_names

        model_coefficients: dict[str, Any] = {}
        for i, name in enumerate(coeff_names):
            if i < len(sm_summary):
                row = sm_summary.iloc[i]
                model_coefficients[name] = {
                    "coefficient": round(float(row.get("Coef.",     row.iloc[0])), 6),
                    "std_error":   round(float(row.get("Std.Err.",  row.iloc[1])), 6),
                    "p_value":     round(float(row.get("P>|t|",     row.iloc[3])), 6),
                    "ci_lower":    round(float(row.get("[0.025",     row.iloc[4])), 6),
                    "ci_upper":    round(float(row.get("0.975]",     row.iloc[5])), 6),
                    "significant": bool(float(row.get("P>|t|", row.iloc[3])) < 0.05),
                }

        sklearn_coefs = dict(zip(self._feature_names, self._model.coef_))
        feature_importance = sorted(
            [
                {
                    "feature":    fname,
                    "coefficient": round(float(coef), 6),
                    "abs_weight": round(abs(float(coef)), 6),
                }
                for fname, coef in sklearn_coefs.items()
            ],
            key=lambda x: x["abs_weight"],
            reverse=True,
        )

        pred_values = forecast_df["predicted_revenue"].values
        trend_direction = (
            "upward"   if pred_values[-1] > pred_values[0]  else
            "downward" if pred_values[-1] < pred_values[0]  else
            "flat"
        )

        pct_change = (
            ((pred_values[-1] - pred_values[0]) / pred_values[0] * 100)
            if pred_values[0] != 0 else 0.0
        )

        trend_summary: dict[str, Any] = {
            "direction":            trend_direction,
            "total_pct_change":     round(float(pct_change), 2),
            "first_day_prediction": round(float(pred_values[0]),  2),
            "last_day_prediction":  round(float(pred_values[-1]), 2),
            "mean_prediction":      round(float(np.mean(pred_values)), 2),
            "max_prediction":       round(float(np.max(pred_values)),  2),
            "min_prediction":       round(float(np.min(pred_values)),  2),
            "total_projected_revenue": round(float(np.sum(pred_values)), 2),
            "horizon_days":         len(forecast_df),
        }

        training_period: dict[str, str] = {}
        if self._daily_df is not None:
            training_period = {
                "start": self._daily_df.index.min().strftime("%Y-%m-%d"),
                "end":   self._daily_df.index.max().strftime("%Y-%m-%d"),
                "total_days": str(len(self._daily_df)),
            }

        report: dict[str, Any] = {
            "metadata": {
                "model_version":   model_version,
                "model_type":      "LinearRegression",
                "target_variable": self.target_col,
                "item_id":         item_id,
                "generated_at":    pd.Timestamp.now().isoformat(),
                "training_period": training_period,
                "feature_count":   len(self._feature_names),
                "lags_used":       self.lags,
                "rolling_windows": self.rolling_windows,
            },
            "validation_metrics": self._metrics.to_dict(),
            "model_info": {
                "r_squared":         round(float(self._sm_model.rsquared),     4),
                "adj_r_squared":     round(float(self._sm_model.rsquared_adj), 4),
                "f_statistic":       round(float(self._sm_model.fvalue),       4),
                "f_pvalue":          round(float(self._sm_model.f_pvalue),     6),
                "aic":               round(float(self._sm_model.aic),          2),
                "bic":               round(float(self._sm_model.bic),          2),
                "coefficients":      model_coefficients,
            },
            "feature_importance":    feature_importance,
            "trend_summary":         trend_summary,
            "forecast": forecast_df.to_dict(orient="records"),
        }

        print(f"[build_report] Reporte generado. "
              f"Horizonte: {trend_summary['horizon_days']} días | "
              f"Tendencia: {trend_direction} ({pct_change:+.1f}%)")
        return report


    def to_json(
        self,
        report: dict[str, Any],
        indent: int = 2,
        ensure_ascii: bool = False,
    ) -> str:

        return json.dumps(report, indent=indent, ensure_ascii=ensure_ascii,
                          default=str)

    def summary(self) -> None:
        print("\n" + "=" * 60)
        print("  LinearSalesForecaster — Estado del Modelo")
        print("=" * 60)
        print(f"  Target        : {self.target_col}")
        print(f"  Lags          : {self.lags}")
        print(f"  Rolling wins  : {self.rolling_windows}")
        print(f"  Scale features: {self.scale_features}")
        print(f"  Test size     : {self.test_size:.0%}")
        print(f"  Modelo sklearn: {' Entrenado' if self._model else ' No entrenado'}")
        print(f"  Modelo OLS    : {' Entrenado' if self._sm_model else ' No entrenado'}")
        if self._metrics:
            print(f"\n  Métricas (test set):")
            print(f"    MAE  = {self._metrics.mae:.2f}")
            print(f"    RMSE = {self._metrics.rmse:.2f}")
            print(f"    R²   = {self._metrics.r2:.4f}")
        print("=" * 60 + "\n")


class ErpDataRepository:

    def __init__(self):
        pass

    def fetch_training_data(self, product_id: str | None = None) -> pd.DataFrame:
        query = """
                SELECT pd.event_date                AS date,
                       pd.quantity                  AS quantity,
                       CASE
                           WHEN pd.quantity > 0 THEN (pd.amount / pd.quantity)
                           ELSE COALESCE(iph.base_price, 0)
                           END                      AS unit_price,
                       pd.product_id                AS item_id,
                       p.brand,
                       pd.category,
                       COALESCE(d.decimal_value, 0) AS active_discount
                FROM processed_data pd
                         INNER JOIN product p ON pd.product_id = p.id
                         LEFT JOIN item_price_history iph ON pd.product_id = iph.item_id
                    AND iph.last_update = (SELECT MAX(sub_iph.last_update)
                                           FROM item_price_history sub_iph
                                           WHERE sub_iph.item_id = pd.product_id
                                             AND sub_iph.last_update <= pd.event_date)
                         LEFT JOIN discount_target dt ON pd.product_id = dt.product_id
                         LEFT JOIN discount d ON dt.discount_id = d.id
                    AND pd.event_date BETWEEN d.start_date AND d.end_date
                WHERE pd.status = 'completed' \
                """

        if product_id:
            query += f" AND pd.product_id = '{product_id}'"

        query += " ORDER BY pd.event_date ASC;"

        conexion = Connect_BD.crear_conexion()
        if not conexion:
            raise ConnectionError("No se pudo establecer la conexión con la base de datos.")

        try:
            with warnings.catch_warnings():
                warnings.simplefilter('ignore', UserWarning)
                df = pd.read_sql(query, conexion)
        finally:
            Connect_BD.cerrar_conexion(conexion)

        return df

    def save_analysis_result(self, analysis_type: str, result_report: dict[str, Any]) -> None:

        upsert_query = """
                       INSERT INTO analysis_results (analysis_type, result)
                       VALUES (%(analysis_type)s, %(result)s)
                       ON CONFLICT (analysis_type)
                           DO UPDATE SET result     = EXCLUDED.result,
                                         updated_at = NOW();  \
                       """

        json_data = json.dumps(result_report, default=str)

        conexion = Connect_BD.crear_conexion()
        if not conexion:
            raise ConnectionError("No se pudo establecer la conexión con la base de datos.")

        try:
            cursor = conexion.cursor()
            cursor.execute(
                upsert_query,
                {
                    "analysis_type": analysis_type,
                    "result": json_data
                }
            )
            conexion.commit()
            cursor.close()
            print(f"[ErpDataRepository]  Resultados guardados en 'analysis_results' para tipo: {analysis_type}")
        finally:
            Connect_BD.cerrar_conexion(conexion)