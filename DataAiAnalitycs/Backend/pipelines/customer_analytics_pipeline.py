from __future__ import annotations

import json
import logging
import math
import warnings
from datetime import datetime, timezone
from typing import Any

import numpy as np
import pandas as pd

warnings.filterwarnings("ignore")

from Backend.data.db_connector import Connect_BD

logger = logging.getLogger(__name__)


class ErpRepository:

    _SQL_CLIENT_PURCHASE_HISTORY = """
                                   SELECT pd.user_id, \
                                          ui.name || ' ' || ui.surname  AS client_name, \
                                          ui.created_at                 AS registration_date, \
                                          MAX(pd.event_date)            AS last_purchase_date, \
                                          MIN(pd.event_date)            AS first_purchase_date, \
                                          COUNT(DISTINCT pd.event_date) AS frequency, \
                                          SUM(pd.amount)                AS monetary, \
                                          SUM(pd.quantity)              AS total_units, \
                                          COUNT(DISTINCT pd.product_id) AS distinct_products, \
                                          AVG(pd.amount)                AS avg_order_value
                                   FROM processed_data pd
                                            INNER JOIN user_info ui
                                                       ON pd.user_id = ui.uuid
                                   WHERE pd.status = 'completed'
                                     AND pd.user_id IS NOT NULL {date_filter}
                                     {user_filter}
                                   GROUP BY
                                       pd.user_id,
                                       ui.name,
                                       ui.surname,
                                       ui.created_at
                                   ORDER BY
                                       monetary DESC; \
                                   """

    _SQL_CLIENT_TRANSACTION_DETAIL = """
                                     SELECT pd.user_id, \
                                            pd.event_date, \
                                            pd.product_id, \
                                            pd.category, \
                                            pd.amount, \
                                            pd.quantity, \
                                            COALESCE(d.decimal_value, 0) AS discount_applied
                                     FROM processed_data pd
                                              LEFT JOIN discount_target dt
                                                        ON pd.product_id = dt.product_id
                                              LEFT JOIN discount d
                                                        ON dt.discount_id = d.id
                                                            AND pd.event_date BETWEEN d.start_date AND d.end_date
                                     WHERE pd.status = 'completed'
                                       AND pd.user_id = %(user_id)s
                                     ORDER BY pd.event_date DESC; \
                                     """

    _SQL_CLIENT_CATEGORY_MIX = """
                               SELECT pd.user_id, \
                                      pd.category, \
                                      COUNT(*)       AS purchase_count, \
                                      SUM(pd.amount) AS category_revenue, \
                                      ROUND( \
                                              SUM(pd.amount) * 100.0 / \
                                              NULLIF(SUM(SUM(pd.amount)) OVER (PARTITION BY pd.user_id), 0) \
                                          , 2)       AS revenue_pct
                               FROM processed_data pd
                               WHERE pd.status = 'completed'
                                 AND pd.user_id IS NOT NULL {user_filter}
                               GROUP BY
                                   pd.user_id,
                                   pd.category
                               ORDER BY
                                   pd.user_id,
                                   category_revenue DESC; \
                               """

    _SQL_UPSERT_RESULT = """
        INSERT INTO analysis_results (analysis_type, model_name, model_version, result, input_data_id)
        VALUES (
            %(analysis_type)s,
            %(model_name)s,
            %(model_version)s,
            %(result)s::jsonb,
            (SELECT record_id FROM processed_data LIMIT 1)
        )
        ON CONFLICT (analysis_type, model_name, model_version)
            DO UPDATE SET result = EXCLUDED.result;
    """

    _SQL_FETCH_LAST_RESULT = """
        SELECT analysis_type, result, created_at
        FROM analysis_results
        WHERE analysis_type = %(analysis_type)s
        ORDER BY created_at DESC
        LIMIT 1;
    """


    def fetch_client_purchase_history(
        self,
        user_id:    str | None = None,
        since_date: str | None = None,
    ) -> pd.DataFrame:
        params: dict[str, Any] = {}

        user_filter = ""
        if user_id is not None:
            user_filter = "AND pd.user_id = %(user_id)s"
            params["user_id"] = user_id

        date_filter = ""
        if since_date:
            date_filter = "AND pd.event_date >= %(since_date)s"
            params["since_date"] = since_date

        query = self._SQL_CLIENT_PURCHASE_HISTORY.format(
            user_filter=user_filter,
            date_filter=date_filter,
        )
        df = self._execute_query(query, params=params)

        if df.empty:
            raise ValueError(
                "No se encontraron registros de clientes completados. "
                "Verifica que processed_data tenga la columna user_id "
                "y que la tabla user_info esté poblada."
            )

        for col in ("last_purchase_date", "first_purchase_date", "registration_date"):
            if col in df.columns:
                df[col] = pd.to_datetime(df[col])

        return df

    def fetch_client_category_mix(
        self,
        user_id: str | None = None,
    ) -> pd.DataFrame:
        params: dict[str, Any] = {}
        user_filter = ""
        if user_id is not None:
            user_filter = "AND pd.user_id = %(user_id)s"
            params["user_id"] = user_id

        query = self._SQL_CLIENT_CATEGORY_MIX.format(user_filter=user_filter)
        return self._execute_query(query, params=params)

    def fetch_client_transaction_detail(self, user_id: str) -> pd.DataFrame:
        df = self._execute_query(
            self._SQL_CLIENT_TRANSACTION_DETAIL,
            params={"user_id": user_id},
        )
        if "event_date" in df.columns:
            df["event_date"] = pd.to_datetime(df["event_date"])
        return df

    def save_result(self, analysis_type: str, report: dict[str, Any]) -> None:

        def _clean(obj: Any) -> Any:
            if isinstance(obj, float) and (math.isnan(obj) or math.isinf(obj)):
                return None
            if isinstance(obj, dict):
                return {k: _clean(v) for k, v in obj.items()}
            if isinstance(obj, list):
                return [_clean(v) for v in obj]
            return obj

        report_clean = _clean(report)

        conn = Connect_BD.crear_conexion()
        if not conn:
            raise ConnectionError("No se pudo conectar a la base de datos.")
        try:
            cursor = conn.cursor()
            cursor.execute(
                self._SQL_UPSERT_RESULT,
                {
                    "analysis_type": analysis_type,
                    "model_name":    "customer_analytics_pipeline",
                    "model_version": "3B-v1.0",
                    "result": json.dumps(report_clean, default=str, ensure_ascii=False),
                },
            )
            conn.commit()
            cursor.close()
            logger.info(
                "✅ Reporte persistido en analysis_results | analysis_type=%s",
                analysis_type,
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
            cursor.execute(self._SQL_FETCH_LAST_RESULT, {"analysis_type": analysis_type})
            row = cursor.fetchone()
            cursor.close()
            if row is None:
                return None
            return row[1] if isinstance(row[1], dict) else json.loads(row[1])
        finally:
            Connect_BD.cerrar_conexion(conn)


    @staticmethod
    def _execute_query(
        query: str,
        params: dict[str, Any] | None = None,
    ) -> pd.DataFrame:
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

class RfmAnalyzer:

    SEGMENT_LABELS: dict[str, str] = {
        "best_customers": "Mejores clientes — alta recencia, frecuencia y valor",
        "loyal_customers": "Clientes leales — compran con frecuencia y alto gasto",
        "promising": "Clientes prometedores — compra reciente, baja frecuencia aún",
        "at_risk": "Clientes en riesgo — compraron bien pero llevan tiempo inactivos",
        "inactive": "Clientes inactivos — sin compras recientes ni frecuencia",
        "occasional": "Clientes ocasionales — comportamiento intermedio",
    }

    def __init__(
        self,
        n_quantiles:    int               = 5,
        reference_date: datetime | None   = None,
    ) -> None:
        self.n_quantiles    = n_quantiles
        self.reference_date = reference_date or datetime.now(timezone.utc).replace(tzinfo=None)

    def compute(self, history_df: pd.DataFrame) -> pd.DataFrame:

        self._validate_columns(history_df)

        df = history_df.copy()

        df["recency_days"] = (
            self.reference_date - df["last_purchase_date"].dt.tz_localize(None)
        ).dt.days.clip(lower=0)

        df["r_score"] = self._score_quantile(df["recency_days"],   ascending=True)
        df["f_score"] = self._score_quantile(df["frequency"],      ascending=False)
        df["m_score"] = self._score_quantile(df["monetary"],       ascending=False)

        df["rfm_score"] = (
            df["r_score"] + df["f_score"] + df["m_score"]
        ) / 3.0
        df["rfm_score"] = df["rfm_score"].round(2)

        df["segment"]       = df.apply(self._assign_segment, axis=1)
        df["segment_label"] = df["segment"].map(self.SEGMENT_LABELS)

        logger.info(
            "RFM calculado | clientes=%d | distribución de segmentos:\n%s",
            len(df),
            df["segment"].value_counts().to_string(),
        )
        return df

    def build_report(
        self,
        rfm_df:         pd.DataFrame,
        category_mix:   pd.DataFrame | None = None,
        pipeline_meta:  dict[str, Any] | None = None,
    ) -> dict[str, Any]:

        report: dict[str, Any] = {
            "generated_at":  datetime.now(timezone.utc).isoformat(),
            "analysis_type": "customer_rfm_segmentation",
            "reference_date": self.reference_date.strftime("%Y-%m-%d"),
            "n_quantiles":   self.n_quantiles,
        }

        report["summary"] = {
            "total_clients":         int(len(rfm_df)),
            "avg_recency_days":      round(float(rfm_df["recency_days"].mean()), 1),
            "avg_frequency":         round(float(rfm_df["frequency"].mean()), 2),
            "avg_monetary":          round(float(rfm_df["monetary"].mean()), 2),
            "total_revenue":         round(float(rfm_df["monetary"].sum()), 2),
            "avg_rfm_score":         round(float(rfm_df["rfm_score"].mean()), 2),
            "median_rfm_score":      round(float(rfm_df["rfm_score"].median()), 2),
        }

        report["segments"] = {}
        for seg, grp in rfm_df.groupby("segment"):
            report["segments"][seg] = {
                "label":            self.SEGMENT_LABELS.get(seg, seg),
                "client_count":     int(len(grp)),
                "pct_of_total":     round(len(grp) / len(rfm_df) * 100, 1),
                "total_revenue":    round(float(grp["monetary"].sum()), 2),
                "avg_revenue":      round(float(grp["monetary"].mean()), 2),
                "avg_frequency":    round(float(grp["frequency"].mean()), 2),
                "avg_recency_days": round(float(grp["recency_days"].mean()), 1),
                "avg_rfm_score":    round(float(grp["rfm_score"].mean()), 2),
                "clients":          self._clients_to_records(grp),
            }

        top10 = rfm_df.nlargest(10, "rfm_score")
        report["top_customers"] = self._clients_to_records(top10)

        at_risk = rfm_df[rfm_df["segment"] == "at_risk"].sort_values(
            "monetary", ascending=False
        )
        report["at_risk_clients"] = self._clients_to_records(at_risk)

        inactive = rfm_df[rfm_df["segment"] == "inactive"].sort_values(
            "recency_days", ascending=False
        )
        report["inactive_clients"] = self._clients_to_records(inactive)

        if category_mix is not None and not category_mix.empty:
            report["category_insights"] = self._build_category_insights(
                rfm_df, category_mix
            )
        else:
            report["category_insights"] = {}

        report["pipeline_meta"] = pipeline_meta or {}

        return report

    def _score_quantile(
        self,
        series:    pd.Series,
        ascending: bool,
    ) -> pd.Series:

        labels = list(range(1, self.n_quantiles + 1))
        if not ascending:
            labels = labels[::-1]

        try:
            scored = pd.qcut(
                series,
                q=self.n_quantiles,
                labels=labels,
                duplicates="drop",
            ).astype(float)
        except ValueError:
            ranked  = series.rank(method="first", ascending=ascending)
            buckets = pd.cut(
                ranked,
                bins=self.n_quantiles,
                labels=labels[::-1] if not ascending else labels,
            )
            scored  = buckets.astype(float)

        return scored.fillna(1.0)

    def _assign_segment(self, row: pd.Series) -> str:
        r, f, m = row["r_score"], row["f_score"], row["m_score"]
        n = self.n_quantiles

        threshold_high = n - 1
        threshold_mid  = n - 2
        threshold_low  = 2

        if r >= threshold_high and f >= threshold_high and m >= threshold_high:
            return "best_customers"
        if f >= threshold_high and m >= threshold_mid:
            return "loyal_customers"
        if r >= threshold_high and f < threshold_high:
            return "promising"
        if r <= threshold_low and (f >= threshold_mid or m >= threshold_mid):
            return "at_risk"
        if r == 1 and f <= threshold_low and m <= threshold_low:
            return "inactive"
        return "occasional"

    @staticmethod
    def _clients_to_records(df: pd.DataFrame) -> list[dict[str, Any]]:
        cols = [
            "user_id", "client_name", "last_purchase_date",
            "recency_days", "frequency", "monetary",
            "r_score", "f_score", "m_score", "rfm_score", "segment",
        ]
        cols = [c for c in cols if c in df.columns]
        records = []
        for _, row in df[cols].iterrows():
            record: dict[str, Any] = {}
            for col in cols:
                val = row[col]
                if isinstance(val, pd.Timestamp):
                    record[col] = val.strftime("%Y-%m-%d")
                elif isinstance(val, (np.integer,)):
                    record[col] = int(val)
                elif isinstance(val, (np.floating,)):
                    record[col] = None if math.isnan(val) else round(float(val), 2)
                else:
                    record[col] = val
            records.append(record)
        return records

    def _build_category_insights(
        self,
        rfm_df:       pd.DataFrame,
        category_mix: pd.DataFrame,
    ) -> dict[str, list[dict[str, Any]]]:

        merged = category_mix.merge(
            rfm_df[["user_id", "segment"]],
            on="user_id",
            how="left",
        )
        insights: dict[str, list[dict[str, Any]]] = {}
        for seg, grp in merged.groupby("segment"):
            top_cats = (
                grp.groupby("category")["category_revenue"]
                .sum()
                .nlargest(3)
                .reset_index()
            )
            insights[seg] = top_cats.rename(
                columns={"category_revenue": "total_revenue"}
            ).to_dict(orient="records")
        return insights

    @staticmethod
    def _validate_columns(df: pd.DataFrame) -> None:
        required = {"user_id", "last_purchase_date", "frequency", "monetary"}
        missing  = required - set(df.columns)
        if missing:
            raise ValueError(
                f"El DataFrame de historial de clientes carece de columnas "
                f"requeridas para RFM: {missing}"
            )

class CustomerAnalyticsPipeline:

    ANALYSIS_KEY = "customer_rfm_segmentation"

    def __init__(
        self,
        n_quantiles:    int         = 5,
        reference_date: str | None  = None,
        analysis_key:   str         = ANALYSIS_KEY,
        model_version:  str         = "3B-v1.0",
    ) -> None:
        self.n_quantiles    = n_quantiles
        self.analysis_key   = analysis_key
        self.model_version  = model_version

        if reference_date:
            self.reference_date = datetime.strptime(reference_date, "%Y-%m-%d")
        else:
            self.reference_date = datetime.now(timezone.utc).replace(tzinfo=None)

        self.repo           = ErpRepository()
        self.analyzer       = RfmAnalyzer(
            n_quantiles=self.n_quantiles,
            reference_date=self.reference_date,
        )

        self.rfm_df:       pd.DataFrame | None      = None
        self.last_report:  dict[str, Any] | None    = None

    def run(
            self,
            since_date: str | None = None,
            user_id: str | None = None,
            persist: bool = True,
            print_summary: bool = True,
            segment_filter: str | None = None,
    ) -> dict[str, Any]:

        sep = "=" * 68

        print(f"\n{sep}")
        print(
            f"  CustomerAnalyticsPipeline | RFM Segmentation\n"
            f"  Clientes: {'#' + str(user_id) if user_id else 'TODOS'} | "
            f"Desde: {since_date or 'inicio'} | "
            f"Ref: {self.reference_date.strftime('%Y-%m-%d')} | "
            f"v{self.model_version}"
        )
        print(sep + "\n")

        print("🔍  [1/5] Extrayendo historial de compras por cliente...")
        history_df = self.repo.fetch_client_purchase_history(
            user_id=user_id,
            since_date=since_date,
        )
        self._print_data_summary(history_df)

        print("\n🏷️   [2/5] Extrayendo mix de categorías por cliente...")
        category_mix = self._safe_fetch(
            lambda: self.repo.fetch_client_category_mix(user_id=user_id)
        )
        if category_mix is not None and not category_mix.empty:
            print(f"   Categorías únicas: {category_mix['category'].nunique()}")
        else:
            print("   Sin datos de categorías (no crítico).")

        print("\n📐  [3/5] Calculando scores RFM y asignando segmentos...")
        self.rfm_df = self.analyzer.compute(history_df)

        if print_summary:
            self._print_rfm_summary(self.rfm_df)

        print("\n📊  [4/5] Ensamblando reporte JSON...")
        pipeline_meta = {
            "pipeline_version": "1.0.0",
            "model_version":    self.model_version,
            "pipeline_ran_at":  datetime.now(timezone.utc).isoformat(),
            "n_quantiles":      self.n_quantiles,
            "since_date":       since_date,
            "client_id_filter": user_id,
        }
        report = self.analyzer.build_report(
            rfm_df        = self.rfm_df,
            category_mix  = category_mix,
            pipeline_meta = pipeline_meta,
        )
        self.last_report = report

        if persist:
            print(f"\n💾  [5/5] Guardando en analysis_results "
                  f"(key='{self.analysis_key}')...")
            self.repo.save_result(
                analysis_type = self.analysis_key,
                report        = report,
            )
        else:
            print("\n⏭️   [5/5] Persistencia omitida (persist=False).")

        best  = report["segments"].get("best_customers", {}).get("client_count", 0)
        risk  = report["segments"].get("at_risk",        {}).get("client_count", 0)
        inact = report["segments"].get("inactive",       {}).get("client_count", 0)

        print(f"\n{sep}")
        print(f"  ✅ Pipeline completado exitosamente")
        print(f"  Clientes analizados : {report['summary']['total_clients']}")
        print(f"  Revenue total       : ${report['summary']['total_revenue']:,.2f}")
        print(f"  Mejores clientes    : {best}")
        print(f"  En riesgo           : {risk}")
        print(f"  Inactivos           : {inact}")
        print(sep + "\n")

        if segment_filter:
            return self._filter_by_segment(report, segment_filter)
        return report

    def get_segment(self, segment: str) -> pd.DataFrame:

        if self.rfm_df is None:
            raise RuntimeError("Ejecuta run() primero.")
        mask = self.rfm_df["segment"] == segment
        return self.rfm_df[mask].reset_index(drop=True)

    def to_json(self, indent: int = 2) -> str:
        """Serializa el último reporte generado a JSON string."""
        if self.last_report is None:
            raise RuntimeError("Ejecuta run() primero.")
        return json.dumps(self.last_report, default=str, ensure_ascii=False, indent=indent)

    def load_last_result(self) -> dict[str, Any] | None:

        return self.repo.fetch_last_result(analysis_type=self.analysis_key)

    @staticmethod
    def _safe_fetch(fn) -> pd.DataFrame | None:
        try:
            return fn()
        except Exception as exc:
            logger.warning("Fetch opcional fallido (no crítico): %s", exc)
            return None

    @staticmethod
    def _print_data_summary(df: pd.DataFrame) -> None:
        print(f"   Clientes únicos     : {df['user_id'].nunique():,}")
        if "last_purchase_date" in df.columns:
            print(
                f"   Rango de compras    : "
                f"{df['last_purchase_date'].min().strftime('%Y-%m-%d')} → "
                f"{df['last_purchase_date'].max().strftime('%Y-%m-%d')}"
            )
        if "monetary" in df.columns:
            print(f"   Revenue total       : ${df['monetary'].sum():,.2f}")

    @staticmethod
    def _print_rfm_summary(rfm_df: pd.DataFrame) -> None:
        print("\n   Distribución de segmentos:")
        seg_counts = rfm_df["segment"].value_counts()
        total      = len(rfm_df)
        for seg, count in seg_counts.items():
            bar   = "█" * int(count / total * 30)
            label = RfmAnalyzer.SEGMENT_LABELS.get(seg, seg)
            print(f"   {seg:<20} {count:>4} ({count/total*100:4.1f}%) {bar}")
            print(f"   {'':20} {label}")
        print()

    @staticmethod
    def _filter_by_segment(
        report: dict[str, Any],
        segment: str,
    ) -> dict[str, Any]:
        if segment not in report.get("segments", {}):
            available = list(report.get("segments", {}).keys())
            raise ValueError(
                f"Segmento '{segment}' no encontrado. "
                f"Disponibles: {available}"
            )
        return {
            "generated_at":  report["generated_at"],
            "analysis_type": report["analysis_type"],
            "segment_filter": segment,
            "segment_data":  report["segments"][segment],
            "pipeline_meta": report.get("pipeline_meta", {}),
        }
