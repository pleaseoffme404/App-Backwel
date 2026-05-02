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