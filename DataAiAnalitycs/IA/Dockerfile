# =============================================================================
# STAGE 1: Builder
# Los paquetes científicos (numpy, pandas, scikit-learn) tienen extensiones C,
# por eso necesitamos herramientas de compilación en esta etapa.
# =============================================================================
FROM python:3.10-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /build

# Dependencias de sistema necesarias para compilar paquetes científicos
RUN apt-get update && apt-get install --no-install-recommends -y \
    build-essential \
    gcc \
    g++ \
    libopenblas-dev \
    && rm -rf /var/lib/apt/lists/*

# Copia primero los archivos de dependencias (cache-friendly)
COPY mierda/pyproject.toml .
# COPY poetry.lock .  # descomenta si usas un lockfile

# Instala las dependencias en un directorio aislado
RUN pip install --upgrade pip && \
    pip install --prefix=/install --no-cache-dir .


# =============================================================================
# STAGE 2: Runtime
# Imagen limpia: sin compiladores, sin headers, sin archivos temporales de build.
# Solo Python + las librerías .so ya compiladas copiadas desde el builder.
# =============================================================================
FROM python:3.10-slim AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH="/app" \
    # Mejora el rendimiento de numpy/scikit-learn en contenedores
    OMP_NUM_THREADS=4 \
    OPENBLAS_NUM_THREADS=4

# Instala solo las librerías de runtime (sin -dev), necesarias para las .so compiladas
RUN apt-get update && apt-get install --no-install-recommends -y \
    libopenblas-base \
    && rm -rf /var/lib/apt/lists/*

# Usuario no-root por seguridad
RUN groupadd --gid 1001 appgroup && \
    useradd --uid 1001 --gid appgroup --shell /bin/bash --create-home appuser

WORKDIR /app

# Copia binarios y paquetes Python ya compilados
COPY --from=builder /install /usr/local

# Copia únicamente el código fuente de IA
COPY IA/ ./IA/

RUN chown -R appuser:appgroup /app

USER appuser

# Los scripts de IA suelen ejecutarse como workers o batch jobs.
# Si expones una API interna de inferencia, define el puerto aquí:
EXPOSE 8001

# Health check adaptado a un worker/servicio de inferencia
HEALTHCHECK --interval=60s --timeout=15s --start-period=30s --retries=3 \
    CMD python -c "import sklearn; import pandas" || exit 1

# Entrypoint flexible: permite sobrescribir el comando desde docker-compose / K8s.
# Ajusta según si es un servidor de inferencia, un worker de cola, o un script batch.
CMD ["python", "-m", "IA.main"]