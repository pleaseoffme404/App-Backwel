from __future__ import annotations

import json
import logging
import warnings
from datetime import datetime, timezone
from typing import Any

import numpy as np
import pandas as pd

warnings.filterwarnings("ignore")

from IA.RegresionLineal.forecaster import LinearSalesForecaster
from IA.RegresionLineal.interference_engine import SalesInferenceEngine

from Backend.data.db_connector import Connect_BD

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)

class ErpRepository:

    _SQL_TRAINING = """
                    SELECT pd.event_date                AS date,
                           pd.quantity                  AS quantity,
                           CASE
                               WHEN pd.quantity > 0 THEN (pd.amount / pd.quantity)
                               ELSE COALESCE(iph.base_price, 0)
                               END                      AS unit_price,
                           pd.amount                    AS line_amount,
                           pd.product_id                AS item_id,
                           pd.category,
                           p.brand,
                           COALESCE(d.decimal_value, 0) AS active_discount,
                           CASE
                               WHEN d.id IS NOT NULL THEN TRUE
                               ELSE FALSE
                               END                      AS has_promotion
                    FROM processed_data pd
                             LEFT JOIN product p
                                       ON pd.product_id = p.id
                             LEFT JOIN item_price_history iph
                                       ON pd.product_id = iph.item_id
                                           AND iph.last_update = (SELECT MAX(sub.last_update)
                                                                  FROM item_price_history sub
                                                                  WHERE sub.item_id = pd.product_id
                                                                    AND sub.last_update <= pd.event_date)
                             LEFT JOIN discount_target dt
                                       ON pd.product_id = dt.product_id
                             LEFT JOIN discount d
                                       ON dt.discount_id = d.id
                                           AND pd.event_date BETWEEN d.start_date AND d.end_date
                    WHERE 1 = 1
                        {product_filter}
                    ORDER BY pd.event_date ASC;
                    """

    _SQL_DATE_RANGE = """
        SELECT
            product_id,
            MIN(event_date)                                 AS first_date,
            MAX(event_date)                                 AS last_date,
            COUNT(*)                                        AS total_records,
            SUM(quantity)                                   AS total_units,
            SUM(amount)                                     AS total_revenue
        FROM processed_data
        WHERE status = 'completed'
          {product_filter}
        GROUP BY product_id
        ORDER BY product_id;
    """

    _SQL_ACTIVE_DISCOUNTS = """
        SELECT
            dt.product_id,
            d.decimal_value                                 AS discount_rate,
            d.start_date,
            d.end_date,
            (d.end_date - CURRENT_DATE)                    AS days_remaining
        FROM discount d
        INNER JOIN discount_target dt
            ON d.id = dt.discount_id
        WHERE
            CURRENT_DATE BETWEEN d.start_date AND d.end_date
            {product_filter}
        ORDER BY d.end_date ASC;
    """

    _SQL_PRICE_HISTORY = """
        SELECT
            item_id,
            base_price,
            last_update,
            LAG(base_price) OVER (
                PARTITION BY item_id ORDER BY last_update
            )                                               AS prev_price,
            ROUND(
                (base_price - LAG(base_price) OVER (
                    PARTITION BY item_id ORDER BY last_update
                )) / NULLIF(LAG(base_price) OVER (
                    PARTITION BY item_id ORDER BY last_update
                ), 0) * 100, 2
            )                                               AS pct_price_change
        FROM item_price_history
        WHERE item_id = %(item_id)s
        ORDER BY last_update DESC
        LIMIT 12;
    """

    _SQL_UPSERT_RESULT = """
        INSERT INTO analysis_results
            (analysis_type, model_name, model_version, result, input_data_id)
        VALUES (
            %(analysis_type)s,
            %(model_name)s,
            %(model_version)s,
            %(result)s::jsonb,
            (SELECT record_id FROM processed_data LIMIT 1)
        )
        ON CONFLICT (analysis_type, model_name, model_version)
        DO UPDATE SET
            result     = EXCLUDED.result,
            created_at = now();
    """

    _SQL_FETCH_LAST_RESULT = """
        SELECT analysis_type,
               result,
               created_at
        FROM analysis_results
        WHERE analysis_type = %(analysis_type)s
        ORDER BY created_at DESC
        LIMIT 1;
    """


    def fetch_training_data(self, product_id: str | None = None) -> pd.DataFrame:

        params = {}
        if product_id:
            product_filter = "AND pd.product_id = %(product_id)s"
            params["product_id"] = product_id
        else:
            product_filter = ""

        query = self._SQL_TRAINING.format(product_filter=product_filter)
        df = self._execute_query(query, params=params)

        if df.empty:
            raise ValueError(
                f"No se encontraron registros completados para product_id={product_id!r}. "
                "Verifica el SKU o el rango de fechas en processed_data."
            )

        df["date"] = pd.to_datetime(df["date"])
        logger.info(
            "Datos de entrenamiento extraídos | product_id=%s | registros=%d | rango=%s → %s",
            product_id or "ALL",
            len(df),
            df["date"].min().strftime("%Y-%m-%d"),
            df["date"].max().strftime("%Y-%m-%d"),
        )
        return df

    def fetch_date_range(self, product_id: str | None = None) -> pd.DataFrame:
        params = {}
        if product_id:
            product_filter = "AND product_id = %(product_id)s"
            params["product_id"] = product_id
        else:
            product_filter = ""

        query = self._SQL_DATE_RANGE.format(product_filter=product_filter)
        return self._execute_query(query, params=params)

    def fetch_active_discounts(self, product_id: str | None = None) -> pd.DataFrame:
        """Retorna los descuentos vigentes a la fecha de hoy."""
        params = {}
        if product_id:
            product_filter = "AND dt.product_id = %(product_id)s"
            params["product_id"] = product_id
        else:
            product_filter = ""

        query = self._SQL_ACTIVE_DISCOUNTS.format(product_filter=product_filter)
        return self._execute_query(query, params=params)

    def fetch_price_history(self, item_id: str) -> pd.DataFrame:
        return self._execute_query(
            self._SQL_PRICE_HISTORY,
            params={"item_id": item_id},
        )

    def save_result(
        self,
        analysis_type: str,
        report: dict[str, Any],
        model_name: str = "sales_forecast_pipeline",
        model_version: str = "3B-v1.0",
    ) -> None:

        import math

        def clean_math_anomalies(obj):
            if isinstance(obj, float) and (math.isnan(obj) or math.isinf(obj)):
                return None
            elif isinstance(obj, dict):
                return {k: clean_math_anomalies(v) for k, v in obj.items()}
            elif isinstance(obj, list):
                return [clean_math_anomalies(v) for v in obj]
            return obj

        report_limpio = clean_math_anomalies(report)

        conn = Connect_BD.crear_conexion()
        if not conn:
            raise ConnectionError("No se pudo conectar a la base de datos.")
        try:
            cursor    = conn.cursor()
            json_data = json.dumps(report_limpio, default=str, ensure_ascii=False)
            cursor.execute(
                self._SQL_UPSERT_RESULT,
                {
                    "analysis_type": analysis_type,
                    "model_name":    model_name,
                    "model_version": model_version,
                    "result":        json_data,
                },
            )
            conn.commit()
            cursor.close()
            logger.info(
                "✅ Reporte persistido en analysis_results | "
                "analysis_type=%s | model_name=%s | model_version=%s",
                analysis_type, model_name, model_version,
            )
        except Exception as exc:
            conn.rollback()
            logger.error("❌ Error al guardar en analysis_results: %s", exc)
            raise RuntimeError(f"Persistencia fallida: {exc}") from exc
        finally:
            Connect_BD.cerrar_conexion(conn)

    def fetch_last_result(self, analysis_type: str) -> dict[str, Any] | None:
        conn = Connect_BD.crear_conexion()
        if not conn:
            raise ConnectionError("No se pudo conectar a la base de datos.")
        try:
            cursor = conn.cursor()
            cursor.execute(
                self._SQL_FETCH_LAST_RESULT,
                {"analysis_type": analysis_type},
            )
            row = cursor.fetchone()
            cursor.close()
            if row is None:
                return None
            result = row[1] if isinstance(row[1], dict) else json.loads(row[1])
            return result
        finally:
            Connect_BD.cerrar_conexion(conn)


    @staticmethod
    def _execute_query(query: str, params: dict[str, Any] | None = None) -> pd.DataFrame:
        """Ejecuta una query y devuelve un DataFrame."""
        conn = Connect_BD.crear_conexion()
        if not conn:
            raise ConnectionError("No se pudo conectar a la base de datos.")
        try:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                df = pd.read_sql(query, conn, params=params)
        finally:
            Connect_BD.cerrar_conexion(conn)
        return df

class SalesForecastPipeline:

    def __init__(
        self,
        target_col:      str       = "revenue",
        test_size:       float     = 0.20,
        lags:            list[int] | None = None,
        rolling_windows: list[int] | None = None,
        scale_features:  bool      = True,
        model_version:   str       = "3B-v1.0",
    ) -> None:
        self.target_col      = target_col
        self.test_size       = test_size
        self.lags            = lags or [1, 7, 14, 28]
        self.rolling_windows = rolling_windows or [7, 14, 30]
        self.scale_features  = scale_features
        self.model_version   = model_version

        self.repo         = ErpRepository()
        self.forecaster:  LinearSalesForecaster | None = None
        self.engine:      SalesInferenceEngine  | None = None
        self.last_report: dict[str, Any]        | None = None

    def run(
        self,
        product_id:             str | None = None,
        horizon_days:           int        = 30,
        historical_window_days: int        = 30,
        persist:                bool       = True,
        print_summary:          bool       = True,
    ) -> dict[str, Any]:

        analysis_key = self._build_analysis_key(product_id)
        sep          = "=" * 68

        print(f"\n{sep}")
        print(f"  Pipeline Fase 3B | SKU: {product_id or 'ALL'} | "
              f"horizonte: {horizon_days}d | versión: {self.model_version}")
        print(sep + "\n")

        print("🔍  [1/6] Extrayendo datos del ERP...")
        raw_df     = self.repo.fetch_training_data(product_id=product_id)
        discounts  = self._safe_fetch(lambda: self.repo.fetch_active_discounts(product_id))
        price_hist = (
            self._safe_fetch(lambda: self.repo.fetch_price_history(product_id))
            if product_id else None
        )
        self._print_data_summary(raw_df, discounts)

        print("\n⚙️   [2/6] Construyendo features temporales...")
        self.forecaster = LinearSalesForecaster(
            target_col      = self.target_col,
            test_size       = self.test_size,
            lags            = self.lags,
            rolling_windows = self.rolling_windows,
            scale_features  = self.scale_features,
        )
        feature_df = self.forecaster.build_features(
            raw_df    = raw_df,
            date_col  = "date",
            qty_col   = "quantity",
            price_col = "unit_price",
        )

        print("\n🧠  [3/6] Entrenando modelo de regresión lineal...")
        metrics = self.forecaster.train_and_validate(feature_df)
        self.forecaster.summary()

        print(f"\n📈  [4/6] Generando predicciones ({horizon_days} días)...")
        self.engine = SalesInferenceEngine(
            forecaster    = self.forecaster,
            item_id       = product_id,
            model_version = self.model_version,
        )
        forecasts = self.engine.predict_future_sales(horizon_days=horizon_days)

        print("\n📊  [5/6] Ensamblando reporte JSON...")
        report = self.engine.build_report(
            forecasts              = forecasts,
            historical_window_days = historical_window_days,
        )
        report       = self._enrich_report(report, discounts, price_hist, product_id)
        self.last_report = report

        if print_summary:
            self.engine.print_summary(forecasts)

        if persist:
            print(f"\n💾  [6/6] Guardando en analysis_results "
                  f"(key='{analysis_key}')...")

            self.repo.save_result(
                analysis_type = analysis_key,
                report        = report,
                model_name    = "sales_forecast_pipeline",
                model_version = self.model_version,
            )
        else:
            print("\n⏭️   [6/6] Persistencia omitida (persist=False).")

        print(f"\n{sep}")
        print(f"  ✅ Pipeline completado exitosamente")
        print(f"  Métricas → RMSE={metrics.rmse:.2f} | R²={metrics.r2:.4f}")
        print(f"  Reporte  → {len(forecasts)} días | "
              f"tendencia={report['trend_analysis']['direction'].upper()}")
        print(sep + "\n")

        return report

    def _build_analysis_key(self, product_id: str | None) -> str:
        """Genera la clave única del análisis para analysis_results."""
        suffix = product_id.replace(" ", "_") if product_id else "ALL"
        return f"linear_sales_forecast_{suffix}"

    def _enrich_report(
        self,
        report:     dict[str, Any],
        discounts:  pd.DataFrame | None,
        price_hist: pd.DataFrame | None,
        product_id: str | None,
    ) -> dict[str, Any]:
        report["pipeline_context"] = {
            "pipeline_version":  "1.0.0",
            "pipeline_ran_at":   datetime.now(timezone.utc).isoformat(),
            "product_id":        product_id,
            "active_discounts":  (
                discounts.to_dict(orient="records")
                if discounts is not None and not discounts.empty
                else []
            ),
            "recent_price_changes": (
                price_hist.to_dict(orient="records")
                if price_hist is not None and not price_hist.empty
                else []
            ),
        }
        return report

    @staticmethod
    def _safe_fetch(fn) -> pd.DataFrame | None:
        try:
            return fn()
        except Exception as exc:
            logger.warning("Fetch opcional fallido (no crítico): %s", exc)
            return None

    @staticmethod
    def _print_data_summary(
        raw_df:    pd.DataFrame,
        discounts: pd.DataFrame | None,
    ) -> None:
        print(f"   Registros     : {len(raw_df):,}")
        if "date" in raw_df.columns:
            print(f"   Rango         : {raw_df['date'].min().strftime('%Y-%m-%d')} "
                  f"→ {raw_df['date'].max().strftime('%Y-%m-%d')}")
        if "category" in raw_df.columns:
            print(f"   Categorías    : {raw_df['category'].nunique()}")
        if discounts is not None and not discounts.empty:
            print(f"   Descuentos activos: {len(discounts)} "
                  f"(tasas: {discounts['discount_rate'].tolist()})")
        else:
            print("   Descuentos activos: ninguno")

    def to_json(self, indent: int = 2) -> str:

        if self.last_report is None:
            raise RuntimeError("Ejecuta run() primero.")
        return self.engine.to_json(self.last_report, indent=indent)

    def load_last_result(self, product_id: str | None = None) -> dict[str, Any] | None:

        key = self._build_analysis_key(product_id)
        return self.repo.fetch_last_result(analysis_type=key)


