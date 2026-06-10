from __future__ import annotations
import base64
import csv
import functools
import json
import logging
import time
import uuid
import warnings
from datetime import datetime, timezone, timedelta
from io import StringIO, BytesIO
from pathlib import Path
from typing import Any, Callable, Optional, Union
import pandas as pd

warnings.filterwarnings("ignore")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("ReportManager")

JsonDict  = dict[str, Any]
DBConn    = Any


def _audit(process_name: str) -> Callable:

    def decorator(fn: Callable) -> Callable:
        @functools.wraps(fn)
        def wrapper(self: "ReportManager", *args, **kwargs):
            t0    = time.perf_counter()
            error = None
            try:
                result = fn(self, *args, **kwargs)
                return result
            except Exception as exc:
                error = exc
                raise
            finally:
                elapsed = timedelta(seconds=time.perf_counter() - t0)
                level   = "ERROR" if error else "INFO"
                self._log_audit(
                    process_name   = f"ReportManager:{process_name}",
                    log_level      = level,
                    execution_time = elapsed,
                    error_message  = str(error) if error else None,
                    metadata       = {"report_id": self.report_id},
                )
        return wrapper
    return decorator


class ReportManager:

    def __init__(
        self,
        prediction_payload:     Union[JsonDict, list[JsonDict]],
        recommendation_payload: JsonDict,
        report_id:              Optional[str]  = None,
        model_name:             str            = "KNN",
        model_version:          str            = "1.0.0",
        language:               str            = "es",
        db_conn:                Optional[DBConn] = None,
        output_dir:             Union[str, Path] = "./reports",
    ) -> None:
        self.report_id    = report_id or str(uuid.uuid4())
        self.model_name   = model_name
        self.model_version = model_version
        self.language     = language
        self.db_conn      = db_conn
        self.output_dir   = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        self._predictions: list[JsonDict] = (
            prediction_payload
            if isinstance(prediction_payload, list)
            else [prediction_payload]
        )
        self._recommendations: JsonDict = recommendation_payload

        self._report: Optional[JsonDict] = None

        self._audit_queue: list[JsonDict] = []

        logger.info(
            "ReportManager inicializado → report_id='%s' | predictions=%d | model='%s'",
            self.report_id, len(self._predictions), model_name,
        )

    @_audit("build_report")
    def build_report(self) -> JsonDict:

        successful  = [p for p in self._predictions if p.get("status") == "success"]
        failed      = [p for p in self._predictions if p.get("status") != "success"]

        dist: dict[str, int] = {}
        confidences: list[float] = []
        for p in successful:
            pred = str(p.get("prediction", "unknown"))
            dist[pred] = dist.get(pred, 0) + 1
            conf = p.get("confidence", {})
            if isinstance(conf, dict) and conf.get("score") is not None:
                confidences.append(float(conf["score"]))

        avg_conf = round(sum(confidences) / len(confidences), 4) if confidences else 0.0

        detail = []
        for p in self._predictions:
            conf_score = None
            conf       = p.get("confidence")
            if isinstance(conf, dict):
                conf_score = conf.get("score")
            detail.append({
                "id":               str(p.get("id", "")),
                "prediction":       p.get("prediction"),
                "confidence_score": _safe_float(conf_score),
                "status":           p.get("status", "unknown"),
                "latency_ms":       _safe_float(p.get("meta", {}).get("latency_ms", 0)),
            })

        top_recos = _clean_list(
            self._recommendations.get("recommendations", [])
        )

        self._report = {
            "report_id":        self.report_id,
            "generated_at":     _now_iso(),
            "model_name":       self.model_name,
            "model_version":    self.model_version,
            "summary": {
                "total_predictions":       len(self._predictions),
                "successful":              len(successful),
                "failed":                  len(failed),
                "avg_confidence":          avg_conf,
                "prediction_distribution": dist,
            },
            "top_recommendations":  top_recos,
            "predictions_detail":   detail,
            "recommendation_type":  self._recommendations.get("recommendation_type", ""),
            "is_cold_start":        bool(self._recommendations.get("is_cold_start", False)),
        }

        logger.info(
            "build_report: OK → predictions=%d (ok=%d, fail=%d) | avg_conf=%.4f | recos=%d",
            len(self._predictions), len(successful), len(failed),
            avg_conf, len(top_recos),
        )
        return self._report

    @_audit("save_to_db")
    def save_to_db(self) -> dict[str, Optional[str]]:

        self._check_report_built()

        ids: dict[str, Optional[str]] = {
            "analysis_id":   None,
            "ai_content_id": None,
        }

        if self.db_conn is None:
            logger.warning(
                "save_to_db: db_conn=None. "
                "Operando en modo offline — no se persiste en BD."
            )
            return ids

        try:
            import psycopg2.extras as pgx

            with self.db_conn.cursor() as cur:
                pgx.register_uuid()

                cur.execute(
                    """
                    INSERT INTO analysis_results
                        (input_data_id, analysis_type, model_name,
                         model_version, result, confidence_score)
                    VALUES (%s, %s, %s, %s, %s::jsonb, %s)
                    RETURNING analysis_id
                    """,
                    (
                        self.report_id,
                        "prediction_report",
                        self.model_name,
                        self.model_version,
                        json.dumps(self._report, default=str),
                        self._report["summary"]["avg_confidence"],
                    ),
                )
                analysis_id = str(cur.fetchone()[0])
                ids["analysis_id"] = analysis_id

                text_content = self._build_text_summary()
                summary_line = text_content.split(".")[0] + "."
                tags         = ["prediction", "recommendation", self.model_name,
                                 self._report.get("recommendation_type", "")]

                cur.execute(
                    """
                    INSERT INTO ai_content
                        (record_id, text_content, summary, tags, model_used, language)
                    VALUES (%s, %s, %s, %s, %s, %s)
                    RETURNING ai_content_id
                    """,
                    (
                        analysis_id,
                        text_content,
                        summary_line,
                        tags,
                        self.model_name,
                        self.language,
                    ),
                )
                ids["ai_content_id"] = str(cur.fetchone()[0])

                for entry in self._audit_queue:
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
                            self.report_id,
                            entry.get("error_message"),
                            str(entry["execution_time"]),
                            json.dumps(entry.get("metadata", {})),
                        ),
                    )

            self.db_conn.commit()
            self._audit_queue.clear()

            logger.info(
                "save_to_db: OK → analysis_id=%s | ai_content_id=%s",
                ids["analysis_id"], ids["ai_content_id"],
            )

        except Exception as exc:
            logger.error("save_to_db: falló → %s", exc, exc_info=True)
            try:
                self.db_conn.rollback()
            except Exception:
                pass

        return ids

    @_audit("export_to_json")
    def export_to_json(
        self,
        output_dir: Optional[Union[str, Path]] = None,
        indent:     int = 2,
    ) -> str:

        self._check_report_built()
        dest = self._resolve_dir(output_dir) / f"{self.report_id}_report.json"
        dest.write_text(
            json.dumps(self._report, indent=indent, ensure_ascii=False, default=str),
            encoding="utf-8",
        )
        logger.info("export_to_json: guardado en '%s'", dest)
        return str(dest)

    @_audit("export_to_csv")
    def export_to_csv(
        self,
        output_dir: Optional[Union[str, Path]] = None,
        include_predictions: bool  = True,
        include_recos:       bool  = True,
    ) -> str:

        self._check_report_built()
        dest    = self._resolve_dir(output_dir) / f"{self.report_id}_report.csv"
        buf     = StringIO()
        writer  = csv.writer(buf)

        writer.writerow(["REPORTE DE IA — FASE 8"])
        writer.writerow(["report_id",    self._report["report_id"]])
        writer.writerow(["generated_at", self._report["generated_at"]])
        writer.writerow(["model",        f"{self.model_name} v{self.model_version}"])
        writer.writerow([])

        s = self._report["summary"]
        writer.writerow(["RESUMEN"])
        writer.writerow(["total_predictions", "successful", "failed", "avg_confidence"])
        writer.writerow([
            s["total_predictions"], s["successful"],
            s["failed"],            s["avg_confidence"],
        ])
        writer.writerow([])

        if include_predictions and self._report.get("predictions_detail"):
            writer.writerow(["PREDICCIONES"])
            df_pred = pd.DataFrame(self._report["predictions_detail"])
            df_pred.to_csv(buf, index=False)
            writer.writerow([])

        if include_recos and self._report.get("top_recommendations"):
            writer.writerow(["RECOMENDACIONES"])
            df_reco = pd.DataFrame(self._report["top_recommendations"])
            df_reco.to_csv(buf, index=False)

        dest.write_text("\ufeff" + buf.getvalue(), encoding="utf-8")
        logger.info("export_to_csv: guardado en '%s'", dest)
        return str(dest)

    @_audit("export_to_pdf")
    def export_to_pdf(
        self,
        output_dir:    Optional[Union[str, Path]] = None,
        return_base64: bool = False,
        title:         str  = "Reporte Ejecutivo de IA",
    ) -> str:

        self._check_report_built()

        try:
            from reportlab.lib.pagesizes import letter
            from reportlab.lib import colors
            from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
            from reportlab.lib.units import inch
            from reportlab.platypus import (
                SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
                HRFlowable, PageBreak,
            )
        except ImportError as exc:
            raise ImportError(
                "reportlab no está instalado. "
                "Instálalo con: pip install reportlab"
            ) from exc

        dest   = self._resolve_dir(output_dir) / f"{self.report_id}_report.pdf"
        buf    = BytesIO()
        styles = getSampleStyleSheet()

        style_title = ParagraphStyle(
            "ReportTitle",
            parent    = styles["Title"],
            fontSize  = 22,
            spaceAfter = 6,
            textColor  = colors.HexColor("#1a1a18"),
        )
        style_h1 = ParagraphStyle(
            "SectionH1",
            parent    = styles["Heading1"],
            fontSize  = 13,
            textColor = colors.HexColor("#1a1a18"),
            spaceBefore = 14,
            spaceAfter  = 6,
        )
        style_meta = ParagraphStyle(
            "Meta",
            parent    = styles["Normal"],
            fontSize  = 9,
            textColor = colors.HexColor("#888780"),
        )
        style_body = styles["Normal"]

        def _footer(canvas_obj, doc_obj):
            canvas_obj.saveState()
            canvas_obj.setFont("Helvetica", 8)
            canvas_obj.setFillColor(colors.HexColor("#888780"))
            canvas_obj.drawString(
                inch * 0.75,
                inch * 0.4,
                f"Reporte: {self.report_id}  |  "
                f"Generado: {self._report['generated_at'][:10]}  |  "
                f"Modelo: {self.model_name} v{self.model_version}",
            )
            canvas_obj.drawRightString(
                letter[0] - inch * 0.75,
                inch * 0.4,
                f"Pág. {doc_obj.page}",
            )
            canvas_obj.restoreState()

        doc = SimpleDocTemplate(
            buf,
            pagesize     = letter,
            rightMargin  = inch * 0.75,
            leftMargin   = inch * 0.75,
            topMargin    = inch,
            bottomMargin = inch * 0.75,
            title        = title,
            author       = f"Backend-IA / {self.model_name}",
        )

        story = []
        s     = self._report["summary"]

        story.append(Spacer(1, inch * 0.5))
        story.append(Paragraph(title, style_title))
        story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#d3d1c7")))
        story.append(Spacer(1, 8))
        story.append(Paragraph(
            f"Reporte ID: <b>{self.report_id}</b>  &nbsp;|&nbsp;  "
            f"Fecha: <b>{self._report['generated_at'][:10]}</b>  &nbsp;|&nbsp;  "
            f"Modelo: <b>{self.model_name} v{self.model_version}</b>",
            style_meta,
        ))
        story.append(Spacer(1, 20))

        story.append(Paragraph("Resumen Ejecutivo", style_h1))
        story.append(Paragraph(self._build_text_summary(), style_body))
        story.append(Spacer(1, 12))

        story.append(Paragraph("KPIs de Predicción", style_h1))
        kpi_data = [
            ["Total Predicciones", "Exitosas", "Fallidas", "Confianza Promedio"],
            [
                str(s["total_predictions"]),
                str(s["successful"]),
                str(s["failed"]),
                f"{s['avg_confidence']:.2%}",
            ],
        ]
        story.append(_styled_table(kpi_data, col_widths=[140, 100, 100, 140]))

        if s.get("prediction_distribution"):
            story.append(Spacer(1, 10))
            story.append(Paragraph("Distribución de Predicciones", style_h1))
            dist_data = [["Clase / Valor", "Cantidad"]] + [
                [str(k), str(v)]
                for k, v in s["prediction_distribution"].items()
            ]
            story.append(_styled_table(dist_data, col_widths=[250, 100]))

        story.append(PageBreak())

        detail = self._report.get("predictions_detail", [])
        if detail:
            story.append(Paragraph("Detalle de Predicciones", style_h1))
            pred_headers = ["ID Registro", "Predicción", "Confianza", "Estado", "Latencia (ms)"]
            pred_rows    = [pred_headers] + [
                [
                    str(r.get("id", ""))[:24],
                    str(r.get("prediction", "")),
                    f"{_safe_float(r.get('confidence_score', 0)):.4f}",
                    str(r.get("status", "")),
                    str(r.get("latency_ms", "")),
                ]
                for r in detail[:30]
            ]
            story.append(_styled_table(pred_rows, col_widths=[140, 80, 70, 70, 80]))
            if len(detail) > 30:
                story.append(Paragraph(
                    f"... y {len(detail) - 30} registros más. Ver el JSON completo.",
                    style_meta,
                ))

        story.append(PageBreak())

        recos = self._report.get("top_recommendations", [])
        if recos:
            reco_type = self._report.get("recommendation_type", "")
            story.append(Paragraph(f"Recomendaciones ({reco_type})", style_h1))
            reco_headers = ["Item ID", "Precio Base", "Confianza", "Descuento %"]
            reco_rows    = [reco_headers] + [
                [
                    str(r.get("item_id", ""))[:24],
                    f"${_safe_float(r.get('base_price', 0)):,.2f}",
                    f"{_safe_float(r.get('confidence_score', 0)):.4f}",
                    f"{_safe_float(r.get('pct_off', 0)):.1f}%",
                ]
                for r in recos
            ]
            story.append(_styled_table(reco_rows, col_widths=[160, 90, 80, 100]))

        doc.build(story, onFirstPage=_footer, onLaterPages=_footer)

        pdf_bytes = buf.getvalue()
        dest.write_bytes(pdf_bytes)
        logger.info("export_to_pdf: guardado en '%s' (%d bytes)", dest, len(pdf_bytes))

        if return_base64:
            return base64.b64encode(pdf_bytes).decode("utf-8")
        return str(dest)


    def _log_audit(
        self,
        process_name:   str,
        log_level:      str,
        execution_time: timedelta,
        error_message:  Optional[str]  = None,
        metadata:       Optional[dict] = None,
    ) -> None:

        entry = {
            "process_name":   process_name,
            "log_level":      log_level,
            "execution_time": execution_time,
            "error_message":  error_message,
            "metadata":       metadata or {},
            "created_at":     _now_iso(),
        }
        self._audit_queue.append(entry)

        log_fn = logger.error if log_level == "ERROR" else logger.info
        log_fn(
            "AUDIT [%s] %s → %.3fs%s",
            log_level, process_name,
            execution_time.total_seconds(),
            f" | ERR: {error_message}" if error_message else "",
        )

    def _build_text_summary(self) -> str:

        s     = self._report["summary"]
        recos = self._report.get("top_recommendations", [])
        rtype = self._report.get("recommendation_type", "N/A")
        cold  = self._report.get("is_cold_start", False)

        if self.language == "en":
            summary = (
                f"AI report generated by model '{self.model_name} v{self.model_version}' "
                f"on {self._report['generated_at'][:10]}. "
                f"A total of {s['total_predictions']} predictions were processed, "
                f"{s['successful']} successful and {s['failed']} failed, "
                f"with an average confidence score of {s['avg_confidence']:.2%}. "
            )
            if recos:
                top_price = max(_safe_float(r.get("base_price", 0)) for r in recos)
                summary += (
                    f"The recommendation engine ({rtype}) identified "
                    f"{len(recos)} complementary items, "
                    f"with a top base price of ${top_price:,.2f}. "
                )
            if cold:
                summary += "Cold Start strategy was applied due to insufficient user history. "
        else:
            summary = (
                f"Reporte de IA generado por el modelo '{self.model_name} v{self.model_version}' "
                f"el {self._report['generated_at'][:10]}. "
                f"Se procesaron {s['total_predictions']} predicciones en total, "
                f"{s['successful']} exitosas y {s['failed']} fallidas, "
                f"con un nivel de confianza promedio del {s['avg_confidence']:.2%}. "
            )
            if recos:
                top_price = max(_safe_float(r.get("base_price", 0)) for r in recos)
                summary += (
                    f"El motor de recomendaciones ({rtype}) identifico "
                    f"{len(recos)} articulos complementarios, "
                    f"con un precio base maximo de ${top_price:,.2f}. "
                )
            if cold:
                summary += "Se aplico estrategia Cold Start por historial insuficiente del usuario. "

        return summary.strip()

    def _check_report_built(self) -> None:
        if self._report is None:
            raise RuntimeError(
                "El reporte no ha sido construido. "
                "Llama a build_report() antes de exportar o guardar en BD."
            )

    def _resolve_dir(self, output_dir: Optional[Union[str, Path]]) -> Path:
        d = Path(output_dir).resolve() if output_dir else self.output_dir
        d.mkdir(parents=True, exist_ok=True)
        return d

    def __repr__(self) -> str:
        built = self._report is not None
        return (
            f"ReportManager("
            f"report_id='{self.report_id}', "
            f"model='{self.model_name}', "
            f"predictions={len(self._predictions)}, "
            f"built={built})"
        )


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _safe_float(value: Any) -> float:
    try:
        return round(float(value), 6)
    except (TypeError, ValueError):
        return 0.0


def _clean_list(items: list[Any]) -> list[JsonDict]:
    result = []
    for item in items:
        if not isinstance(item, dict):
            continue
        clean = {k: (str(v) if isinstance(v, uuid.UUID) else v)
                 for k, v in item.items()}
        result.append(clean)
    return result


def _styled_table(data: list[list], col_widths: list[int]) -> "Table":
    from reportlab.platypus import Table, TableStyle
    from reportlab.lib import colors

    table = Table(data, colWidths=col_widths, repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND",  (0, 0), (-1, 0),  colors.HexColor("#1a1a18")),
        ("TEXTCOLOR",   (0, 0), (-1, 0),  colors.white),
        ("FONTNAME",    (0, 0), (-1, 0),  "Helvetica-Bold"),
        ("FONTSIZE",    (0, 0), (-1, 0),  9),
        ("BOTTOMPADDING",(0,0), (-1, 0),  7),
        ("TOPPADDING",  (0, 0), (-1, 0),  7),

        ("ROWBACKGROUNDS",(0, 1),(-1,-1), [colors.white, colors.HexColor("#f5f5f3")]),
        ("FONTNAME",    (0, 1), (-1, -1), "Helvetica"),
        ("FONTSIZE",    (0, 1), (-1, -1), 8),
        ("TOPPADDING",  (0, 1), (-1, -1), 5),
        ("BOTTOMPADDING",(0,1), (-1, -1), 5),

        ("GRID",        (0, 0), (-1, -1), 0.4, colors.HexColor("#d3d1c7")),
        ("LINEBELOW",   (0, 0), (-1, 0),  1.0, colors.HexColor("#888780")),
        ("ALIGN",       (0, 0), (-1, -1), "LEFT"),
        ("VALIGN",      (0, 0), (-1, -1), "MIDDLE"),
    ]))
    return table
