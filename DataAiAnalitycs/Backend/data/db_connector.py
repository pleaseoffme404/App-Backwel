import psycopg2
from dotenv import load_dotenv
from psycopg2 import OperationalError
import os
from pathlib import Path

load_dotenv(override=False)

host = os.getenv("host")
database = os.getenv("database")
user = os.getenv("user")
password = os.getenv("password")
port = os.getenv("port")

class Connect_BD:

    @staticmethod
    def crear_conexion():
        try:

            conexion = psycopg2.connect(
                host=host,
                database=database,
                user=user,
                password=password,
                port=port
            )
            print("¡Conexión a PostgreSQL exitosa!")
            cursor = conexion.cursor()
            cursor.execute("SELECT datname FROM pg_database WHERE datistemplate = false;")
            bases_de_datos = cursor.fetchall()
            return conexion

        except OperationalError as e:
            print(f"Ocurrió un error al conectar a la base de datos: {e}")
            return None

    @staticmethod
    def cerrar_conexion(conexion):
        if conexion is not None:
            conexion.close()
            print("La conexión a PostgreSQL ha sido cerrada.")



if __name__ == "__main__":
    conn = Connect_BD.crear_conexion()
    Connect_BD.cerrar_conexion(conn)