import pandas as pd
import logging
from abc import ABC, abstractmethod
from typing import List, Dict, Type, Optional
import sys
import glob
from pathlib import Path


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)


class BaseColumnCleaner(ABC):

    _registry: Dict[str, Type["BaseColumnCleaner"]] = {}

    def __init_subclass__(cls, data_types: List[str] = None, **kwargs):
        super().__init_subclass__(**kwargs)
        if data_types:
            for dt in data_types:
                BaseColumnCleaner._registry[dt.lower()] = cls

    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)

    @classmethod
    def get_cleaner(cls, data_type: str) -> "BaseColumnCleaner":
        dt_lower = data_type.lower()
        if dt_lower not in cls._registry:
            raise ValueError(f"No hay un limpiador registrado para el tipo de dato: '{data_type}'")
        clase_limpiadora = cls._registry[dt_lower]
        return clase_limpiadora()

    @abstractmethod
    def clean(self, df: pd.DataFrame, column_name: str) -> pd.DataFrame:
        ...

class TextCleaner(BaseColumnCleaner, data_types=["text", "string", "texto"]):
    def clean(self, df: pd.DataFrame, column_name: str) -> pd.DataFrame:
        if column_name in df.columns:
            df[column_name] = df[column_name].astype(str).str.strip().str.title()
            df[column_name] = df[column_name].replace(['nan', 'Nan', 'None', 'none', ''], 'Desconocido')
        return df

class CurrencyCleaner(BaseColumnCleaner, data_types=["currency", "money", "moneda", "float"]):
    def clean(self, df: pd.DataFrame, column_name: str) -> pd.DataFrame:
        if column_name in df.columns:
            df[column_name] = df[column_name].astype(str).replace({r'\$': '', r',': '', r'\s': ''}, regex=True)
            df[column_name] = pd.to_numeric(df[column_name], errors='coerce').fillna(0.0)
        return df

class QuantityCleaner(BaseColumnCleaner, data_types=["quantity", "integer", "int", "cantidad"]):
    def clean(self, df: pd.DataFrame, column_name: str) -> pd.DataFrame:
        if column_name in df.columns:
            df[column_name] = pd.to_numeric(df[column_name], errors='coerce').fillna(0)
            df[column_name] = df[column_name].abs().astype(int)
        return df

class EmailCleaner(BaseColumnCleaner, data_types=["email", "correo"]):
    def clean(self, df: pd.DataFrame, column_name: str) -> pd.DataFrame:
        if column_name in df.columns:
            df[column_name] = df[column_name].astype(str).str.strip().str.lower()
            df[column_name] = df[column_name].replace(['nan', 'none', ''], 'sin_correo@empresa.com')
        return df

class DatetimeCleaner(BaseColumnCleaner, data_types=["datetime", "timestamp", "fecha"]):
    def clean(self, df: pd.DataFrame, column_name: str) -> pd.DataFrame:
        if column_name in df.columns:
            df[column_name] = pd.to_datetime(df[column_name], errors='coerce')
        return df

class UUIDCleaner(BaseColumnCleaner, data_types=["uuid", "id"]):
    def clean(self, df: pd.DataFrame, column_name: str) -> pd.DataFrame:
        if column_name in df.columns:
            df[column_name] = df[column_name].astype(str).str.strip().str.lower()
            df[column_name] = df[column_name].replace(['nan', 'none', ''], None)
        return df

class BooleanCleaner(BaseColumnCleaner, data_types=["boolean", "bool"]):
    def clean(self, df: pd.DataFrame, column_name: str) -> pd.DataFrame:
        if column_name in df.columns:
            mapeo_bool = {
                'true': True, '1': True, '1.0': True, 't': True, 'yes': True, 'sí': True, 'si': True,
                'false': False, '0': False, '0.0': False, 'f': False, 'no': False
            }
            df[column_name] = df[column_name].astype(str).str.strip().str.lower().map(mapeo_bool)
            df[column_name] = df[column_name].fillna(False).astype(bool)
        return df

class DataCleaner:
    def __init__(
        self,
        schema: Dict[str, str],
        drop_duplicates: bool = True,
        dropna_all: bool = True,
        unique_keys: Optional[List[str]] = None
    ):
        self.schema = schema
        self.drop_duplicates = drop_duplicates
        self.dropna_all = dropna_all
        self.unique_keys = unique_keys
        self.logger = logging.getLogger(self.__class__.__name__)

    def process(self, df_crudo: pd.DataFrame) -> pd.DataFrame:
        if df_crudo.empty:
            self.logger.warning("El DataFrame recibido está vacío. Abortando limpieza.")
            return df_crudo

        self.logger.info("Iniciando limpieza general de datos...")
        df = df_crudo.copy()

        for col in df.columns:
            if df[col].apply(lambda x: isinstance(x, (dict, list))).any():
                self.logger.warning(f"Columna '{col}' contiene objetos anidados → se convierte a string.")
                df[col] = df[col].apply(lambda x: str(x) if isinstance(x, (dict, list)) else x)

        if self.dropna_all:
            if self.unique_keys:
                df.dropna(how='all', subset=self.unique_keys, inplace=True)
            else:
                df.dropna(how='all', inplace=True)

        if self.drop_duplicates:
            if self.unique_keys:
                df.drop_duplicates(subset=self.unique_keys, inplace=True)
            else:
                df.drop_duplicates(inplace=True)

        for columna, tipo_dato in self.schema.items():
            if columna not in df.columns:
                self.logger.warning(f"La columna '{columna}' no existe en el DataFrame. Se omitirá.")
                continue
            try:
                limpiador_especifico = BaseColumnCleaner.get_cleaner(tipo_dato)
                df = limpiador_especifico.clean(df, columna)
            except ValueError as e:
                self.logger.error(f"Error procesando la columna '{columna}': {e}")
            except Exception as e:
                self.logger.error(f"Fallo inesperado limpiando '{columna}' como '{tipo_dato}': {e}")

        self.logger.info(f"Limpieza finalizada. Filas listas: {len(df)}")
        return df

def cargar_archivo(ruta: str) -> pd.DataFrame:
    logger = logging.getLogger("cargar_archivo")
    ext = Path(ruta).suffix.lower()

    if ext == ".csv":
        return pd.read_csv(ruta)
    elif ext in [".xlsx", ".xls"]:
        return pd.read_excel(ruta)
    elif ext == ".json":
        return pd.read_json(ruta)
    else:
        logger.warning(f"Extensión '{ext}' no soportada para el archivo: {ruta}")
        return pd.DataFrame()

def guardar_archivo(df: pd.DataFrame, ruta_salida: str, ext_original: str):
    if ext_original in [".xlsx", ".xls"]:
        df.to_excel(ruta_salida, index=False)
    elif ext_original == ".json":
        df.to_json(ruta_salida, orient="records", force_ascii=False, indent=2)
    else:
        df.to_csv(ruta_salida, index=False, encoding="utf-8-sig")