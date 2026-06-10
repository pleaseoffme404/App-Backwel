import json
import logging
import uuid
from pathlib import Path
from typing import List, Optional

import pandas as pd

from Backend.src.ingestion.readers import BaseReader

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("JsonParser")


class JsonParser:

    def __init__(
            self,
            carpetas_datos: Optional[List[str]] = None,
            carpeta_salida: Optional[str] = None,
    ):
        self.carpetas_datos = carpetas_datos or ["../../../example"]

        if carpeta_salida:
            self.carpeta_salida = Path(carpeta_salida)
        else:
            self.carpeta_salida = Path("../../../data2")

        self._procesar_archivos()

    def _df_a_json(self, df: pd.DataFrame, archivo: str, id_archivo: str) -> str:
        if df is None or df.empty:
            logger.warning("DataFrame vacío recibido para: %s", archivo)
            return "{}"
        df = df.dropna(how="all")
        try:
            df = df.drop_duplicates()
        except TypeError:
            logger.warning("Se omitió drop_duplicates porque hay datos anidados (unhashable dict).")

        df = df.astype(object)
        df = df.where(pd.notna(df), None)
        df = df.replace(['NaN', 'nan', 'NaT', 'None', '<NA>'], None)

        resultado = {
            "id_archivo": id_archivo,
            "archivo": archivo,
            "total_registros": len(df),
            "datos": df.to_dict(orient="records"),
        }
        return json.dumps(resultado, ensure_ascii=False, indent=4, default=str)

    def _ruta_salida(self, archivo: str) -> Path:
        origen = Path(archivo)
        nuevo_nombre = f"{origen.stem}_{origen.suffix[1:]}.json"
        if self.carpeta_salida:
            self.carpeta_salida.mkdir(parents=True, exist_ok=True)
            return self.carpeta_salida / nuevo_nombre
        return origen.with_name(nuevo_nombre)

    def _procesar_archivos(self) -> None:
        archivos = BaseReader.find_files(self.carpetas_datos, recursive=True)

        if not archivos:
            logger.warning("No se encontraron archivos para procesar en las rutas indicadas.")
            return

        for n, archivo_actual in enumerate(archivos, start=1):
            id_archivo = str(uuid.uuid5(uuid.NAMESPACE_URL, archivo_actual))

            print(f"\n{'=' * 30}\nArchivo {n}: {archivo_actual}\nID: {id_archivo}")

            try:
                cargador = BaseReader.get_reader(archivo_actual)
                df = cargador.load_optimized(chunksize=1_000_000)

                df['id_archivo_origen'] = id_archivo

                salida_json = self._df_a_json(df, archivo_actual, id_archivo)

                destino = self._ruta_salida(archivo_actual)
                destino.write_text(salida_json, encoding="utf-8")
                logger.info("Guardado en: %s", destino)

            except Exception as e:
                logger.error("No se pudo procesar '%s'. Motivo: %s", archivo_actual, e)