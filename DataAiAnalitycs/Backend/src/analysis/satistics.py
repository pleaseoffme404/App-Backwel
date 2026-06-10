import json
import logging
import os
from pathlib import Path

import pandas as pd
from dotenv import load_dotenv
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError

load_dotenv(override=False)

host     = os.getenv("host")
database = os.getenv("database")
user     = os.getenv("user")
password = os.getenv("password")
port     = os.getenv("port")

DB_URI = f"postgresql://{user}:{password}@{host}:{port}/{database}"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger(__name__)

SEPARADOR = "=" * 60

def calcular_estadisticas_columna(serie: pd.Series, nombre_col: str) -> dict:

    serie = pd.to_numeric(serie, errors="coerce").dropna()

    if serie.empty:
        logger.warning("Columna [%s] — sin datos numéricos.", nombre_col.upper())
        return {}

    promedio  = serie.mean()
    mediana   = serie.median()
    moda_vals = serie.mode()
    moda      = float(moda_vals.iloc[0]) if not moda_vals.empty else None
    std       = serie.std()
    sesgo     = serie.skew()
    minimo    = serie.min()
    maximo    = serie.max()

    if pd.notna(sesgo):
        if sesgo > 1:
            interpretacion = "Cola larga hacia la derecha"
        elif sesgo < -1:
            interpretacion = "Cola larga hacia la izquierda"
        elif -0.5 <= sesgo <= 0.5:
            interpretacion = "Distribución simétrica"
        else:
            interpretacion = "Asimetría moderada"
    else:
        interpretacion = "Insuficientes datos"

    return {
        "columna":              nombre_col,
        "promedio":             round(float(promedio), 2)         if pd.notna(promedio) else None,
        "mediana":              round(float(mediana), 2)          if pd.notna(mediana)  else None,
        "moda":                 round(float(moda), 2)             if moda is not None   else None,
        "desv_estandar":        round(float(std), 2)              if pd.notna(std)      else None,
        "sesgo":                round(float(sesgo), 3)            if pd.notna(sesgo)    else None,
        "sesgo_interpretacion": interpretacion,
        "minimo":               round(float(minimo), 2)           if pd.notna(minimo)   else None,
        "maximo":               round(float(maximo), 2)           if pd.notna(maximo)   else None,
        "total_registros":      int(serie.count()),
    }


def _calcular_estadisticas_columna(serie: pd.Series, nombre_col: str):
    resultado = calcular_estadisticas_columna(serie, nombre_col)
    if not resultado:
        print(f"\n  Columna [{nombre_col.upper()}] — sin datos numéricos.")
        return

    print(f"\n  Columna: [{resultado['columna'].upper()}]")
    print(f"  • Promedio:        {resultado['promedio']:,.2f}" if resultado['promedio'] is not None else "  • Promedio:        N/A")
    print(f"  • Mediana:         {resultado['mediana']:,.2f}"  if resultado['mediana']  is not None else "  • Mediana:         N/A")
    print(f"  • Moda:            {resultado['moda']:,.2f}"     if resultado['moda']     is not None else "  • Moda:            N/A")
    print(f"  • Desv. Estándar:  {resultado['desv_estandar']:,.2f}" if resultado['desv_estandar'] is not None else "  • Desv. Estándar:  Insuficientes datos")
    print(f"  • Sesgo:           {resultado['sesgo']:,.3f} ({resultado['sesgo_interpretacion']})" if resultado['sesgo'] is not None else "  • Sesgo:           Insuficientes datos")
    print(f"  • Rango:           De {resultado['minimo']:,.2f} a {resultado['maximo']:,.2f}")


def _analizar_tendencia(serie: pd.Series) -> str:
    serie = pd.to_numeric(serie, errors="coerce").dropna()
    if len(serie) < 2:
        return "Insuficientes datos"
    delta = serie.iloc[-1] - serie.iloc[0]
    if delta > 0:
        return f"Creciente (+{delta:,.2f})"
    elif delta < 0:
        return f"Decreciente ({delta:,.2f})"
    return "Estable"


def calcular_estadisticas_dataframe(df: pd.DataFrame, nombre: str = "dataset") -> dict:

    if df is None or df.empty:
        logger.warning("DataFrame '%s' está vacío o es None.", nombre)
        return {"nombre": nombre, "total_registros": 0, "columnas": {}, "tendencia_ventas": None}

    resultado = {
        "nombre":           nombre,
        "total_registros":  len(df),
        "columnas":         {},
        "tendencia_ventas": None,
    }

    cols_numericas = df.select_dtypes(include="number").columns.tolist()

    for col in cols_numericas:
        stats = calcular_estadisticas_columna(df[col], col)
        if stats:
            resultado["columnas"][col] = stats

    col_tendencia = next(
        (c for c in ["fecha", "mes_nombre", "mes", "event_date"] if c in df.columns),
        None,
    )
    if col_tendencia and "total_amount" in df.columns:
        df_sorted = df.sort_values(col_tendencia)
        resultado["tendencia_ventas"] = _analizar_tendencia(df_sorted["total_amount"])

    logger.info("Estadísticas calculadas para '%s' (%d columnas numéricas).", nombre, len(cols_numericas))
    return resultado


def _imprimir_encabezado(nombre: str, total: int):
    print(f"\n{SEPARADOR}")
    print(f"  Reporte: {nombre.upper()}")
    print(f"  Total de registros analizados: {total:,}")
    print(SEPARADOR)


class Analysis:

    @staticmethod
    def generarEstadisticas(nombreTabla: str, db_uri: str):

        logger.info("Iniciando cálculo analítico para tabla: '%s'...", nombreTabla)

        try:
            engine = create_engine(db_uri)

            with engine.connect() as conexion:
                total = conexion.execute(
                    text(f'SELECT COUNT(*) FROM "{nombreTabla}"')
                ).scalar()

                if total == 0:
                    logger.warning("La tabla '%s' está vacía.", nombreTabla)
                    return

            _imprimir_encabezado(nombreTabla, total)

            query_schema = f"""
                SELECT column_name, data_type
                FROM information_schema.columns
                WHERE table_name = '{nombreTabla}'
            """
            cols_info = pd.read_sql(query_schema, engine)

            cols_numericas = []
            for _, row in cols_info.iterrows():
                tipo = row["data_type"].upper()
                if any(t in tipo for t in ["INT", "NUMERIC", "DECIMAL", "REAL", "DOUBLE"]):
                    cols_numericas.append(row["column_name"])

            if cols_numericas:
                print("\n  Métricas Numéricas y de Dispersión")
                for col in cols_numericas:
                    df_col = pd.read_sql(
                        f'SELECT "{col}" FROM "{nombreTabla}" WHERE "{col}" IS NOT NULL',
                        engine,
                    )
                    _calcular_estadisticas_columna(df_col[col], col)

            logger.info("Análisis finalizado para tabla '%s'.", nombreTabla)

        except SQLAlchemyError as e:
            logger.error("Error de base de datos: %s", e)
        except Exception as e:
            logger.error("Error inesperado: %s", e)

    @staticmethod
    def desde_analysis_results(analysis_type: str, db_uri: str):
        logger.info("Cargando '%s' desde analysis_results...", analysis_type)

        try:
            engine = create_engine(db_uri)

            query = text("""
                SELECT result, created_at
                FROM analysis_results
                WHERE analysis_type = :tipo
                ORDER BY created_at DESC
                LIMIT 1
            """)

            with engine.connect() as conn:
                row = conn.execute(query, {"tipo": analysis_type}).fetchone()

            if row is None:
                logger.warning(
                    "No se encontró '%s' en analysis_results. "
                    "¿Ya corriste transform_analysis?", analysis_type
                )
                return

            result_json = row[0]
            datos = result_json.get("datos", [])

            if not datos:
                logger.warning("El resultado de '%s' está vacío.", analysis_type)
                return

            df = pd.DataFrame(datos)
            Analysis.desde_dataframe(df, analysis_type)

        except SQLAlchemyError as e:
            logger.error("Error de base de datos: %s", e)
        except Exception as e:
            logger.error("Error inesperado: %s", e)

    @staticmethod
    def desde_dataframe(df: pd.DataFrame, nombre: str):
        if df.empty:
            logger.warning("DataFrame '%s' está vacío.", nombre)
            return

        _imprimir_encabezado(nombre, len(df))

        cols_numericas = df.select_dtypes(include="number").columns.tolist()

        if cols_numericas:
            print("\n  Métricas Numéricas y de Dispersión")
            for col in cols_numericas:
                _calcular_estadisticas_columna(df[col], col)

        col_tendencia = None
        for candidato in ["fecha", "mes_nombre", "mes", "event_date"]:
            if candidato in df.columns:
                col_tendencia = candidato
                break

        if col_tendencia and "total_amount" in df.columns:
            df_sorted = df.sort_values(col_tendencia)
            tendencia = _analizar_tendencia(df_sorted["total_amount"])
            print(f"\n  Tendencia de ventas (total_amount): {tendencia}")

        logger.info("Estadísticas calculadas para '%s'.", nombre)

    @staticmethod
    def analizar_todos(db_uri: str):
        try:
            engine = create_engine(db_uri)
            query  = text("""
                SELECT DISTINCT analysis_type
                FROM analysis_results
                WHERE model_name = 'transform_analysis'
                ORDER BY analysis_type
            """)

            with engine.connect() as conn:
                tipos = [row[0] for row in conn.execute(query).fetchall()]

            if not tipos:
                logger.warning(
                    "No hay resultados en analysis_results. "
                    "Ejecuta transform_analysis primero."
                )
                return

            logger.info("Tipos encontrados: %s", tipos)
            for tipo in tipos:
                Analysis.desde_analysis_results(tipo, db_uri)

        except Exception as e:
            logger.error("Error en analizar_todos: %s", e)
