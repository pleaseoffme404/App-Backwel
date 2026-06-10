#!/bin/bash

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
else
    echo "Error: Archivo .env no encontrado."
    exit 1
fi

echo "--- Limpieza Total (Fantasmas y Volúmenes) ---"
docker compose down -v --remove-orphans

rm -rf ./api-service/target

echo "---️ Reconstruyendo y Levantando Stack ---"
docker compose up -d --build --force-recreate

echo "--- Verificando salud del sistema ---"
docker compose ps

# =====================================================================
# INYECCIÓN DE COMANDOS PARA LA BASE DE DATOS
# =====================================================================

echo "--- Esperando a que la Base de Datos esté lista ---"
# Le damos 5 segundos para que el motor de Postgres termine de arrancar internamente
sleep 5

echo "--- Ejecutando scripts SQL de inicialización ---"
# Modificamos el comando para enviarlo directamente al contenedor sin el modo interactivo (-it)
docker exec frontend-database bash -c "
for f in /docker-entrypoint-initdb.d/*.sql; do
    echo '-> Ejecutando \$f';
    psql -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -f \"\$f\";
done
"