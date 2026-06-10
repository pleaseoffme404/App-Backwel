from Backend.data.db_connector import Connect_BD

class DB_Inspector:
    def __init__(self, connection):
        self.connection = connection
        self.cursor = connection.cursor()

    def obtener_tablas(self):
        self.cursor.execute("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
            AND table_type = 'BASE TABLE'
            ORDER BY table_name;
        """)
        return [row[0] for row in self.cursor.fetchall()]

    def obtener_columnas(self, tabla):
        self.cursor.execute("""
            SELECT 
                column_name,
                data_type,
                character_maximum_length,
                is_nullable,
                column_default
            FROM information_schema.columns
            WHERE table_schema = 'public'
            AND table_name = %s
            ORDER BY ordinal_position;
        """, (tabla,))
        columnas = []
        for row in self.cursor.fetchall():
            columnas.append({
                "nombre":   row[0],
                "tipo":     row[1],
                "longitud": row[2],
                "nullable": row[3],
                "default":  row[4],
            })
        return columnas

    def describir_bd(self):
        tablas = self.obtener_tablas()

        if not tablas:
            print("⚠️  No se encontraron tablas en la base de datos.")
            return

        print(f"\n📦 Base de datos: {self._nombre_bd()}")
        print(f"📋 Total de tablas: {len(tablas)}\n")
        print("=" * 60)

        for tabla in tablas:
            columnas = self.obtener_columnas(tabla)
            print(f"\n🗂️  TABLA: {tabla}  ({len(columnas)} columnas)")
            print("-" * 60)

            for col in columnas:
                tipo = col["tipo"]
                if col["longitud"]:
                    tipo += f"({col['longitud']})"

                flags = []
                if col["nullable"] == "NO":
                    flags.append("NOT NULL")
                if col["default"] is not None:
                    flags.append(f"DEFAULT={col['default']}")

                extras = "  " + " | ".join(flags) if flags else ""
                print(f"  • {col['nombre']:<25} {tipo:<20}{extras}")

        print("\n" + "=" * 60)
        print("✅ Descripción completada.\n")

    def _nombre_bd(self):
        self.cursor.execute("SELECT current_database();")  # ← PostgreSQL
        result = self.cursor.fetchone()
        return result[0] if result else "desconocida"

    def cerrar(self):
        self.cursor.close()


if __name__ == "__main__":
    connect = Connect_BD.crear_conexion()

    if connect is None:
        print("❌ No se pudo establecer conexión.")
    else:
        inspector = DB_Inspector(connect)
        inspector.describir_bd()
        inspector.cerrar()
        Connect_BD.cerrar_conexion(connect)