import pandas as pd
import os
import glob
import logging
from sqlalchemy import create_engine
from sqlalchemy.exc import SQLAlchemyError
from abc import ABC, abstractmethod
from typing import List, Union, Dict, Type, Optional

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)

class BaseReader(ABC):

    _registry: Dict[str, Type["BaseReader"]] = {}

    def __init_subclass__(cls, extensions: List[str] = None, **kwargs):
        super().__init_subclass__(**kwargs)
        if extensions:
            for ext in extensions:
                BaseReader._registry[ext.lower()] = cls

    def __init__(self, file_path: str, encoding: str = "utf-8"):
        self.file_path = file_path
        self.encoding = encoding
        self.logger = logging.getLogger(self.__class__.__name__)

        if not os.path.exists(self.file_path):
            raise FileNotFoundError(f"No se encontró el archivo en: {self.file_path}")

    @classmethod
    def find_files(
        cls,
        carpetas: Union[str, List[str]],
        recursive: bool = False,
    ) -> List[str]:

        if isinstance(carpetas, str):
            carpetas = [carpetas]

        extensiones_soportadas = cls._registry.keys()
        archivos_encontrados = []
        logger = logging.getLogger("BaseReader.find_files")

        for carpeta in carpetas:
            if not os.path.isdir(carpeta):
                logger.warning("La carpeta '%s' no existe o no es un directorio.", carpeta)
                continue

            for ext in extensiones_soportadas:
                patron = f"**/*.{ext}" if recursive else f"*.{ext}"
                ruta_busqueda = os.path.join(carpeta, patron)
                archivos_encontrados.extend(glob.glob(ruta_busqueda, recursive=recursive))

        logger.info("Total de archivos encontrados: %d", len(archivos_encontrados))
        return archivos_encontrados

    @classmethod
    def get_reader(cls, file_path: str, encoding: str = "utf-8") -> "BaseReader":
        ext = file_path.rsplit(".", 1)[-1].lower()
        if ext not in cls._registry:
            raise ValueError(f"No hay un lector registrado para la extensión: .{ext}")
        clase_lectora = cls._registry[ext]
        return clase_lectora(file_path, encoding=encoding)

    def _filtrar_columnas(self,df: pd.DataFrame,required_columns: Optional[List[str]],) -> pd.DataFrame:
        if not required_columns:
            return df

        faltantes = set(required_columns) - set(df.columns)
        if faltantes:
            self.logger.warning("Columnas no encontradas en '%s': %s", self.file_path, faltantes)
        columnas_existentes = [c for c in required_columns if c in df.columns]
        return df[columnas_existentes]

    @abstractmethod
    def load_data(self, required_columns: Optional[List[str]] = None) -> pd.DataFrame:
        ...

    def load_optimized(self,required_columns: Optional[List[str]] = None,chunksize: Optional[int] = None,) -> pd.DataFrame:
        if chunksize:
            if hasattr(self, "_load_chunked"):
                return self._load_chunked(required_columns, chunksize)
            else:
                self.logger.warning("%s no soporta lectura por chunks. Leyendo completo.",self.__class__.__name__,)
        return self.load_data(required_columns)

class CSVReader(BaseReader, extensions=["csv", "xlsx", "xls"]):

    def load_data(self, required_columns: Optional[List[str]] = None) -> pd.DataFrame:
        extension = self.file_path.rsplit(".", 1)[-1].lower()
        self.logger.info("Leyendo %s: %s", extension.upper(), self.file_path)

        try:
            if extension == "csv":
                df = self._read_csv()
            else:
                df = pd.read_excel(self.file_path)

            return self._filtrar_columnas(df, required_columns)

        except Exception as e:
            self.logger.error("Error leyendo %s: %s", extension.upper(), e)
            return pd.DataFrame(columns=required_columns or [])

    def _read_csv(self) -> pd.DataFrame:
        try:
            return pd.read_csv(self.file_path, encoding=self.encoding)
        except UnicodeDecodeError:
            self.logger.warning(
                "Encoding '%s' falló en '%s'. Reintentando con 'latin-1'.",
                self.encoding,
                self.file_path,
            )
            return pd.read_csv(self.file_path, encoding="latin-1")

    def _load_chunked(
        self, required_columns: Optional[List[str]], chunksize: int
    ) -> pd.DataFrame:
        extension = self.file_path.rsplit(".", 1)[-1].lower()
        if extension != "csv":
            self.logger.warning(
                "Lectura por chunks solo disponible para CSV. Usando load_data normal."
            )
            return self.load_data(required_columns)

        self.logger.info("Leyendo CSV en chunks de %d filas: %s", chunksize, self.file_path)
        try:
            chunks = pd.read_csv(
                self.file_path,
                encoding=self.encoding,
                chunksize=chunksize,
            )
            df = pd.concat(chunks, ignore_index=True)
            return self._filtrar_columnas(df, required_columns)
        except UnicodeDecodeError:
            self.logger.warning("Fallback a latin-1 durante lectura por chunks.")
            chunks = pd.read_csv(
                self.file_path,
                encoding="latin-1",
                chunksize=chunksize,
            )
            df = pd.concat(chunks, ignore_index=True)
            return self._filtrar_columnas(df, required_columns)
        except Exception as e:
            self.logger.error("Error en lectura por chunks: %s", e)
            return pd.DataFrame(columns=required_columns or [])

    def load_sheet(self,sheet_name: Union[str, int] = 0,required_columns: Optional[List[str]] = None,) -> pd.DataFrame:
        extension = self.file_path.rsplit(".", 1)[-1].lower()
        if extension not in ("xlsx", "xls"):
            raise ValueError("load_sheet solo está disponible para archivos Excel.")

        self.logger.info("Leyendo hoja '%s' de: %s", sheet_name, self.file_path)
        try:
            df = pd.read_excel(self.file_path, sheet_name=sheet_name)  # sin usecols
            return self._filtrar_columnas(df, required_columns)
        except Exception as e:
            self.logger.error("Error leyendo hoja '%s': %s", sheet_name, e)
            return pd.DataFrame(columns=required_columns or [])

class JsonReader(BaseReader, extensions=["json"]):

    def load_data(self, required_columns: Optional[List[str]] = None) -> pd.DataFrame:
        self.logger.info("Leyendo JSON: %s", self.file_path)
        try:
            df = pd.read_json(self.file_path, encoding=self.encoding)
            return self._filtrar_columnas(df, required_columns)
        except UnicodeDecodeError:
            self.logger.warning("Encoding '%s' falló en JSON. Reintentando con 'latin-1'.", self.encoding)
            try:
                df = pd.read_json(self.file_path, encoding="latin-1")
                return self._filtrar_columnas(df, required_columns)
            except Exception as e:
                self.logger.error("Error leyendo JSON con latin-1: %s", e)
                return pd.DataFrame(columns=required_columns or [])
        except Exception as e:
            self.logger.error("Error leyendo JSON: %s", e)
            return pd.DataFrame(columns=required_columns or [])

class TxtReader(BaseReader, extensions=["txt"]):

    def __init__(self,file_path: str,encoding: str = "utf-8",sep: Optional[str] = None,):
        super().__init__(file_path, encoding)
        self.sep = sep

    def load_data(self, required_columns: Optional[List[str]] = None) -> pd.DataFrame:
        self.logger.info("Leyendo TXT: %s", self.file_path)
        try:
            df = self._read_txt()
            return self._filtrar_columnas(df, required_columns)
        except pd.errors.EmptyDataError:
            self.logger.warning("El archivo está vacío: %s", self.file_path)
            return pd.DataFrame(columns=required_columns or [])
        except Exception as e:
            self.logger.error("Error leyendo TXT: %s", e)
            return pd.DataFrame(columns=required_columns or [])

    def _read_txt(self) -> pd.DataFrame:
        kwargs = dict(
            sep=self.sep,
            engine="python" if self.sep is None else "c",
            encoding=self.encoding,
        )
        try:
            return pd.read_csv(self.file_path, **kwargs)
        except UnicodeDecodeError:
            self.logger.warning("Fallback a latin-1 en TXT.")
            kwargs["encoding"] = "latin-1"
            return pd.read_csv(self.file_path, **kwargs)

    def _load_chunked(
        self, required_columns: Optional[List[str]], chunksize: int
    ) -> pd.DataFrame:
        self.logger.info("Leyendo TXT en chunks de %d filas.", chunksize)
        try:
            chunks = pd.read_csv(
                self.file_path,
                sep=self.sep,
                engine="python" if self.sep is None else "c",
                encoding=self.encoding,  # sin usecols
                chunksize=chunksize,
            )
            df = pd.concat(chunks, ignore_index=True)
            return self._filtrar_columnas(df, required_columns)
        except Exception as e:
            self.logger.error("Error en chunks TXT: %s", e)
            return pd.DataFrame(columns=required_columns or [])

class XmlReader(BaseReader, extensions=["xml"]):

    def load_data(self, required_columns: Optional[List[str]] = None) -> pd.DataFrame:
        self.logger.info("Leyendo XML: %s", self.file_path)
        try:
            try:
                df = pd.read_xml(self.file_path, encoding=self.encoding)
            except Exception:
                df = pd.read_xml(self.file_path, encoding=self.encoding, parser="etree")
            return self._filtrar_columnas(df, required_columns)
        except Exception as e:
            self.logger.error("Error leyendo XML: %s", e)
            return pd.DataFrame(columns=required_columns or [])

class CompressedReader(BaseReader, extensions=["gz", "zip", "bz2"]):
    def load_data(self, required_columns: Optional[List[str]] = None) -> pd.DataFrame:
        self.logger.info("Leyendo archivo comprimido: %s", self.file_path)
        try:
            df = pd.read_csv(
                self.file_path,
                encoding=self.encoding,
                compression="infer",
            )
            return self._filtrar_columnas(df, required_columns)
        except UnicodeDecodeError:
            self.logger.warning("Fallback a latin-1 en archivo comprimido.")
            try:
                df = pd.read_csv(
                    self.file_path,
                    encoding="latin-1",
                    compression="infer",
                )
                return self._filtrar_columnas(df, required_columns)
            except Exception as e:
                self.logger.error("Error con latin-1 en comprimido: %s", e)
                return pd.DataFrame(columns=required_columns or [])
        except Exception as e:
            self.logger.error("Error leyendo archivo comprimido: %s", e)
            return pd.DataFrame(columns=required_columns or [])

    def _load_chunked(
        self, required_columns: Optional[List[str]], chunksize: int
    ) -> pd.DataFrame:
        self.logger.info("Leyendo comprimido en chunks de %d filas.", chunksize)
        try:
            chunks = pd.read_csv(
                self.file_path,
                encoding=self.encoding,
                compression="infer",
                chunksize=chunksize,
            )
            df = pd.concat(chunks, ignore_index=True)
            return self._filtrar_columnas(df, required_columns)
        except Exception as e:
            self.logger.error("Error en chunks comprimido: %s", e)
            return pd.DataFrame(columns=required_columns or [])

class PostgresReader:
    def __init__(self, db_uri: str):
 
        self.db_uri = db_uri
        self.logger = logging.getLogger(self.__class__.__name__)
        try:
            self.engine = create_engine(self.db_uri)
            self.logger.info("Conexión a PostgreSQL preparada.")
        except Exception as e:
            self.logger.error("Error al preparar el motor de BD: %s", e)
            raise

    def load_table(self, table_name: str, required_columns: Optional[List[str]] = None) -> pd.DataFrame:
        self.logger.info("Leyendo tabla PostgreSQL: %s", table_name)
        try:
            if required_columns:
                cols = ", ".join([f'"{col}"' for col in required_columns])
                query = f"SELECT {cols} FROM {table_name}"
            else:
                query = f"SELECT * FROM {table_name}"

            df = pd.read_sql(query, self.engine)
            return df
        except SQLAlchemyError as e:
            self.logger.error("Error leyendo la tabla %s: %s", table_name, e)
            return pd.DataFrame(columns=required_columns or [])

    def load_query(self, query: str) -> pd.DataFrame:
        self.logger.info("Ejecutando query personalizado...")
        try:
            return pd.read_sql(query, self.engine)
        except SQLAlchemyError as e:
            self.logger.error("Error ejecutando query: %s", e)
            return pd.DataFrame()

def leer()->None:
    logger = logging.getLogger("main")

    carpetas_datos = ["../../../example"]
    archivos_procesar = BaseReader.find_files(carpetas_datos, recursive=True)

    if not archivos_procesar:
        logger.warning("No se encontraron archivos procesables en las carpetas.")
    else:
        logger.info("Se encontraron %d archivos. Iniciando...", len(archivos_procesar))

        for archivo_actual in archivos_procesar:
            logger.info("--- Procesando: %s ---", archivo_actual)
            try:
                cargador = BaseReader.get_reader(archivo_actual)

                df_limpio = cargador.load_optimized(chunksize=5000)

                if df_limpio is not None and not df_limpio.empty:
                    print(df_limpio.head())
                else:
                    logger.warning("El DataFrame regresó vacío: %s", archivo_actual)

            except Exception as e:
                logger.error("No se pudo procesar '%s'. Motivo: %s", archivo_actual, e)
