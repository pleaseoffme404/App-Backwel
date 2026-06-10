
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE SEQUENCE IF NOT EXISTS discount_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS error_logs_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS item_price_history_seq START WITH 1 INCREMENT BY 5;
CREATE SEQUENCE IF NOT EXISTS user_address_seq START WITH 1 INCREMENT BY 50;


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


CREATE TABLE user_info
(
    uuid          UUID         NOT NULL,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    surname       VARCHAR(255) NOT NULL,
    country_code  VARCHAR(2)   NOT NULL,
    currency_code VARCHAR(3)   NOT NULL,
    phone_number  VARCHAR(255) NOT NULL,
    picture_url   TEXT         NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    last_updated  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_userinfo PRIMARY KEY (uuid)
);

CREATE TABLE user_address
(
    id                         BIGINT       NOT NULL,
    user_id                    UUID         NOT NULL,
    slot_index                 INTEGER      NOT NULL,
    internal_name              VARCHAR(100) NOT NULL,
    place_id                   VARCHAR(512),
    formatted_address          TEXT         NOT NULL,
    street_number              VARCHAR(255),
    route                      VARCHAR(255),
    locality                   VARCHAR(255),
    administrative_area_level1 VARCHAR(255),
    postal_code                VARCHAR(10),
    country_code               VARCHAR(2)   NOT NULL,
    latitude                   DECIMAL(11, 8),
    longitude                  DECIMAL(10, 8),
    CONSTRAINT pk_useraddress PRIMARY KEY (id)
);

CREATE TABLE category
(
    id         UUID                         NOT NULL,
    name       VARCHAR(100)                 NOT NULL,
    parent_id  UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (id)
);

CREATE TABLE product
(
    id          UUID         NOT NULL,
    category_id UUID         NOT NULL,
    brand       VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_product PRIMARY KEY (id)
);

CREATE TABLE product_attribute
(
    id            UUID         NOT NULL,
    product_id    UUID         NOT NULL,
    attribute_key VARCHAR(255) NOT NULL,
    CONSTRAINT pk_productattribute PRIMARY KEY (id)
);

CREATE TABLE item
(
    id            UUID         NOT NULL,
    sku           VARCHAR(255) NOT NULL,
    product_id    UUID,
    base_price    DECIMAL      NOT NULL,
    logical_limit INTEGER      NOT NULL,
    visible       BOOLEAN      NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    updated_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_item PRIMARY KEY (id)
);

CREATE TABLE item_attribute
(
    id              UUID         NOT NULL,
    item_id         UUID         NOT NULL,
    attribute_id    UUID         NOT NULL,
    attribute_value VARCHAR(255) NOT NULL,
    CONSTRAINT pk_itemattribute PRIMARY KEY (id)
);

CREATE TABLE item_picture
(
    id          UUID NOT NULL,
    item_id     UUID NOT NULL,
    url         TEXT NOT NULL,
    image_order INTEGER,
    CONSTRAINT pk_itempicture PRIMARY KEY (id)
);

CREATE TABLE item_price_history
(
    id            BIGINT  NOT NULL,
    item_id       UUID    NOT NULL,
    base_price    DECIMAL NOT NULL,
    nominal_price DECIMAL NOT NULL,
    last_update   TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_itempricehistory PRIMARY KEY (id)
);

CREATE TABLE inventory_trace
(
    id                 UUID NOT NULL,
    item_id            UUID,
    physical_balance   INTEGER,
    physical_delta     INTEGER,
    available_balance  INTEGER,
    available_delta    INTEGER,
    redundancy_balance INTEGER,
    redundancy_delta   INTEGER,
    reserved_balance   INTEGER,
    reserved_delta     INTEGER,
    timestamp          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_inventorytrace PRIMARY KEY (id)
);

CREATE TABLE cart
(
    id          UUID NOT NULL,
    user_id     UUID NOT NULL,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_cart PRIMARY KEY (id)
);

CREATE TABLE cart_item
(
    id              UUID NOT NULL,
    cart_id         UUID,
    variant_id      UUID NOT NULL,
    saved_quantity  INTEGER,
    last_update     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_cartitem PRIMARY KEY (id)
);

CREATE TABLE wish_list
(
    id             UUID         NOT NULL,
    user_id        UUID         NOT NULL,
    tittle         VARCHAR(255) NOT NULL,
    description    TEXT,
    principal_list BOOLEAN      NOT NULL,
    last_update    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_wishlist PRIMARY KEY (id)
);

CREATE TABLE wish_item
(
    id          UUID NOT NULL,
    wish_list_id UUID NOT NULL,
    item_id     UUID NOT NULL,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_wishitem PRIMARY KEY (id)
);

CREATE TABLE saved_later_list
(
    id          UUID NOT NULL,
    user_id     UUID NOT NULL,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_savedlaterlist PRIMARY KEY (id)
);

CREATE TABLE saved_later_item
(
    id         UUID                         NOT NULL,
    list_id    UUID                         NOT NULL,
    item_id    UUID                         NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_savedlateritem PRIMARY KEY (id)
);

CREATE TABLE discount
(
    id            BIGINT                      NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    decimal_value DECIMAL                     NOT NULL,
    stackable     BOOLEAN                     NOT NULL,
    start_date    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    active        BOOLEAN                     NOT NULL,
    last_update   TIMESTAMP WITHOUT TIME ZONE,
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_discount PRIMARY KEY (id)
);

CREATE TABLE discount_target
(
    discount_id BIGINT NOT NULL,
    category_id UUID,
    product_id  UUID,
    item_id     UUID,
    CONSTRAINT pk_discounttarget PRIMARY KEY (discount_id)
);

CREATE TABLE error_logs
(
    id             BIGINT                      NOT NULL,
    trace_id       UUID,
    exception_name VARCHAR(255),
    message        TEXT,
    stack_trace    TEXT,
    user_id        UUID,
    request_uri    VARCHAR(255),
    http_method    VARCHAR(255),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_error_logs PRIMARY KEY (id)
);

ALTER TABLE user_info ADD CONSTRAINT uc_userinfo_email UNIQUE (email);
ALTER TABLE category ADD CONSTRAINT uc_category_name UNIQUE (name);
ALTER TABLE product ADD CONSTRAINT uc_product_name UNIQUE (name);
ALTER TABLE item ADD CONSTRAINT uc_item_sku UNIQUE (sku);
ALTER TABLE item_attribute ADD CONSTRAINT uc_a0a2f07c05463ca91f7c28114 UNIQUE (item_id, attribute_id);
ALTER TABLE cart ADD CONSTRAINT uc_cart_user UNIQUE (user_id);
ALTER TABLE cart_item ADD CONSTRAINT uk_cart_product UNIQUE (cart_id, variant_id);
ALTER TABLE wish_list ADD CONSTRAINT uc_wishlist_tittle UNIQUE (tittle);
ALTER TABLE wish_list ADD CONSTRAINT uk_wish_list_user UNIQUE (user_id, principal_list);
ALTER TABLE wish_item ADD CONSTRAINT uk_with_list_product UNIQUE (wish_list_id, item_id);
ALTER TABLE saved_later_list ADD CONSTRAINT uc_savedlaterlist_user UNIQUE (user_id);
ALTER TABLE saved_later_item ADD CONSTRAINT uk_saved_list_item UNIQUE (list_id, item_id);
ALTER TABLE discount_target ADD CONSTRAINT uk_discount_target_target_id UNIQUE (discount_id);


CREATE INDEX idx_discount_dates ON discount (start_date, end_date);
CREATE INDEX idx_item_price ON item (base_price);
CREATE INDEX idx_product_created_at ON product (created_at DESC);
CREATE UNIQUE INDEX idx_product_name ON product (name);
CREATE INDEX idx_user_info_email ON user_info (email);
CREATE INDEX idx_item_product ON item (product_id);
CREATE INDEX idx_product_category ON product (category_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_data_sources_file_name ON data_sources (file_name);
CREATE UNIQUE INDEX IF NOT EXISTS uq_raw_data_checksum ON raw_data (checksum);
CREATE UNIQUE INDEX IF NOT EXISTS uq_processed_raw_batch ON processed_data (raw_id, batch_id);
CREATE SEQUENCE IF NOT EXISTS error_logs_id_seq;
ALTER TABLE error_logs
ALTER COLUMN id SET DEFAULT nextval('error_logs_id_seq');


ALTER TABLE cart_item ADD CONSTRAINT FK_CARTITEM_ON_CART FOREIGN KEY (cart_id) REFERENCES cart (id);
ALTER TABLE cart_item ADD CONSTRAINT FK_CARTITEM_ON_VARIANT FOREIGN KEY (variant_id) REFERENCES item (id);

ALTER TABLE cart ADD CONSTRAINT FK_CART_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE category ADD CONSTRAINT FK_CATEGORY_ON_PARENT FOREIGN KEY (parent_id) REFERENCES category (id);

ALTER TABLE discount_target ADD CONSTRAINT FK_DISCOUNTTARGET_ON_DISCOUNT FOREIGN KEY (discount_id) REFERENCES discount (id);

ALTER TABLE inventory_trace ADD CONSTRAINT FK_INVENTORYTRACE_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE item_attribute ADD CONSTRAINT FK_ITEMATTRIBUTE_ON_ATTRIBUTE FOREIGN KEY (attribute_id) REFERENCES product_attribute (id);
ALTER TABLE item_attribute ADD CONSTRAINT FK_ITEMATTRIBUTE_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE item_picture ADD CONSTRAINT FK_ITEMPICTURE_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE item_price_history ADD CONSTRAINT FK_ITEMPRICEHISTORY_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE item ADD CONSTRAINT FK_ITEM_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES product (id);

ALTER TABLE product_attribute ADD CONSTRAINT FK_PRODUCTATTRIBUTE_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES product (id);

ALTER TABLE product ADD CONSTRAINT FK_PRODUCT_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES category (id);

ALTER TABLE saved_later_item ADD CONSTRAINT FK_SAVEDLATERITEM_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);
ALTER TABLE saved_later_item ADD CONSTRAINT FK_SAVEDLATERITEM_ON_LIST FOREIGN KEY (list_id) REFERENCES saved_later_list (id);

ALTER TABLE saved_later_list ADD CONSTRAINT FK_SAVEDLATERLIST_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE user_address ADD CONSTRAINT FK_USERADDRESS_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);



ALTER TABLE wish_item ADD CONSTRAINT FK_WISHITEM_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);
ALTER TABLE wish_item ADD CONSTRAINT FK_WISHITEM_ON_WISH_LIST FOREIGN KEY (wish_list_id) REFERENCES wish_list (id);

ALTER TABLE wish_list ADD CONSTRAINT FK_WISHLIST_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE analysis_results ADD CONSTRAINT uq_analysis_type UNIQUE (analysis_type);
ALTER TABLE analysis_results
ADD CONSTRAINT uq_analysis_type_model
UNIQUE (analysis_type, model_name, model_version);



CREATE TABLE client (
    id                SERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    registration_date DATE NOT NULL DEFAULT CURRENT_DATE
);

-- Columna nueva en processed_data
ALTER TABLE processed_data
    ADD COLUMN IF NOT EXISTS client_id INTEGER REFERENCES client(id);









DO $$ BEGIN
    ALTER TABLE data_sources ADD CONSTRAINT data_sources_file_name_uq UNIQUE (file_name);
EXCEPTION WHEN duplicate_table THEN NULL;
END $$;
DO $$ BEGIN
    ALTER TABLE raw_data ADD CONSTRAINT raw_data_checksum_uq UNIQUE (checksum);
EXCEPTION WHEN duplicate_table THEN NULL;
END $$;
DO $$ BEGIN
    ALTER TABLE processed_data ADD CONSTRAINT processed_data_raw_batch_uq UNIQUE (raw_id, batch_id);
EXCEPTION WHEN duplicate_table THEN NULL;
END $$;
CREATE INDEX IF NOT EXISTS processed_data_event_date_partial_idx
    ON processed_data (event_date)
    WHERE event_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS processed_data_status_idx
    ON processed_data (status);
CREATE INDEX IF NOT EXISTS processed_data_category_date_idx
    ON processed_data (category, event_date DESC)
    WHERE category IS NOT NULL;
CREATE INDEX IF NOT EXISTS data_ingestions_status_idx
    ON data_ingestions (status)
    WHERE status IN ('processing', 'failed');

ALTER TABLE analysis_results DROP CONSTRAINT uq_analysis_type;
ALTER TABLE analysis_results DROP CONSTRAINT unique_analysis_type;

ALTER TABLE analysis_results
ADD CONSTRAINT uq_analysis_type_name_version
UNIQUE (analysis_type, model_name, model_version);