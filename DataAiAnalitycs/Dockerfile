FROM python:3.11-slim

RUN apt-get update && apt-get install -y \
    gcc \
    libpq-dev \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV PYTHONPATH=/app:/app/Backend:/app/IA

COPY pyproject.toml uv.lock requeriments.txt ./

RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r requeriments.txt

COPY Backend/ ./Backend/
COPY Main/ ./Main/
COPY IA/ ./IA/
COPY utils/ ./utils/
COPY db/ ./db/

EXPOSE 7777

CMD ["python", "Main/app.py"]