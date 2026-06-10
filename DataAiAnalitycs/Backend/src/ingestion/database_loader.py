import sqlite3
import pandas as pd
from pathlib import Path
import logging

logger = logging.getLogger(__name__)
class db_loader:


        def guardarDB(df: pd.DataFrame, nombreTabla: str = "inventario_limpio") -> bool:
            if df.empty:
                logger.warning("No hay datos para guardar en la base de datos.")
                return False

            logger.info("Iniciando carga en base de datos...")

            rutaScript = Path(__file__).resolve()
            rutaCarpetaData = rutaScript.parent.parent.parent / 'data'
            rutaDb = rutaCarpetaData / 'dbPrueba.db'
            rutaCarpetaData.mkdir(parents=True, exist_ok=True)

            try:
                conexion = sqlite3.connect(str(rutaDb))
                cursor = conexion.cursor()

                cursor.execute(
                    f'''CREATE TABLE IF NOT EXISTS {nombreTabla} ( nombre TEXT PRIMARY KEY,precio REAL, stock INTEGER,proveedor TEXT )''')

                tablaTemp = f"{nombreTabla}_temp"
                df.to_sql(tablaTemp, conexion, if_exists='replace', index=False)

                cursor.execute(f'''
                    INSERT OR REPLACE INTO {nombreTabla} (nombre, precio, stock, proveedor)
                    SELECT nombre, precio, stock, proveedor FROM {tablaTemp}
                ''')

                cursor.execute(f"DROP TABLE {tablaTemp}")

                conexion.commit()
                conexion.close()

                logger.info(f"Carga exitosa, datos fusionados en '{nombreTabla}'.")
                return True

            except Exception as e:
                logger.error(f"Error al intentar guardar en la base de datos: {e}")
                return False