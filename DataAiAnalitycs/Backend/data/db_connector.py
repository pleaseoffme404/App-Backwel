import psycopg2
from psycopg2 import OperationalError
import os
import logging

logger = logging.getLogger(__name__)

class Connect_BD:

    @staticmethod
    def _get_params() -> dict:
        return {
            "host":     os.getenv("host"),
            "database": os.getenv("database"),
            "user":     os.getenv("user"),
            "password": os.getenv("password"),
            "port":     os.getenv("port", "5432"),
        }

    @staticmethod
    def crear_conexion():
        params = Connect_BD._get_params()
        missing = [k for k, v in params.items() if not v]
        if missing:
            logger.error(
                "Variables de entorno faltantes: %s — "
                "verifica tu docker-compose.yml o .env",
                missing
            )
            return None

        try:
            conexion = psycopg2.connect(**params)
            logger.info(
                "Conexión a PostgreSQL exitosa → %s:%s/%s",
                params["host"], params["port"], params["database"]
            )
            return conexion

        except OperationalError as e:
            logger.error("Error al conectar a PostgreSQL: %s", e)
            return None

    @staticmethod
    def cerrar_conexion(conexion):
        if conexion is not None:
            conexion.close()
            logger.info("Conexión a PostgreSQL cerrada.")


if __name__ == "__main__":
    conn = Connect_BD.crear_conexion()
    Connect_BD.cerrar_conexion(conn)