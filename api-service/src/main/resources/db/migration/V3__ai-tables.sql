CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE data_sources
(
    source_id   UUID PRIMARY KEY     DEFAULT uuidv7(),
    source_type VARCHAR(50) NOT NULL,
    file_name   TEXT,
    endpoint    TEXT,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE data_ingestions
(
    batch_id       UUID PRIMARY KEY     DEFAULT uuidv7(),
    source_id      UUID        NOT NULL,
    ingestion_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    file_name      TEXT,
    endpoint       TEXT,
    record_count   INTEGER,
    status         VARCHAR(20) NOT NULL DEFAULT 'pending',
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ingestions_source FOREIGN KEY (source_id) REFERENCES data_sources (source_id)
);

CREATE TABLE raw_data
(
    raw_id         UUID PRIMARY KEY     DEFAULT uuidv7(),
    batch_id       UUID        NOT NULL,
    source_id      UUID        NOT NULL,
    ingestion_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    file_format    VARCHAR(10),
    raw_content    JSONB       NOT NULL,
    checksum       TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_raw_batch FOREIGN KEY (batch_id) REFERENCES data_ingestions (batch_id),
    CONSTRAINT fk_raw_source FOREIGN KEY (source_id) REFERENCES data_sources (source_id)
);


CREATE TABLE processed_data
(
    record_id    UUID PRIMARY KEY     DEFAULT uuidv7(),
    raw_id       UUID,
    batch_id     UUID        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    event_date   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id      UUID,
    customer_id  UUID,
    product_id   UUID,
    category     VARCHAR(100),
    type         VARCHAR(100),
    amount       NUMERIC,
    quantity     NUMERIC,
    score        NUMERIC,
    value        NUMERIC,
    source       VARCHAR(100),
    region       VARCHAR(100),
    device       VARCHAR(100),
    data         JSONB,
    CONSTRAINT fk_processed_raw FOREIGN KEY (raw_id) REFERENCES raw_data (raw_id),
    CONSTRAINT fk_processed_batch FOREIGN KEY (batch_id) REFERENCES data_ingestions (batch_id)
);


CREATE TABLE ai_content
(
    ai_content_id UUID PRIMARY KEY     DEFAULT uuidv7(),
    record_id     UUID        NOT NULL,
    text_content  TEXT,
    summary       TEXT,
    tags          TEXT[],
    embedding_id  UUID,
    embedding     DOUBLE PRECISION[],
    model_used    VARCHAR(100),
    language      VARCHAR(10)          DEFAULT 'es',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_processed FOREIGN KEY (record_id) REFERENCES processed_data (record_id)
);


CREATE TABLE analysis_results
(
    analysis_id      UUID PRIMARY KEY     DEFAULT uuidv7(),
    input_data_id    UUID        NOT NULL,
    analysis_type    VARCHAR(50) NOT NULL,
    model_name       VARCHAR(100),
    model_version    VARCHAR(50),
    result           JSONB       NOT NULL,
    confidence_score NUMERIC,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_analysis_input FOREIGN KEY (input_data_id) REFERENCES processed_data (record_id)
);


CREATE TABLE audit_logs
(
    log_id         UUID PRIMARY KEY      DEFAULT uuidv7(),
    process_name   VARCHAR(200) NOT NULL,
    log_level      VARCHAR(10)  NOT NULL DEFAULT 'INFO',
    batch_id       UUID,
    record_id      UUID,
    analysis_id    UUID,
    error_message  TEXT,
    execution_time INTERVAL,
    metadata       JSONB,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);