from __future__ import annotations
import json
import logging
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Literal

import numpy as np
import pandas as pd
from scipy import stats as scipy_stats
from IA.RegresionLineal.forecaster import LinearSalesForecaster, ValidationMetrics

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)


ANALYSIS_TYPE: Literal["sales_forecast"] = "sales_forecast"
MODEL_NAME:    Literal["linear_regression"] = "linear_regression"
CONFIDENCE_LEVEL: float = 0.95
Z_SCORE_95:       float = 1.959964



@dataclass(frozen=True)
class DailyForecast:

    date:              str
    predicted_revenue: float
    lower_bound:       float
    upper_bound:       float
    day_of_week:       str
    is_weekend:        bool
    week_number:       int

    def to_dict(self) -> dict[str, Any]:
        return {
            "date":              self.date,
            "predicted_revenue": self.predicted_revenue,
            "lower_bound":       self.lower_bound,
            "upper_bound":       self.upper_bound,
            "day_of_week":       self.day_of_week,
            "is_weekend":        self.is_weekend,
            "week_number":       self.week_number,
        }


@dataclass
class TrendAnalysis:

    direction:               str
    total_pct_change:        float
    avg_daily_revenue:       float
    total_projected_revenue: float
    peak_days:               list[dict[str, Any]]
    valley_days:             list[dict[str, Any]]
    weekly_pattern:          dict[str, float]
    monthly_comparison:      dict[str, Any]
    volatility_index:        float
    growth_rate_daily:       float

    def to_dict(self) -> dict[str, Any]:
        return {
            "direction":               self.direction,
            "total_pct_change":        self.total_pct_change,
            "avg_daily_revenue":       self.avg_daily_revenue,
            "total_projected_revenue": self.total_projected_revenue,
            "peak_days":               self.peak_days,
            "valley_days":             self.valley_days,
            "weekly_pattern":          self.weekly_pattern,
            "monthly_comparison":      self.monthly_comparison,
            "volatility_index":        self.volatility_index,
            "growth_rate_daily":       self.growth_rate_daily,
        }


@dataclass
class ModelEquation:

    intercept:     float
    coefficients:  list[dict[str, Any]]
    equation_str:  str
    r_squared:     float
    adj_r_squared: float
    aic:           float

    def to_dict(self) -> dict[str, Any]:
        return {
            "intercept":     self.intercept,
            "coefficients":  self.coefficients,
            "equation_str":  self.equation_str,
            "r_squared":     self.r_squared,
            "adj_r_squared": self.adj_r_squared,
            "aic":           self.aic,
        }

class SalesInferenceEngine:

    def __init__(
        self,
        forecaster:    LinearSalesForecaster,
        item_id:       str | None = None,
        model_version: str        = "3B-v1.0",
    ) -> None:
        self._validate_forecaster(forecaster)
        self.forecaster    = forecaster
        self.item_id       = item_id
        self.model_version = model_version

        self._model        = forecaster._model
        self._sm_model     = forecaster._sm_model
        self._scaler       = forecaster._scaler
        self._feature_names: list[str] = forecaster._feature_names
        self._metrics: ValidationMetrics | None = forecaster._metrics
        self._daily_df: pd.DataFrame | None     = forecaster._daily_df

        logger.info(
            "SalesInferenceEngine inicializado | item=%s | version=%s | "
            "features=%d | rmse=%.2f",
            item_id,
            model_version,
            len(self._feature_names),
            self._metrics.rmse if self._metrics else 0.0,
        )

    def predict_future_sales(
        self,
        horizon_days: int = 30,
    ) -> list[DailyForecast]:

        if not (1 <= horizon_days <= 365):
            raise ValueError("horizon_days debe estar entre 1 y 365.")
        if self._daily_df is None:
            raise RuntimeError(
                "No hay datos históricos. El forecaster debe haber ejecutado "
                "build_features() antes de instanciar SalesInferenceEngine."
            )

        rmse_base: float = self._metrics.rmse if self._metrics else 0.0
        last_date = self._daily_df.index[-1]

        max_lookback = max(
            max(self.forecaster.lags),
            max(self.forecaster.rolling_windows),
        )
        history: list[float] = list(
            self._daily_df[self.forecaster.target_col].values[-max_lookback:]
        )

        results: list[DailyForecast] = []

        for step in range(1, horizon_days + 1):
            future_date = last_date + pd.Timedelta(days=step)
            row         = self._build_feature_row(future_date, history, step)

            x_vec = np.array(
                [row.get(fname, 0.0) for fname in self._feature_names],
                dtype=np.float64,
            ).reshape(1, -1)

            if self._scaler is not None:
                x_vec = self._scaler.transform(x_vec)

            pred_raw: float = float(self._model.predict(x_vec)[0])
            pred:     float = max(round(pred_raw, 2), 0.0)
            uncertainty_factor: float = float(np.sqrt(step / 30))
            margin: float = round(Z_SCORE_95 * rmse_base * uncertainty_factor, 2)
            lower:  float = max(round(pred - margin, 2), 0.0)
            upper:  float = round(pred + margin, 2)

            results.append(
                DailyForecast(
                    date              = future_date.strftime("%Y-%m-%d"),
                    predicted_revenue = pred,
                    lower_bound       = lower,
                    upper_bound       = upper,
                    day_of_week       = future_date.strftime("%A"),
                    is_weekend        = bool(future_date.dayofweek >= 5),
                    week_number       = int(future_date.isocalendar()[1]),
                )
            )
            history.append(pred)

        logger.info(
            "Forecast generado: %d días | %s → %s",
            horizon_days,
            results[0].date,
            results[-1].date,
        )
        return results

    def _analyze_trends(
        self,
        forecasts: list[DailyForecast],
        historical_window_days: int = 30,
    ) -> TrendAnalysis:

        preds   = np.array([f.predicted_revenue for f in forecasts])
        dates   = [f.date for f in forecasts]
        dow_map = [f.day_of_week for f in forecasts]

        n    = len(preds)
        mean = float(np.mean(preds))
        std  = float(np.std(preds))

        first_val = float(preds[0])  if preds[0]  > 0 else 0.001
        last_val  = float(preds[-1]) if preds[-1] > 0 else 0.0

        pct_change = round(((last_val - first_val) / first_val) * 100, 2)

        if pct_change > 1.0:
            direction = "upward"
        elif pct_change < -1.0:
            direction = "downward"
        else:
            direction = "flat"

        daily_growth = (
            round(float(np.power(last_val / first_val, 1 / max(n - 1, 1))) - 1, 6)
            if first_val > 0 else 0.0
        )

        volatility = round(std / mean, 4) if mean > 0 else 0.0

        sorted_idx_desc = np.argsort(preds)[::-1]
        sorted_idx_asc  = np.argsort(preds)

        peak_days: list[dict[str, Any]] = [
            {
                "date":              dates[i],
                "day_of_week":       dow_map[i],
                "predicted_revenue": round(float(preds[i]), 2),
                "rank":              rank + 1,
            }
            for rank, i in enumerate(sorted_idx_desc[:3])
        ]

        valley_days: list[dict[str, Any]] = [
            {
                "date":              dates[i],
                "day_of_week":       dow_map[i],
                "predicted_revenue": round(float(preds[i]), 2),
                "rank":              rank + 1,
            }
            for rank, i in enumerate(sorted_idx_asc[:3])
        ]

        dow_series = pd.Series(preds, index=pd.to_datetime(dates))
        weekly_raw = (
            dow_series
            .groupby(dow_series.index.day_name())
            .mean()
        )
        day_order   = ["Monday", "Tuesday", "Wednesday", "Thursday",
                       "Friday", "Saturday", "Sunday"]
        weekly_pattern: dict[str, float] = {
            day: round(float(weekly_raw.get(day, 0.0)), 2)
            for day in day_order
            if day in weekly_raw.index
        }

        monthly_comparison = self._compute_monthly_comparison(
            forecasts, historical_window_days
        )

        return TrendAnalysis(
            direction               = direction,
            total_pct_change        = pct_change,
            avg_daily_revenue       = round(mean, 2),
            total_projected_revenue = round(float(np.sum(preds)), 2),
            peak_days               = peak_days,
            valley_days             = valley_days,
            weekly_pattern          = weekly_pattern,
            monthly_comparison      = monthly_comparison,
            volatility_index        = volatility,
            growth_rate_daily       = daily_growth,
        )

    def _compute_monthly_comparison(
        self,
        forecasts: list[DailyForecast],
        historical_window_days: int,
    ) -> dict[str, Any]:

        window = min(30, len(forecasts))
        future_preds = np.array([f.predicted_revenue for f in forecasts[:window]])
        future_total = float(np.sum(future_preds))

        prior_total = 0.0
        prior_label = "no_data"

        if self._daily_df is not None and len(self._daily_df) >= historical_window_days:
            prior_series = self._daily_df[self.forecaster.target_col].values
            prior_window = prior_series[-historical_window_days:]
            prior_total  = float(np.sum(prior_window))
            prior_label  = (
                f"{self._daily_df.index[-historical_window_days].strftime('%Y-%m-%d')}"
                f" → {self._daily_df.index[-1].strftime('%Y-%m-%d')}"
            )

        if prior_total > 0:
            pct = round(((future_total - prior_total) / prior_total) * 100, 2)
            interpretation = (
                f"Se espera un {'aumento' if pct > 0 else 'descenso'} "
                f"del {abs(pct):.1f}% respecto al período previo."
            )
        else:
            pct = 0.0
            interpretation = "No hay datos históricos suficientes para comparar."

        return {
            "future_period_total":  round(future_total, 2),
            "prior_period_total":   round(prior_total,  2),
            "prior_period_label":   prior_label,
            "pct_change":           pct,
            "window_days":          window,
            "interpretation":       interpretation,
        }

    def build_report(
        self,
        forecasts:               list[DailyForecast],
        historical_window_days:  int = 30,
    ) -> dict[str, Any]:

        trend      = self._analyze_trends(forecasts, historical_window_days)
        eq         = self._extract_model_equation()
        metrics    = self._metrics.to_dict() if self._metrics else {}
        training_period = self._get_training_period()

        report: dict[str, Any] = {
            "metadata": {
                "report_id":       str(uuid.uuid4()),
                "model_version":   self.model_version,
                "model_type":      MODEL_NAME,
                "analysis_type":   ANALYSIS_TYPE,
                "item_id":         self.item_id,
                "target_variable": self.forecaster.target_col,
                "generated_at":    datetime.now(timezone.utc).isoformat(),
                "training_period": training_period,
                "horizon_days":    len(forecasts),
                "feature_count":   len(self._feature_names),
                "lags_used":       self.forecaster.lags,
                "rolling_windows": self.forecaster.rolling_windows,
            },
            "model_equation":     eq.to_dict(),
            "validation_metrics": metrics,
            "trend_analysis":     trend.to_dict(),
            "confidence_config": {
                "level":        CONFIDENCE_LEVEL,
                "z_score":      Z_SCORE_95,
                "base_rmse":    round(self._metrics.rmse, 4) if self._metrics else None,
                "note":         (
                    "El margen del IC escala con √(step/30) para reflejar "
                    "la incertidumbre acumulada en horizontes largos."
                ),
            },
            "forecast": [f.to_dict() for f in forecasts],
        }

        logger.info(
            "Reporte ensamblado | report_id=%s | días=%d | tendencia=%s (%.1f%%)",
            report["metadata"]["report_id"],
            len(forecasts),
            trend.direction,
            trend.total_pct_change,
        )
        return report

    def save_to_db(
        self,
        report:  dict[str, Any],
        session: Any,
        *,
        commit:  bool = True,
    ) -> str:

        try:
            from sqlalchemy.dialects.postgresql import insert as pg_insert

            try:
                from models import AnalysisResult  # type: ignore[import]
            except ImportError:
                return self._save_to_db_raw_sql(report, session, commit)

            record_id = str(uuid.uuid4())
            record = AnalysisResult(
                id            = record_id,
                analysis_type = ANALYSIS_TYPE,
                model_name    = MODEL_NAME,
                item_id       = self.item_id,
                model_version = self.model_version,
                result        = report,
                generated_at  = datetime.now(timezone.utc),
            )
            session.add(record)
            if commit:
                session.commit()
                logger.info(" Reporte guardado en DB | id=%s", record_id)
            return record_id

        except Exception as exc:
            session.rollback()
            logger.error(" Error al guardar en DB: %s", exc)
            raise RuntimeError(f"Fallo en persistencia: {exc}") from exc

    def _save_to_db_raw_sql(
        self,
        report:  dict[str, Any],
        session: Any,
        commit:  bool,
    ) -> str:

        from sqlalchemy import text

        record_id   = str(uuid.uuid4())
        result_json = json.dumps(report, ensure_ascii=False, default=str)

        sql = text("""
            INSERT INTO analysis_results
                (id, analysis_type, model_name, item_id, model_version,
                 result, generated_at)
            VALUES
                (:id, :analysis_type, :model_name, :item_id,
                 :model_version, CAST(:result AS jsonb), :generated_at)
        """)
        session.execute(sql, {
            "id":            record_id,
            "analysis_type": ANALYSIS_TYPE,
            "model_name":    MODEL_NAME,
            "item_id":       self.item_id or "",
            "model_version": self.model_version,
            "result":        result_json,
            "generated_at":  datetime.now(timezone.utc).isoformat(),
        })
        if commit:
            session.commit()
            logger.info("Reporte guardado (raw SQL) | id=%s", record_id)
        return record_id

    @staticmethod
    def _validate_forecaster(forecaster: LinearSalesForecaster) -> None:
        if forecaster._model is None or forecaster._sm_model is None:
            raise RuntimeError(
                "El LinearSalesForecaster no tiene un modelo entrenado. "
                "Ejecuta train_and_validate() antes de instanciar "
                "SalesInferenceEngine."
            )

    def _build_feature_row(
        self,
        future_date: pd.Timestamp,
        history:     list[float],
        step:        int,
    ) -> dict[str, float]:

        row: dict[str, float] = {}

        base_trend = len(self._daily_df) if self._daily_df is not None else 0
        row["trend"]       = float(base_trend + step)
        row["day_of_year"] = float(future_date.dayofyear)
        row["is_weekend"]  = float(future_date.dayofweek >= 5)

        for m in range(2, 13):
            row[f"month_{m}"] = float(future_date.month == m)

        for d in range(1, 7):
            row[f"dow_{d}"] = float(future_date.dayofweek == d)

        for q in range(2, 5):
            row[f"q_{q}"] = float(future_date.quarter == q)

        for lag in self.forecaster.lags:
            row[f"lag_{lag}d"] = float(history[-lag]) if len(history) >= lag else 0.0

        for window in self.forecaster.rolling_windows:
            recent = history[-window:] if len(history) >= window else history
            row[f"rolling_mean_{window}d"] = float(np.mean(recent))
            row[f"rolling_std_{window}d"]  = (
                float(np.std(recent)) if len(recent) > 1 else 0.0
            )

        return row

    def _extract_model_equation(self) -> ModelEquation:

        sm         = self._sm_model
        sm_table   = sm.summary2().tables[1]

        col_coef   = "Coef."
        col_se     = "Std.Err."
        col_pv     = "P>|t|"
        col_ci_lo  = "[0.025"
        col_ci_hi  = "0.975]"

        intercept  = round(float(sm_table.iloc[0][col_coef]), 6)

        coefficients: list[dict[str, Any]] = []
        for i, fname in enumerate(self._feature_names):
            row = sm_table.iloc[i + 1]
            beta     = round(float(row[col_coef]), 6)
            p_val    = round(float(row[col_pv]),   6)
            ci_lo    = round(float(row[col_ci_lo]),6)
            ci_hi    = round(float(row[col_ci_hi]),6)
            coefficients.append({
                "feature":     fname,
                "beta":        beta,
                "abs_beta":    round(abs(beta), 6),
                "p_value":     p_val,
                "significant": bool(p_val < 0.05),
                "ci_lower":    ci_lo,
                "ci_upper":    ci_hi,
            })

        coefficients.sort(key=lambda x: x["abs_beta"], reverse=True)

        top5   = coefficients[:5]
        terms  = [f"{c['beta']:+.4f}·{c['feature']}" for c in top5]
        eq_str = (
            f"ŷ = {intercept:.4f} + "
            + " ".join(terms)
            + (" + … " if len(coefficients) > 5 else "")
        )

        return ModelEquation(
            intercept     = intercept,
            coefficients  = coefficients,
            equation_str  = eq_str,
            r_squared     = round(float(sm.rsquared),     4),
            adj_r_squared = round(float(sm.rsquared_adj), 4),
            aic           = round(float(sm.aic),          2),
        )

    def _get_training_period(self) -> dict[str, str]:
        if self._daily_df is None:
            return {}
        return {
            "start":      self._daily_df.index.min().strftime("%Y-%m-%d"),
            "end":        self._daily_df.index.max().strftime("%Y-%m-%d"),
            "total_days": str(len(self._daily_df)),
        }

    def to_json(
        self,
        report:       dict[str, Any],
        indent:       int  = 2,
        ensure_ascii: bool = False,
    ) -> str:

        def _default_serializer(obj: Any) -> Any:
            if isinstance(obj, (pd.Timestamp, datetime)):
                return obj.isoformat()
            if isinstance(obj, (np.integer,)):
                return int(obj)
            if isinstance(obj, (np.floating,)):
                return float(obj)
            if isinstance(obj, np.ndarray):
                return obj.tolist()
            if isinstance(obj, pd.Series):
                return obj.tolist()
            raise TypeError(f"Tipo no serializable: {type(obj)}")

        return json.dumps(
            report,
            indent        = indent,
            ensure_ascii  = ensure_ascii,
            default       = _default_serializer,
        )

    def print_summary(self, forecasts: list[DailyForecast]) -> None:
        trend = self._analyze_trends(forecasts)
        sep   = "=" * 64

        print(f"\n{sep}")
        print(f"  SalesInferenceEngine — Resumen Ejecutivo")
        print(sep)
        print(f"  Ítem           : {self.item_id or 'N/A'}")
        print(f"  Horizonte      : {len(forecasts)} días  "
              f"({forecasts[0].date} → {forecasts[-1].date})")
        print(f"  Dirección      : {trend.direction.upper()}")
        print(f"  Cambio total   : {trend.total_pct_change:+.1f}%")
        print(f"  Ingreso prom.  : ${trend.avg_daily_revenue:,.2f}/día")
        print(f"  Ingreso total  : ${trend.total_projected_revenue:,.2f}")
        print(f"  Volatilidad CV : {trend.volatility_index:.4f}")
        print(f"\n   Pico #1     : {trend.peak_days[0]['date']} — "
              f"${trend.peak_days[0]['predicted_revenue']:,.2f}")
        print(f"   Valle #1    : {trend.valley_days[0]['date']} — "
              f"${trend.valley_days[0]['predicted_revenue']:,.2f}")
        print("  Patrón semanal (ingreso medio):")
        max_val = max(trend.weekly_pattern.values()) if trend.weekly_pattern else 0
        for day, val in trend.weekly_pattern.items():
            if max_val > 0:
                bar = "█" * int((val / max_val) * 20)
            else:
                bar = ""
            print(f"   {day[:3]}: ${val:,.2f} {bar}")