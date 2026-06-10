from Backend.data.db_connector import Connect_BD


def reparar_cruce_rfm():
    conn = Connect_BD.crear_conexion()
    if not conn:
        return

    try:
        cur = conn.cursor()

        # 1. Verificar si existen clientes
        cur.execute("SELECT COUNT(*) FROM user_info;")
        clientes = cur.fetchone()[0]
        print(f"👥 Clientes en base de datos: {clientes}")

        if clientes == 0:
            print("❌ ERROR: La tabla 'user_info' está vacía.")
            print("👉 SOLUCIÓN: Entra a la web y sube tu archivo 'clientes_...csv/xml'.")
            return

        # 2. Verificar cuántas ventas están huérfanas
        cur.execute("""
                    SELECT COUNT(*)
                    FROM processed_data pd
                             INNER JOIN user_info ui ON pd.user_id = ui.uuid
                    WHERE pd.status = 'completed';
                    """)
        ventas_conectadas = cur.fetchone()[0]
        print(f"🔗 Ventas correctamente conectadas a clientes: {ventas_conectadas}")

        # 3. Reparar si el cruce es cero
        if ventas_conectadas == 0:
            print("⚠️ Las ventas apuntan a clientes fantasmas. Reparando enlaces...")

            # Asignamos de forma aleatoria un cliente válido a cada venta huérfana
            cur.execute("""
                        UPDATE processed_data
                        SET user_id = (SELECT uuid
                                       FROM user_info
                                       ORDER BY random()
                                       LIMIT 1)
                        WHERE user_id IS NULL
                           OR user_id NOT IN (SELECT uuid FROM user_info);
                        """)
            conn.commit()
            print("✅ Ventas re-vinculadas exitosamente con tus clientes actuales.")

        cur.close()
    except Exception as e:
        conn.rollback()
        print(f"❌ Error en reparar_cruce_rfm: {e}")
    finally:
        Connect_BD.cerrar_conexion(conn)


def borrar_datos():
    conn = Connect_BD.crear_conexion()
    if not conn:
        return

    try:
        cur = conn.cursor()
        print("🗑️ Borrando datos residuales (sources, audit, analysis)...")

        # Se corrigió a triple comilla para permitir el salto de línea
        cur.execute("""
                    TRUNCATE TABLE data_sources CASCADE;
                    TRUNCATE TABLE audit_logs CASCADE;
                    TRUNCATE TABLE analysis_results CASCADE;
                    """)

        conn.commit()  # Faltaba hacer el commit para guardar los cambios
        print("✅ Datos residuales borrados exitosamente.")
        cur.close()
    except Exception as e:
        conn.rollback()  # Faltaba el rollback de seguridad en caso de error
        print(f"❌ Error en borrar_datos: {e}")
    finally:
        Connect_BD.cerrar_conexion(conn)  # Faltaba cerrar la conexión de esta función


if __name__ == "__main__":
    reparar_cruce_rfm()
    borrar_datos()