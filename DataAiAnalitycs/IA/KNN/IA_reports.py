from __future__ import annotations

import base64
import csv
import functools
import io
import json
import logging
import os
import time
import traceback
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable, Optional, Union

import numpy as np
import pandas as pd

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("ReportManager")

JSONDict   = dict[str, Any]
CursorLike = Any          # psycopg2 cursor o similar


def _audited(process_name: Optional[str] = None) -> Callable:

    def decorator(fn: Callable) -> Callable:
        @functools.wraps(fn)
        def wrapper(self: "ReportManager", *args, **kwargs):
            pname  = process_name or f"ReportManager.{fn.__name__}"
            t0     = time.perf_counter()
            entry: JSONDict = {
                "process_name":  pname,
                "log_level":     "INFO",
                "execution_time": None,
                "error_message":  None,
                "metadata":       {"args": str(args)[:200], "kwargs": str(kwargs)[:200]},
            }
            try:
                result = fn(self, *args, **kwargs)
                elapsed = time.perf_counter() - t0
                entry["execution_time"] = elapsed
                entry["metadata"]["elapsed_ms"] = round(elapsed * 1000, 2)
                logger.info("[AUDIT] %s → OK (%.0f ms)", pname, elapsed * 1000)
                self._audit_log.append(entry)
                return result
            except Exception as exc:
                elapsed = time.perf_counter() - t0
                entry["log_level"]      = "ERROR"
                entry["execution_time"] = elapsed
                entry["error_message"]  = f"{type(exc).__name__}: {exc}"
                entry["metadata"]["traceback"] = traceback.format_exc()[-800:]
                logger.error("[AUDIT] %s → ERROR (%.0f ms): %s", pname, elapsed * 1000, exc)
                self._audit_log.append(entry)
                raise
        return wrapper
    return decorator

class ReportManager:

    def __init__(
        self,
        predictions:     Union[JSONDict, list[JSONDict]],
        recommendations: JSONDict,
        report_meta:     Optional[JSONDict] = None,
        model_name:      str = "KNN-POS",
        model_version:   str = "1.0.0",
        language:        str = "es",
    ) -> None:
        self.model_name    = model_name
        self.model_version = model_version
        self.language      = language
        self.report_meta   = report_meta or {}

        self._raw_predictions: list[JSONDict] = (
            predictions if isinstance(predictions, list) else [predictions]
        )
        self._raw_recommendations: JSONDict = recommendations

        self._report:    Optional[JSONDict] = None
        self._report_id: str = str(uuid.uuid4())

        self._audit_log: list[JSONDict] = []

        logger.info(
            "ReportManager inicializado → id=%s | predictions=%d | model=%s",
            self._report_id[:8], len(self._raw_predictions), model_name,
        )

    @_audited("build_report")
    def build_report(self) -> JSONDict:

        preds = self._clean_predictions(self._raw_predictions)
        recos = self._raw_recommendations.get("recommendations", [])

        successful = [p for p in preds if p.get("status") == "success"]
        failed     = [p for p in preds if p.get("status") != "success"]

        confidence_scores = [
            p["confidence"]["score"]
            for p in successful
            if p.get("confidence") and p["confidence"].get("score") is not None
        ]
        avg_confidence = (
            round(float(np.mean(confidence_scores)), 4)
            if confidence_scores else 0.0
        )

        predicted_values = [
            p.get("prediction") for p in successful if p.get("prediction") is not None
        ]

        reco_confidences = [
            r.get("confidence_score", 0.0) for r in recos
        ]
        avg_reco_conf = (
            round(float(np.mean(reco_confidences)), 4)
            if reco_confidences else 0.0
        )

        total_discount = sum(
            r.get("base_price", 0.0) * r.get("pct_off", 0.0) / 100.0
            for r in recos
        )

        self._report = {
            "report_id":    self._report_id,
            "generated_at": _now_iso(),
            "meta":         {**self.report_meta},
            "summary": {
                "total_records":          len(preds),
                "successful_predictions": len(successful),
                "failed_predictions":     len(failed),
                "avg_confidence":         avg_confidence,
                "predicted_values":       [_serialize(v) for v in predicted_values],
            },
            "predictions":  preds,
            "recommendations": {
                "type":          self._raw_recommendations.get("recommendation_type", ""),
                "count":         len(recos),
                "items":         [_clean_reco(r) for r in recos],
                "is_cold_start": bool(self._raw_recommendations.get("is_cold_start", False)),
            },
            "kpis": {
                "top_predicted_value":            _serialize(predicted_values[0]) if predicted_values else None,
                "top_recommended_item":           str(recos[0]["item_id"]) if recos else None,
                "avg_recommendation_confidence":  avg_reco_conf,
                "total_discount_exposure":        round(float(total_discount), 4),
            },
            "model_info": {
                "model_name":    self.model_name,
                "model_version": self.model_version,
                "language":      self.language,
            },
        }

        logger.info(
            "build_report: OK → %d predicciones | %d recomendaciones | conf=%.3f",
            len(preds), len(recos), avg_confidence,
        )
        return self._report

    @_audited("save_to_db")
    def save_to_db(self, cur: CursorLike) -> dict[str, str]:

        self._require_report()

        import psycopg2.extras  # type: ignore

        report   = self._report
        conf     = report["summary"]["avg_confidence"]

        analysis_id = str(uuid.uuid4())
        cur.execute(
            """
            INSERT INTO analysis_results
                (analysis_id, input_data_id, analysis_type, model_name,
                 model_version, result, confidence_score)
            VALUES (%s, %s, %s, %s, %s, %s::jsonb, %s)
            """,
            (
                analysis_id,
                self._report_id,
                report["recommendations"]["type"] or "prediction_report",
                self.model_name,
                self.model_version,
                json.dumps(report, ensure_ascii=False, default=str),
                conf,
            ),
        )
        logger.info("save_to_db: analysis_results insertado → %s", analysis_id)

        ai_content_id = str(uuid.uuid4())
        text, summary, tags = self._generate_text_content(report)

        cur.execute(
            """
            INSERT INTO ai_content
                (ai_content_id, record_id, text_content, summary,
                 tags, model_used, language)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            """,
            (
                ai_content_id,
                analysis_id,
                text,
                summary,
                tags,
                self.model_name,
                self.language,
            ),
        )
        logger.info("save_to_db: ai_content insertado → %s", ai_content_id)

        for entry in self._audit_log:
            elapsed_interval = (
                f"{entry['execution_time']} seconds"
                if entry["execution_time"] is not None else None
            )
            cur.execute(
                """
                INSERT INTO audit_logs
                    (process_name, log_level, batch_id,
                     error_message, execution_time, metadata)
                VALUES (%s, %s, %s, %s, %s::interval, %s::jsonb)
                """,
                (
                    entry["process_name"],
                    entry["log_level"],
                    self._report_id,
                    entry["error_message"],
                    elapsed_interval,
                    json.dumps(entry["metadata"], ensure_ascii=False, default=str),
                ),
            )
        logger.info("save_to_db: %d entradas de audit_log persistidas.", len(self._audit_log))

        return {"analysis_id": analysis_id, "ai_content_id": ai_content_id}

    @_audited("export_to_json")
    def export_to_json(
        self,
        path:         Optional[Union[str, Path]] = None,
        indent:       int  = 2,
        return_string: bool = False,
    ) -> Union[str, Path]:

        self._require_report()
        json_str = json.dumps(self._report, indent=indent, ensure_ascii=False, default=str)

        if path is None or return_string:
            logger.info("export_to_json: devolviendo string (%d bytes).", len(json_str))
            return json_str

        dest = Path(path)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(json_str, encoding="utf-8")
        logger.info("export_to_json: guardado → %s", dest)
        return dest

    @_audited("export_to_csv")
    def export_to_csv(
        self,
        path:          Optional[Union[str, Path]] = None,
        include_recos: bool = True,
    ) -> Union[str, Path]:

        self._require_report()

        preds_rows = []
        for p in self._report["predictions"]:
            conf = p.get("confidence") or {}
            preds_rows.append({
                "record_id":        str(p.get("id", "")),
                "prediction":       p.get("prediction"),
                "confidence_score": conf.get("score"),
                "model_type":       p.get("meta", {}).get("model_type", ""),
                "predicted_at":     p.get("meta", {}).get("predicted_at", ""),
                "status":           p.get("status", ""),
            })
        df_preds = pd.DataFrame(preds_rows)

        df_recos = pd.DataFrame(self._report["recommendations"]["items"]) \
            if self._report["recommendations"]["items"] else pd.DataFrame()

        if path is None:
            buf = io.StringIO()
            df_preds.to_csv(buf, index=False)
            logger.info("export_to_csv: devolviendo string CSV (%d filas).", len(df_preds))
            return buf.getvalue()

        dest = Path(path)
        dest.parent.mkdir(parents=True, exist_ok=True)

        if dest.suffix.lower() == ".xlsx":
            with pd.ExcelWriter(dest, engine="openpyxl") as writer:
                df_preds.to_excel(writer, sheet_name="Predictions", index=False)
                if include_recos and not df_recos.empty:
                    df_recos.to_excel(writer, sheet_name="Recommendations", index=False)
            logger.info("export_to_csv: Excel guardado → %s", dest)
        else:
            df_preds.to_csv(dest, index=False)
            if include_recos and not df_recos.empty:
                reco_dest = dest.with_name(dest.stem + "_recos" + dest.suffix)
                df_recos.to_csv(reco_dest, index=False)
                logger.info("export_to_csv: recomendaciones → %s", reco_dest)
            logger.info("export_to_csv: CSV guardado → %s", dest)

        return dest

    @_audited("export_to_pdf")
    def export_to_pdf(
        self,
        output_dir:    Union[str, Path] = ".",
        return_base64: bool             = False,
    ) -> Union[Path, str]:

        self._require_report()

        try:
            from fpdf import FPDF, XPos, YPos  # type: ignore
        except ImportError as exc:
            raise ImportError(
                "fpdf2 no está instalado. Ejecuta: pip install fpdf2"
            ) from exc

        report = self._report
        meta   = report.get("meta", {})
        summ   = report["summary"]
        kpis   = report["kpis"]
        recos  = report["recommendations"]["items"]
        preds  = report["predictions"]

        pdf = FPDF()
        pdf.set_auto_page_break(auto=True, margin=15)
        pdf.add_page()

        pdf.set_font("Helvetica", "B", 18)
        pdf.set_fill_color(30, 30, 30)
        pdf.set_text_color(255, 255, 255)
        pdf.cell(0, 12,
                 _pdf_safe(meta.get("report_title", "Reporte de IA - POS / E-commerce")),
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT, fill=True, align="C")

        pdf.set_text_color(0, 0, 0)
        pdf.set_font("Helvetica", "", 9)
        pdf.cell(0, 7,
                 _pdf_safe(
                     f"Generado: {report['generated_at']}   |   "
                     f"Modelo: {self.model_name} v{self.model_version}   |   "
                     f"Store: {meta.get('store_id', 'N/A')}   |   "
                     f"Periodo: {meta.get('period', 'N/A')}"
                 ),
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT, align="C")
        pdf.ln(4)

        _pdf_section_title(pdf, "1. Resumen Ejecutivo")
        kpi_rows = [
            ("Total de registros procesados", str(summ["total_records"])),
            ("Predicciones exitosas",         str(summ["successful_predictions"])),
            ("Predicciones fallidas",         str(summ["failed_predictions"])),
            ("Confianza promedio (modelo)",   f"{summ['avg_confidence']:.2%}"),
            ("Valor predicho top",            str(kpis.get("top_predicted_value", "-"))),
            ("Ítem recomendado top",          str(kpis.get("top_recommended_item", "-"))[:36]),
            ("Confianza promedio (recos)",    f"{kpis['avg_recommendation_confidence']:.2%}"),
            ("Exposición total descuentos",   f"${kpis['total_discount_exposure']:,.2f}"),
        ]
        _pdf_kv_table(pdf, kpi_rows)
        pdf.ln(4)

        _pdf_section_title(pdf, "2. Predicciones del Modelo")
        pred_headers = ["ID Registro", "Predicción", "Confianza", "Estado"]
        pred_rows    = []
        for p in preds[:100]:   #Mover esta mamada para los reportes del pdf y que sean mas o menos
            conf = p.get("confidence") or {}
            sc   = conf.get("score")
            pred_rows.append([
                str(p.get("id", ""))[:20],
                str(p.get("prediction", "-")),
                f"{sc:.2%}" if sc is not None else "-",
                p.get("status", ""),
            ])
        _pdf_table(pdf, pred_headers, pred_rows, col_widths=[55, 35, 30, 25])
        if len(preds) > 25:
            pdf.set_font("Helvetica", "I", 8)
            pdf.cell(0, 5, f"... y {len(preds) - 25} registros más.", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        pdf.ln(4)

        _pdf_section_title(pdf, "3. Recomendaciones")
        reco_type = report["recommendations"]["type"].replace("_", " ").title()
        pdf.set_font("Helvetica", "I", 9)
        pdf.cell(0, 5,
                 f"Tipo: {reco_type}   |   "
                 f"Cold Start: {'Sí' if report['recommendations']['is_cold_start'] else 'No'}",
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        pdf.ln(2)

        if recos:
            reco_headers = ["Item ID", "Precio Base", "Confianza", "Descuento %"]
            reco_rows    = [
                [
                    str(r.get("item_id", ""))[:36],
                    f"${float(r.get('base_price', 0)):,.2f}",
                    f"{float(r.get('confidence_score', 0)):.2%}",
                    f"{float(r.get('pct_off', 0)):.1f}%",
                ]
                for r in recos
            ]
            _pdf_table(pdf, reco_headers, reco_rows, col_widths=[75, 35, 30, 30])
        else:
            pdf.set_font("Helvetica", "I", 9)
            pdf.cell(0, 5, "Sin recomendaciones disponibles.", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        pdf.ln(4)

        pdf.set_font("Helvetica", "I", 7)
        pdf.set_text_color(150, 150, 150)
        pdf.cell(0, 5,
                 f"Report ID: {self._report_id}   |   "
                 f"Backend-IA Phase 8   |   Confidencial",
                 align="C")

        pdf_bytes = pdf.output()

        if return_base64:
            b64 = base64.b64encode(pdf_bytes).decode("ascii")
            logger.info("export_to_pdf: %d bytes → base64 string.", len(pdf_bytes))
            return b64

        dest = Path(output_dir)
        dest.mkdir(parents=True, exist_ok=True)
        filename = dest / f"ai_report_{self._report_id[:8]}_{_ts_filename()}.pdf"
        filename.write_bytes(pdf_bytes)
        logger.info("export_to_pdf: PDF guardado → %s", filename)
        return filename

    def _generate_text_content(
        self, report: JSONDict
    ) -> tuple[str, str, list[str]]:

        summ  = report["summary"]
        kpis  = report["kpis"]
        meta  = report["meta"]
        recos = report["recommendations"]

        text_content = (
            f"REPORTE DE IA — {self.model_name} v{self.model_version}\n"
            f"Generado: {report['generated_at']}\n"
            f"Tienda: {meta.get('store_id', 'N/A')} | Período: {meta.get('period', 'N/A')}\n\n"
            f"PREDICCIONES\n"
            f"Total registros: {summ['total_records']}\n"
            f"Exitosas: {summ['successful_predictions']} | "
            f"Fallidas: {summ['failed_predictions']}\n"
            f"Confianza promedio: {summ['avg_confidence']:.2%}\n"
            f"Valor top predicho: {kpis['top_predicted_value']}\n\n"
            f"RECOMENDACIONES ({recos['type']})\n"
            f"Total recomendaciones: {recos['count']}\n"
            f"Cold Start: {'Sí' if recos['is_cold_start'] else 'No'}\n"
            f"Ítem top: {kpis['top_recommended_item']}\n"
            f"Confianza promedio recos: {kpis['avg_recommendation_confidence']:.2%}\n"
            f"Exposición total descuentos: ${kpis['total_discount_exposure']:,.2f}\n"
        )

        summary = (
            f"El modelo {self.model_name} procesó {summ['total_records']} registros "
            f"con una confianza promedio del {summ['avg_confidence']:.0%}. "
            f"Se generaron {recos['count']} recomendaciones de tipo '{recos['type']}'. "
            f"La exposición total en descuentos es de ${kpis['total_discount_exposure']:,.2f}."
        )

        tags = [
            self.model_name,
            recos["type"],
            f"store_{meta.get('store_id', 'unknown')}",
            f"period_{meta.get('period', 'unknown')}",
            "cold_start" if recos["is_cold_start"] else "personalized",
            f"confidence_{int(summ['avg_confidence'] * 100)}pct",
        ]

        return text_content, summary, tags

    def _require_report(self) -> None:
        if self._report is None:
            raise RuntimeError(
                "El reporte no ha sido construido. Llama build_report() primero."
            )

    @staticmethod
    def _clean_predictions(preds: list[JSONDict]) -> list[JSONDict]:

        clean = []
        for p in preds:
            conf = p.get("confidence") or {}
            clean.append({
                "id":         str(p.get("id", "")),
                "status":     str(p.get("status", "unknown")),
                "prediction": _serialize(p.get("prediction")),
                "confidence": {
                    "score":  _safe_float(conf.get("score")),
                    "method": str(conf.get("method", "")),
                    "probability_per_class": {
                        str(k): _safe_float(v)
                        for k, v in (conf.get("probability_per_class") or {}).items()
                    } or None,
                },
                "meta":       {
                    "task":         str(p.get("meta", {}).get("task", "")),
                    "model_type":   str(p.get("meta", {}).get("model_type", "")),
                    "predicted_at": str(p.get("meta", {}).get("predicted_at", "")),
                    "latency_ms":   _safe_float(p.get("meta", {}).get("latency_ms")),
                },
                "error": p.get("error"),
            })
        return clean

    def __repr__(self) -> str:
        built = self._report is not None
        return (
            f"ReportManager("
            f"id={self._report_id[:8]}, "
            f"model='{self.model_name}', "
            f"built={built}, "
            f"predictions={len(self._raw_predictions)})"
        )


def _pdf_section_title(pdf: Any, title: str) -> None:
    from fpdf import XPos, YPos  # type: ignore
    pdf.set_font("Helvetica", "B", 11)
    pdf.set_fill_color(55, 55, 55)
    pdf.set_text_color(255, 255, 255)
    pdf.cell(0, 8, title, new_x=XPos.LMARGIN, new_y=YPos.NEXT, fill=True)
    pdf.set_text_color(0, 0, 0)
    pdf.ln(1)


def _pdf_kv_table(pdf: Any, rows: list[tuple[str, str]]) -> None:
    from fpdf import XPos, YPos  # type: ignore
    pdf.set_font("Helvetica", "", 9)
    for i, (k, v) in enumerate(rows):
        fill_color = (240, 240, 240) if i % 2 == 0 else (255, 255, 255)
        pdf.set_fill_color(*fill_color)
        pdf.cell(95, 6, k, border=1, fill=True)
        pdf.cell(95, 6, v, border=1, fill=True,
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT)


def _pdf_table(
    pdf:        Any,
    headers:    list[str],
    rows:       list[list[str]],
    col_widths: Optional[list[int]] = None,
) -> None:

    from fpdf import XPos, YPos  # type: ignore

    n     = len(headers)
    avail = 190
    widths = col_widths if col_widths else [avail // n] * n

    pdf.set_font("Helvetica", "B", 9)
    pdf.set_fill_color(30, 30, 30)
    pdf.set_text_color(255, 255, 255)
    for h, w in zip(headers, widths):
        pdf.cell(w, 7, h, border=1, fill=True)
    pdf.ln()

    pdf.set_text_color(0, 0, 0)
    pdf.set_font("Helvetica", "", 8)
    for ri, row in enumerate(rows):
        fill_color = (240, 240, 240) if ri % 2 == 0 else (255, 255, 255)
        pdf.set_fill_color(*fill_color)
        for cell, w in zip(row, widths):
            pdf.cell(w, 6, str(cell)[:40], border=1, fill=True)
        pdf.ln()

def _serialize(value: Any) -> Any:
    if value is None:
        return None
    if isinstance(value, (np.integer,)):
        return int(value)
    if isinstance(value, (np.floating,)):
        return None if np.isnan(value) else float(value)
    if isinstance(value, np.ndarray):
        return value.tolist()
    if isinstance(value, (pd.Timestamp,)):
        return value.isoformat()
    return value

def _safe_float(value: Any) -> Optional[float]:
    if value is None:
        return None
    try:
        return round(float(value), 6)
    except (TypeError, ValueError):
        return None

def _clean_reco(r: JSONDict) -> JSONDict:
    return {
        "item_id":          str(r.get("item_id", "")),
        "base_price":       _safe_float(r.get("base_price", 0.0)) or 0.0,
        "confidence_score": _safe_float(r.get("confidence_score", 0.0)) or 0.0,
        "pct_off":          _safe_float(r.get("pct_off", 0.0)) or 0.0,
    }

def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()

def _pdf_safe(text: str) -> str:
    return text.encode("latin-1", errors="replace").decode("latin-1")

def _ts_filename() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
