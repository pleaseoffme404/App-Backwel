import os
import uuid
import random
from datetime import datetime, timedelta
from typing import Final
import numpy as np
import pandas as pd
from faker import Faker

SEMILLA: Final[int] = random.randint(0, 999_999)
random.seed(SEMILLA)
np.random.seed(SEMILLA)

fake = Faker("es_MX")
Faker.seed(SEMILLA)

CARPETA_SALIDA: Final[str] = "datasets_simulados_masivos"

FILAS: Final[dict[str, int]] = {
    "catalogo":        2_000,
    "clientes":        5_00,
    "transacciones":   30_000,
    "data_sources":    50,
    "data_ingestions": 200,
    "audit_logs":      10_000,
}

AHORA:       pd.Timestamp = pd.Timestamp.utcnow()
HACE_1_ANO:  pd.Timestamp = AHORA - pd.DateOffset(years=1)
HACE_3_ANOS: pd.Timestamp = AHORA - pd.DateOffset(years=3)

TS: Final[str] = AHORA.strftime("%Y%m%d_%H%M%S")

CATEGORIAS: Final[list[str]] = [
    "Lácteos", "Carnes", "Frutas", "Verduras", "Panadería",
    "Bebidas", "Limpieza", "Higiene Personal", "Congelados", "Snacks",
    "Electrónica", "Mascotas", "Cereales", "Enlatados", "Licores",
    "Parafarmacia", "Jardinería", "Ropa y Textil", "Papelería", "Juguetería",
]
CATEGORIAS_PRECIOS: Final[dict[str, tuple[float, float]]] = {
    "Lácteos":         (10.0,   120.0),
    "Carnes":          (50.0,   400.0),
    "Frutas":          (8.0,    80.0),
    "Verduras":        (5.0,    60.0),
    "Panadería":       (12.0,   90.0),
    "Bebidas":         (15.0,   200.0),
    "Limpieza":        (20.0,   250.0),
    "Higiene Personal":(18.0,   300.0),
    "Congelados":      (30.0,   220.0),
    "Snacks":          (10.0,   100.0),
    "Electrónica":     (200.0, 5000.0),
    "Mascotas":        (25.0,   350.0),
    "Cereales":        (20.0,   150.0),
    "Enlatados":       (15.0,   120.0),
    "Licores":         (80.0,  1500.0),
    "Parafarmacia":    (30.0,   500.0),
    "Jardinería":      (40.0,   800.0),
    "Ropa y Textil":   (100.0,  900.0),
    "Papelería":       (10.0,   200.0),
    "Juguetería":      (50.0,   700.0),
}
REGIONES:    Final[list[str]] = ["CDMX", "GDL", "MTY", "PUE", "MER", "TIJ", "CUN", "VER"]
DISPOSITIVOS:Final[list[str]] = ["desktop", "mobile", "tablet", "app_ios", "app_android"]
FUENTES:     Final[list[str]] = ["web", "app_movil", "marketplace", "crm", "api_externa", "pos"]
TIPOS_EVENTO:Final[list[str]] = ["Compra", "Factura", "Devolución"]
PESOS_TIPO:  Final[list[float]] = [0.60, 0.25, 0.15]
ESTADOS_TX:  Final[list[str]] = ["Emitida", "Pendiente", "Cancelada"]
PESOS_ESTADO:Final[list[float]] = [0.70, 0.20, 0.10]
MARCAS:      Final[list[str]] = [
    "Sony", "Samsung", "Nike", "Adidas", "Apple",
    "Dell", "LG", "Genérico", "Nestlé", "Bimbo",
]
SEGMENTOS:   Final[list[str]] = ["bronce", "plata", "oro", "platino"]
PAISES:      Final[list[str]] = ["México", "Colombia", "Argentina", "Chile"]
TIPOS_FUENTE:Final[list[str]] = ["csv", "json", "xml", "txt", "api"]
EST_INGESTA: Final[list[str]] = ["pending", "processing", "completed", "failed"]
PES_INGESTA: Final[list[float]] = [0.05, 0.05, 0.85, 0.05]
LOG_LEVELS:  Final[list[str]] = ["INFO", "WARNING", "ERROR"]
PESOS_LOG:   Final[list[float]] = [0.75, 0.15, 0.10]
PROCESOS:    Final[list[str]] = [
    "pipeline_raw", "anomaly_detection", "transform_analysis", "kpis_engine",
]

def uid_batch(n: int) -> list[str]:
    return [str(uuid.uuid4()) for _ in range(n)]


def fechas_batch(inicio: pd.Timestamp, fin: pd.Timestamp, n: int) -> np.ndarray:
    start_u: int = int(inicio.value // 10**9)
    end_u:   int = int(fin.value   // 10**9)
    ts = pd.to_datetime(
        np.random.randint(start_u, end_u, n), unit="s", utc=True
    )
    return ts.astype(str)

def _df_a_xml(df: pd.DataFrame, ruta: str, root: str, row: str) -> None:
    try:
        df.to_xml(
            path_or_buffer=ruta,
            index=False,
            root_name=root,
            row_name=row,
            encoding="utf-8",
            xml_declaration=True,
        )
    except Exception:
        import xml.etree.ElementTree as ET
        from xml.dom import minidom

        raiz = ET.Element(root)
        for _, fila in df.iterrows():
            nodo = ET.SubElement(raiz, row)
            for col, val in fila.items():
                hijo = ET.SubElement(nodo, str(col))
                hijo.text = str(val)

        xml_raw: str = ET.tostring(raiz, encoding="unicode")
        pretty:  str = minidom.parseString(xml_raw).toprettyxml(indent="  ")
        lineas        = [l for l in pretty.splitlines() if not l.startswith("<?xml")]
        with open(ruta, "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n')
            f.write("\n".join(lineas))


def exportar_todos_formatos(
    df:     pd.DataFrame,
    nombre: str,
    root_xml: str = "dataset",
    row_xml:  str = "record",
) -> list[tuple[str, str]]:

    os.makedirs(CARPETA_SALIDA, exist_ok=True)
    base: str = os.path.join(CARPETA_SALIDA, f"{nombre}_{TS}")
    generados: list[tuple[str, str]] = []

    ruta_csv = f"{base}.csv"
    df.to_csv(ruta_csv, index=False, encoding="utf-8-sig")
    generados.append((ruta_csv, "csv"))

    ruta_json = f"{base}.json"
    df.to_json(ruta_json, orient="records", force_ascii=False, indent=2)
    generados.append((ruta_json, "json"))

    ruta_xml = f"{base}.xml"
    _df_a_xml(df, ruta_xml, root=root_xml, row=row_xml)
    generados.append((ruta_xml, "xml"))

    ruta_txt = f"{base}.txt"
    df.to_csv(ruta_txt, index=False, sep="\t", encoding="utf-8-sig")
    generados.append((ruta_txt, "txt"))

    return generados


def gen_catalogo() -> pd.DataFrame:
    total: int = FILAS["catalogo"]
    cats  = list(CATEGORIAS_PRECIOS.keys())
    base_n, resto = divmod(total, len(cats))
    contadores: dict[str, int] = {
        c: base_n + (1 if i < resto else 0)
        for i, c in enumerate(cats)
    }

    filas: list[dict] = []
    for cat, qty in contadores.items():
        pmin, pmax = CATEGORIAS_PRECIOS[cat]
        for idx in range(1, qty + 1):
            filas.append({
                "product_id": str(uuid.uuid4()),
                "name":       f"{cat.replace(' ', '_')}_{idx}",
                "category":   cat,
                "base_price": round(random.uniform(pmin, pmax), 2),
                "marca":      random.choice(MARCAS),
                "sku":        f"SKU-{random.randint(1000,9999)}-{idx}",
                "peso_kg":    round(random.uniform(0.1, 30.0), 2),
                "activo":     random.choices([True, False], weights=[92, 8])[0],
                "stock":      random.randint(0, 500),
                "created_at": str(
                    pd.Timestamp(
                        HACE_3_ANOS.value // 10**9
                        + random.randint(
                            0,
                            int((HACE_1_ANO - HACE_3_ANOS).total_seconds())
                        ),
                        unit="s",
                        tz="UTC",
                    )
                ),
            })

    return pd.DataFrame(filas)

def gen_clientes(df_catalogo: pd.DataFrame) -> pd.DataFrame:
    n: int = FILAS["clientes"]
    nombres   = [fake.first_name() for _ in range(min(n, 1000))]
    apellidos = [fake.last_name()  for _ in range(min(n, 1000))]

    nom_arr = np.random.choice(nombres,   n)
    ape_arr = np.random.choice(apellidos, n)

    df = pd.DataFrame({
        "user_id":     uid_batch(n),
        "customer_id": uid_batch(n),
        "nombre":      nom_arr,
        "apellido":    ape_arr,
        "region":      np.random.choice(REGIONES, n),
        "genero":      np.random.choice(["M", "F", "otro"], n, p=[0.48, 0.48, 0.04]),
        "pais":        np.random.choice(PAISES, n),
        "segmento":    np.random.choice(SEGMENTOS, n, p=[0.50, 0.30, 0.15, 0.05]),
        "activo":      np.random.choice([True, False], n, p=[0.92, 0.08]),
        "created_at":  fechas_batch(HACE_3_ANOS, HACE_1_ANO, n),
    })
    df["email"] = (
        df["nombre"].str.lower() + "."
        + df["apellido"].str.lower()
        + np.random.randint(100, 9999, n).astype(str)
        + "@ejemplo.com"
    )
    df["telefono"] = (
        np.random.randint(100, 1000, n).astype(str)
        + np.random.randint(1_000_000, 10_000_000, n).astype(str)
    )
    return df

def gen_transacciones(
    df_catalogo: pd.DataFrame,
    df_clientes:  pd.DataFrame,
) -> pd.DataFrame:
    n: int = FILAS["transacciones"]

    prod_idx  = np.random.randint(0, len(df_catalogo), n)
    cli_idx   = np.random.randint(0, len(df_clientes), n)

    product_ids = df_catalogo["product_id"].values[prod_idx]
    categories  = df_catalogo["category"].values[prod_idx]
    base_prices = df_catalogo["base_price"].values[prod_idx].astype(float)
    user_ids    = df_clientes["user_id"].values[cli_idx]
    customer_ids= df_clientes["customer_id"].values[cli_idx]

    tipos      = np.random.choice(TIPOS_EVENTO, n, p=PESOS_TIPO)
    estados    = np.random.choice(ESTADOS_TX,   n, p=PESOS_ESTADO)
    quantities = np.random.randint(1, 6, n)
    descuentos = np.round(np.random.uniform(0.0, 0.25, n), 4)

    amounts_normal     = np.round(base_prices * quantities * (1 - descuentos), 2)
    amounts_devolucion = np.round(-base_prices * np.random.randint(1, 4, n), 2)
    amounts = np.where(tipos == "Devolución", amounts_devolucion, amounts_normal)

    riesgo_alto = (tipos == "Devolución") | (estados == "Cancelada")
    scores = np.where(
        riesgo_alto,
        np.round(np.random.uniform(0.40, 0.99, n), 4),
        np.round(np.random.uniform(0.01, 0.55, n), 4),
    )

    df = pd.DataFrame({
        "record_id":    uid_batch(n),
        "user_id":      user_ids,
        "customer_id":  customer_ids,
        "product_id":   product_ids,
        "category":     categories,
        "type":         tipos,
        "amount":       amounts,
        "quantity":     quantities,
        "score":        scores,
        "base_price":   np.round(base_prices, 2),
        "descuento_pct":descuentos,
        "moneda":       "MXN",
        "source":       np.random.choice(FUENTES,      n),
        "region":       np.random.choice(REGIONES,     n),
        "device":       np.random.choice(DISPOSITIVOS, n),
        "status":       estados,
        "numero_orden": [f"ORD-{random.randint(100_000, 999_999)}" for _ in range(n)],
        "event_date":   fechas_batch(HACE_1_ANO, AHORA, n),
    })
    df.sort_values("event_date", inplace=True)
    df.reset_index(drop=True, inplace=True)
    return df

def gen_data_sources() -> pd.DataFrame:
    n: int = FILAS["data_sources"]
    df = pd.DataFrame({
        "source_id":   uid_batch(n),
        "source_type": np.random.choice(TIPOS_FUENTE, n),
        "region":      np.random.choice(REGIONES, n),
        "activo":      np.random.choice([True, False], n, p=[0.90, 0.10]),
        "created_at":  fechas_batch(HACE_3_ANOS, HACE_1_ANO, n),
    })
    df["file_name"] = "fuente_" + df.index.astype(str) + "." + df["source_type"]
    return df

def gen_data_ingestions(df_sources: pd.DataFrame) -> pd.DataFrame:
    n: int = FILAS["data_ingestions"]
    source_ids = df_sources["source_id"].values[
        np.random.randint(0, len(df_sources), n)
    ]
    return pd.DataFrame({
        "batch_id":      uid_batch(n),
        "source_id":     source_ids,
        "ingestion_date":fechas_batch(HACE_1_ANO, AHORA, n),
        "file_name":     [f"upload_{random.randint(1000, 9999)}.csv" for _ in range(n)],
        "record_count":  np.random.randint(100, 50_000, n),
        "status":        np.random.choice(EST_INGESTA, n, p=PES_INGESTA),
        "created_at":    fechas_batch(HACE_1_ANO, AHORA, n),
    })

def gen_audit_logs(df_ingestions: pd.DataFrame) -> pd.DataFrame:
    n: int = FILAS["audit_logs"]
    batch_ids = df_ingestions["batch_id"].values[
        np.random.randint(0, len(df_ingestions), n)
    ]
    return pd.DataFrame({
        "log_id":        uid_batch(n),
        "process_name":  np.random.choice(PROCESOS,    n),
        "log_level":     np.random.choice(LOG_LEVELS,  n, p=PESOS_LOG),
        "batch_id":      batch_ids,
        "execution_time":[f"{random.randint(50, 15_000)} ms" for _ in range(n)],
        "created_at":    fechas_batch(HACE_1_ANO, AHORA, n),
    })

def main() -> None:
    SEP = "=" * 70

    print(SEP)
    print("  GENERADOR MASIVO DE DATOS — SUPERMERCADO PIPELINE")
    print(f"  Carpeta de salida : {CARPETA_SALIDA}/")
    print(f"  Timestamp         : {TS}")
    print(f"  Semilla utilizada : {SEMILLA}  ← guárdala para reproducir este dataset")
    print(SEP)

    print("\n⚙  Generando tablas en memoria …\n")

    print("  [1/6] Catálogo de productos …")
    df_catalogo     = gen_catalogo()

    print("  [2/6] Pool de clientes …")
    df_clientes     = gen_clientes(df_catalogo)

    print("  [3/6] Transacciones …")
    df_transac      = gen_transacciones(df_catalogo, df_clientes)

    print("  [4/6] Data Sources …")
    df_sources      = gen_data_sources()

    print("  [5/6] Data Ingestions …")
    df_ingestions   = gen_data_ingestions(df_sources)

    print("  [6/6] Audit Logs …")
    df_logs         = gen_audit_logs(df_ingestions)

    print(f"\n Exportando a {CARPETA_SALIDA}/ en CSV · JSON · XML · TXT …\n")

    tablas: list[tuple[str, pd.DataFrame, str, str]] = [
        ("catalogo",        df_catalogo,   "catalogo",      "producto"),
        ("clientes",        df_clientes,   "clientes",      "cliente"),
        ("transacciones",   df_transac,    "transacciones", "transaccion"),
        ("data_sources",    df_sources,    "data_sources",  "source"),
        ("data_ingestions", df_ingestions, "data_ingestions","ingestion"),
        ("audit_logs",      df_logs,       "audit_logs",    "log"),
    ]

    resumen: list[tuple[str, int, list[str]]] = []

    for nombre, df, root, row in tablas:
        archivos = exportar_todos_formatos(df, nombre, root_xml=root, row_xml=row)
        fmts     = [fmt for _, fmt in archivos]
        resumen.append((nombre, len(df), fmts))
        fmts_str = " · ".join(f.upper() for f in fmts)
        print(f"  ✔ {nombre:<18} {len(df):>8,} filas  [{fmts_str}]")

    total_filas    = sum(f for _, f, _ in resumen)
    total_archivos = len(tablas) * 4

    print(f"\n{SEP}")
    print("  RESUMEN")
    print(SEP)
    print(f"  Tablas generadas    : {len(tablas)}")
    print(f"  Archivos exportados : {total_archivos}  (4 formatos × {len(tablas)} tablas)")
    print(f"  Total filas creadas : {total_filas:,}")
    print(f"  Semilla utilizada   : {SEMILLA}")
    print(SEP)

    print("\nVista previa — transacciones (primeras 5 filas):")
    print(df_transac[["record_id","category","type","amount","quantity","status","event_date"]]
          .head()
          .to_string(index=False))

    print("\nDistribución por tipo de evento:")
    print(df_transac["type"].value_counts().to_string())

    print("\nDistribución por status:")
    print(df_transac["status"].value_counts().to_string())

    print("\nResumen estadístico de 'amount':")
    print(df_transac["amount"].describe().round(2).to_string())


if __name__ == "__main__":
    main()