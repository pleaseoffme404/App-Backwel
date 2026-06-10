import os
import logging
import argparse
from datetime import datetime, timezone
from typing import Optional
import pandas as pd
from psycopg2.extras import RealDictCursor

from Backend.data.db_connector import Connect_BD

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("query_raw_data")


def get_connection():
    conn = Connect_BD.crear_conexion()
    if conn is None:
        raise ConnectionError("No se pudo establecer conexión con la base de datos.")
    logger.info("Conexión establecida via Connect_BD.")
    return conn


def _fetch(conn, sql: str, params: dict) -> pd.DataFrame:

    with conn.cursor(cursor_factory=RealDictCursor) as cur:
        cur.execute(sql, params)
        rows = cur.fetchall()
    df = pd.DataFrame(rows)
    logger.info("  → %d filas obtenidas.", len(df))
    return df

def query_processed_data(
    conn,
    start_date:  Optional[str] = None,
    end_date:    Optional[str] = None,
    record_type: Optional[str] = None,
    status:      Optional[str] = None,
    category:    Optional[str] = None,
    batch_id:    Optional[str] = None,
) -> pd.DataFrame:

    logger.info("Extrayendo processed_data...")

    sql = """
        SELECT
            record_id,
            raw_id,
            batch_id,
            processed_at,
            status,
            event_date,
            created_at,
            updated_at,
            user_id,
            customer_id,
            product_id,
            category,
            type,
            amount,
            quantity,
            score,
            value,
            source,
            region,
            device,
            data
        FROM processed_data
        WHERE
            (%(start_date)s::timestamptz IS NULL OR event_date >= %(start_date)s::timestamptz)
            AND (%(end_date)s::timestamptz   IS NULL OR event_date <= %(end_date)s::timestamptz)
            AND (%(record_type)s IS NULL OR type     = %(record_type)s)
            AND (%(status)s      IS NULL OR status   = %(status)s)
            AND (%(category)s    IS NULL OR category = %(category)s)
            AND (%(batch_id)s::uuid IS NULL OR batch_id = %(batch_id)s::uuid)
        ORDER BY event_date DESC
    """
    params = {
        "start_date":  start_date,
        "end_date":    end_date,
        "record_type": record_type,
        "status":      status,
        "category":    category,
        "batch_id":    batch_id,
    }
    return _fetch(conn, sql, params)


def query_ingestions(
    conn,
    status:     Optional[str] = None,
    start_date: Optional[str] = None,
    end_date:   Optional[str] = None,
    source_id:  Optional[str] = None,
) -> pd.DataFrame:

    logger.info("Extrayendo data_ingestions...")

    sql = """
        SELECT
            di.batch_id,
            di.source_id,
            ds.source_type,
            ds.file_name   AS source_file_name,
            di.ingestion_date,
            di.file_name,
            di.record_count,
            di.status,
            di.notes,
            di.created_at
        FROM data_ingestions di
        JOIN data_sources ds ON ds.source_id = di.source_id
        WHERE
            (%(status)s    IS NULL OR di.status    = %(status)s)
            AND (%(source_id)s::uuid IS NULL OR di.source_id = %(source_id)s::uuid)
            AND (%(start_date)s::timestamptz IS NULL OR di.ingestion_date >= %(start_date)s::timestamptz)
            AND (%(end_date)s::timestamptz   IS NULL OR di.ingestion_date <= %(end_date)s::timestamptz)
        ORDER BY di.ingestion_date DESC
    """
    params = {
        "status":     status,
        "source_id":  source_id,
        "start_date": start_date,
        "end_date":   end_date,
    }
    return _fetch(conn, sql, params)

def query_raw_data(
    conn,
    batch_id:    Optional[str] = None,
    file_format: Optional[str] = None,
    start_date:  Optional[str] = None,
    end_date:    Optional[str] = None,
) -> pd.DataFrame:

    logger.info("Extrayendo raw_data...")

    sql = """
        SELECT
            r.raw_id,
            r.batch_id,
            r.source_id,
            ds.source_type,
            ds.file_name   AS source_file_name,
            r.ingestion_date,
            r.file_format,
            r.raw_content,
            r.checksum,
            r.created_at
        FROM raw_data r
        JOIN data_sources ds ON ds.source_id = r.source_id
        WHERE
            (%(batch_id)s::uuid IS NULL OR r.batch_id = %(batch_id)s::uuid)
            AND (%(file_format)s IS NULL OR r.file_format = %(file_format)s)
            AND (%(start_date)s::timestamptz IS NULL OR r.ingestion_date >= %(start_date)s::timestamptz)
            AND (%(end_date)s::timestamptz   IS NULL OR r.ingestion_date <= %(end_date)s::timestamptz)
        ORDER BY r.ingestion_date DESC
    """
    params = {
        "batch_id":    batch_id,
        "file_format": file_format,
        "start_date":  start_date,
        "end_date":    end_date,
    }
    return _fetch(conn, sql, params)

def query_sources(
    conn,
    source_type: Optional[str] = None,
) -> pd.DataFrame:

    logger.info("Extrayendo data_sources...")

    sql = """
        SELECT
            ds.source_id,
            ds.source_type,
            ds.file_name,
            ds.endpoint,
            ds.description,
            ds.created_at,
            COUNT(di.batch_id)      AS total_batches,
            MAX(di.ingestion_date)  AS last_ingestion,
            SUM(di.record_count)    AS total_records_ingested
        FROM data_sources ds
        LEFT JOIN data_ingestions di ON di.source_id = ds.source_id
        WHERE (%(source_type)s IS NULL OR ds.source_type = %(source_type)s)
        GROUP BY ds.source_id, ds.source_type, ds.file_name,
                 ds.endpoint, ds.description, ds.created_at
        ORDER BY ds.created_at DESC
    """
    params = {"source_type": source_type}
    return _fetch(conn, sql, params)


def query_audit_logs(
    conn,
    log_level:  Optional[str] = None,
    batch_id:   Optional[str] = None,
    start_date: Optional[str] = None,
    end_date:   Optional[str] = None,
    only_errors: bool = False,
) -> pd.DataFrame:

    logger.info("Extrayendo audit_logs...")

    sql = """
        SELECT
            log_id,
            process_name,
            log_level,
            batch_id,
            record_id,
            analysis_id,
            error_message,
            execution_time,
            metadata,
            created_at
        FROM audit_logs
        WHERE
            (%(log_level)s   IS NULL OR log_level = %(log_level)s)
            AND (%(batch_id)s::uuid IS NULL OR batch_id = %(batch_id)s::uuid)
            AND (%(start_date)s::timestamptz IS NULL OR created_at >= %(start_date)s::timestamptz)
            AND (%(end_date)s::timestamptz   IS NULL OR created_at <= %(end_date)s::timestamptz)
            AND (NOT %(only_errors)s OR error_message IS NOT NULL)
        ORDER BY created_at DESC
    """
    params = {
        "log_level":   log_level,
        "batch_id":    batch_id,
        "start_date":  start_date,
        "end_date":    end_date,
        "only_errors": only_errors,
    }
    return _fetch(conn, sql, params)

class RawDataPipeline:


    AVAILABLE_QUERIES = {
        "processed_data",
        "ingestions",
        "raw_data",
        "sources",
        "audit_logs",
        "all",
    }

    def __init__(self):
        self.datasets: dict[str, pd.DataFrame] = {}
        self._conn = None


    def __enter__(self):
        self._conn = get_connection()
        return self

    def __exit__(self, *_):
        if self._conn:
            Connect_BD.cerrar_conexion(self._conn)
            logger.info("Conexión cerrada via Connect_BD.")


    def run(
        self,
        query:       str = "all",
        start_date:  Optional[str] = None,
        end_date:    Optional[str] = None,
        record_type: Optional[str] = None,
        status:      Optional[str] = None,
        category:    Optional[str] = None,
        batch_id:    Optional[str] = None,
        source_type: Optional[str] = None,
        file_format: Optional[str] = None,
        log_level:   Optional[str] = None,
        only_errors: bool = False,
    ) -> dict[str, pd.DataFrame]:

        if query not in self.AVAILABLE_QUERIES:
            raise ValueError(
                f"Query '{query}' no reconocido. Opciones: {self.AVAILABLE_QUERIES}"
            )

        if self._conn is None:
            self._conn = get_connection()

        run_all = query == "all"

        if run_all or query == "processed_data":
            self.datasets["processed_data"] = query_processed_data(
                self._conn,
                start_date=start_date,
                end_date=end_date,
                record_type=record_type,
                status=status,
                category=category,
                batch_id=batch_id,
            )

        if run_all or query == "ingestions":
            self.datasets["ingestions"] = query_ingestions(
                self._conn,
                status=status,
                start_date=start_date,
                end_date=end_date,
            )

        if run_all or query == "raw_data":
            self.datasets["raw_data"] = query_raw_data(
                self._conn,
                batch_id=batch_id,
                file_format=file_format,
                start_date=start_date,
                end_date=end_date,
            )

        if run_all or query == "sources":
            self.datasets["sources"] = query_sources(
                self._conn,
                source_type=source_type,
            )

        if run_all or query == "audit_logs":
            self.datasets["audit_logs"] = query_audit_logs(
                self._conn,
                log_level=log_level,
                batch_id=batch_id,
                start_date=start_date,
                end_date=end_date,
                only_errors=only_errors,
            )

        self._log_summary()
        return self.datasets


    def _log_summary(self):
        logger.info("=" * 55)
        logger.info("Resumen — datasets extraídos:")
        for name, df in self.datasets.items():
            logger.info("  %-20s → %d filas, %d columnas", name, *df.shape)
        logger.info("=" * 55)

def parse_args():
    parser = argparse.ArgumentParser(
        description="Query Raw Data — Fase 2, extrae datos del pipeline de ingesta."
    )
    parser.add_argument(
        "--query",
        choices=RawDataPipeline.AVAILABLE_QUERIES,
        default="all",
        help="Dataset a extraer (default: all)",
    )
    parser.add_argument("--start",       default=None, help="Fecha inicio  YYYY-MM-DD")
    parser.add_argument("--end",         default=None, help="Fecha fin     YYYY-MM-DD")
    parser.add_argument("--type",        default=None, help="Tipo de registro (processed_data)")
    parser.add_argument("--status",      default=None, help="Estado: processed | pending | failed | completed")
    parser.add_argument("--category",    default=None, help="Categoría (processed_data)")
    parser.add_argument("--batch-id",    default=None, help="UUID del batch")
    parser.add_argument("--source-type", default=None, help="Tipo de fuente (sources)")
    parser.add_argument("--file-format", default=None, help="Formato del archivo (raw_data)")
    parser.add_argument("--log-level",   default=None, help="Nivel de log: INFO | ERROR | WARNING")
    parser.add_argument("--only-errors", action="store_true", help="Solo registros con error (audit_logs)")
    parser.add_argument("--output-dir",  default="./output", help="Directorio de salida para CSVs")
    parser.add_argument("--save-csv",    action="store_true", help="Guarda los datasets como CSV")
    return parser.parse_args()


def main():
    args = parse_args()

    with RawDataPipeline() as pipeline:
        datasets = pipeline.run(
            query=args.query,
            start_date=args.start,
            end_date=args.end,
            record_type=args.type,
            status=args.status,
            category=args.category,
            batch_id=args.batch_id,
            source_type=args.source_type,
            file_format=args.file_format,
            log_level=args.log_level,
            only_errors=args.only_errors,
        )

        if args.save_csv:
            os.makedirs(args.output_dir, exist_ok=True)
            ts = datetime.now(tz=timezone.utc).strftime("%Y%m%d_%H%M%S")
            for name, df in datasets.items():
                path = os.path.join(args.output_dir, f"{name}_{ts}.csv")
                df.to_csv(path, index=False)
                logger.info("Guardado: %s", path)

    return datasets


if __name__ == "__main__":
    main()

    from query_raw_data import RawDataPipeline

    with RawDataPipeline() as pipeline:
        datasets = pipeline.run(query="processed_data")

    df = datasets["processed_data"]

    print("=== TIPOS DE COLUMNAS ===")
    print(df.dtypes)

    print("\n=== VALORES ÚNICOS ===")
    for col in ["type", "category", "status", "region", "device", "source"]:
        print(f"{col}: {df[col].unique()}")

    print("\n=== MUESTRA DE DATOS (3 filas) ===")
    print(df.head(3).to_string())

    print("\n=== NULOS POR COLUMNA ===")
    print(df.isnull().sum())