

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;


CREATE TABLE public.ai_content (
    ai_content_id uuid DEFAULT uuidv7() NOT NULL,
    record_id uuid NOT NULL,
    text_content text,
    summary text,
    tags text[],
    embedding_id uuid,
    embedding double precision[],
    model_used character varying(100),
    language character varying(10) DEFAULT 'es'::character varying,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.ai_content OWNER TO postgres;


CREATE TABLE public.analysis_results (
    analysis_id uuid DEFAULT uuidv7() NOT NULL,
    input_data_id uuid NOT NULL,
    analysis_type character varying(50) NOT NULL,
    model_name character varying(100),
    model_version character varying(50),
    result jsonb NOT NULL,
    confidence_score numeric,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.analysis_results OWNER TO postgres;


CREATE TABLE public.audit_logs (
    log_id uuid DEFAULT uuidv7() NOT NULL,
    process_name character varying(200) NOT NULL,
    log_level character varying(10) DEFAULT 'INFO'::character varying NOT NULL,
    batch_id uuid,
    record_id uuid,
    analysis_id uuid,
    error_message text,
    execution_time interval,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.audit_logs OWNER TO postgres;

CREATE TABLE public.cart (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    last_update timestamp without time zone
);


ALTER TABLE public.cart OWNER TO postgres;

CREATE TABLE public.cart_item (
    id uuid NOT NULL,
    cart_id uuid,
    variant_id uuid NOT NULL,
    saved_quantity integer,
    last_update timestamp without time zone
);


ALTER TABLE public.cart_item OWNER TO postgres;

CREATE TABLE public.category (
    id uuid NOT NULL,
    name character varying(100) NOT NULL,
    parent_id uuid,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.category OWNER TO postgres;


CREATE TABLE public.data_ingestions (
    batch_id uuid DEFAULT uuidv7() NOT NULL,
    source_id uuid NOT NULL,
    ingestion_date timestamp with time zone DEFAULT now() NOT NULL,
    file_name text,
    endpoint text,
    record_count integer,
    status character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.data_ingestions OWNER TO postgres;


CREATE TABLE public.data_sources (
    source_id uuid DEFAULT uuidv7() NOT NULL,
    source_type character varying(50) NOT NULL,
    file_name text,
    endpoint text,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.data_sources OWNER TO postgres;


CREATE TABLE public.discount (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    decimal_value numeric NOT NULL,
    stackable boolean NOT NULL,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    active boolean NOT NULL,
    last_update timestamp without time zone,
    created_at timestamp without time zone
);


ALTER TABLE public.discount OWNER TO postgres;


CREATE TABLE public.discount_target (
    discount_id bigint NOT NULL,
    category_id uuid,
    product_id uuid,
    item_id uuid
);


ALTER TABLE public.discount_target OWNER TO postgres;


CREATE SEQUENCE public.error_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.error_logs_id_seq OWNER TO postgres;


CREATE TABLE public.error_logs (
    id bigint DEFAULT nextval('public.error_logs_id_seq'::regclass) NOT NULL,
    trace_id uuid,
    exception_name character varying(255),
    message text,
    stack_trace text,
    user_id uuid,
    request_uri character varying(255),
    http_method character varying(255),
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.error_logs OWNER TO postgres;



CREATE TABLE public.inventory_trace (
    id uuid NOT NULL,
    item_id uuid,
    physical_balance integer,
    physical_delta integer,
    available_balance integer,
    available_delta integer,
    redundancy_balance integer,
    redundancy_delta integer,
    reserved_balance integer,
    reserved_delta integer,
    "timestamp" timestamp without time zone
);


ALTER TABLE public.inventory_trace OWNER TO postgres;

CREATE TABLE public.item (
    id uuid NOT NULL,
    sku character varying(255) NOT NULL,
    product_id uuid,
    base_price numeric NOT NULL,
    logical_limit integer NOT NULL,
    visible boolean NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE public.item OWNER TO postgres;


CREATE TABLE public.item_attribute (
    id uuid NOT NULL,
    item_id uuid NOT NULL,
    attribute_id uuid NOT NULL,
    attribute_value character varying(255) NOT NULL
);


ALTER TABLE public.item_attribute OWNER TO postgres;

CREATE TABLE public.item_picture (
    id uuid NOT NULL,
    item_id uuid NOT NULL,
    url text NOT NULL,
    image_order integer
);


ALTER TABLE public.item_picture OWNER TO postgres;

CREATE TABLE public.item_price_history (
    id bigint NOT NULL,
    item_id uuid NOT NULL,
    base_price numeric NOT NULL,
    nominal_price numeric NOT NULL,
    last_update timestamp without time zone
);


ALTER TABLE public.item_price_history OWNER TO postgres;


CREATE TABLE public.processed_data (
    record_id uuid DEFAULT uuidv7() NOT NULL,
    raw_id uuid,
    batch_id uuid NOT NULL,
    processed_at timestamp with time zone DEFAULT now() NOT NULL,
    status character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    event_date timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    user_id uuid,
    customer_id uuid,
    product_id uuid,
    category character varying(100),
    type character varying(100),
    amount numeric,
    quantity numeric,
    score numeric,
    value numeric,
    source character varying(100),
    region character varying(100),
    device character varying(100),
    data jsonb
);


ALTER TABLE public.processed_data OWNER TO postgres;


CREATE TABLE public.product (
    id uuid NOT NULL,
    category_id uuid NOT NULL,
    brand character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description text NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE public.product OWNER TO postgres;

CREATE TABLE public.product_attribute (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    attribute_key character varying(255) NOT NULL
);


ALTER TABLE public.product_attribute OWNER TO postgres;


CREATE TABLE public.raw_data (
    raw_id uuid DEFAULT uuidv7() NOT NULL,
    batch_id uuid NOT NULL,
    source_id uuid NOT NULL,
    ingestion_date timestamp with time zone DEFAULT now() NOT NULL,
    file_format character varying(10),
    raw_content jsonb NOT NULL,
    checksum text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.raw_data OWNER TO postgres;


CREATE TABLE public.saved_later_item (
    id uuid NOT NULL,
    list_id uuid NOT NULL,
    item_id uuid NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


ALTER TABLE public.saved_later_item OWNER TO postgres;

CREATE TABLE public.saved_later_list (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    last_update timestamp without time zone
);


ALTER TABLE public.saved_later_list OWNER TO postgres;

CREATE TABLE public.user_address (
    id bigint NOT NULL,
    user_id uuid NOT NULL,
    slot_index integer NOT NULL,
    internal_name character varying(100) NOT NULL,
    place_id character varying(512),
    formatted_address text NOT NULL,
    street_number character varying(255),
    route character varying(255),
    locality character varying(255),
    administrative_area_level1 character varying(255),
    postal_code character varying(10),
    country_code character varying(2) NOT NULL,
    latitude numeric(11,8),
    longitude numeric(10,8)
);


ALTER TABLE public.user_address OWNER TO postgres;


CREATE TABLE public.user_info (
    uuid uuid NOT NULL,
    email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    surname character varying(255) NOT NULL,
    country_code character varying(2) NOT NULL,
    currency_code character varying(3) NOT NULL,
    phone_number character varying(255) NOT NULL,
    picture_url text NOT NULL,
    created_at timestamp without time zone,
    last_updated timestamp without time zone
);


ALTER TABLE public.user_info OWNER TO postgres;

CREATE TABLE public.wish_item (
    id uuid NOT NULL,
    wish_list_id uuid NOT NULL,
    item_id uuid NOT NULL,
    last_update timestamp without time zone
);


ALTER TABLE public.wish_item OWNER TO postgres;

CREATE TABLE public.wish_list (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    tittle character varying(255) NOT NULL,
    description text,
    principal_list boolean NOT NULL,
    last_update timestamp without time zone
);


ALTER TABLE public.wish_list OWNER TO postgres;

ALTER TABLE ONLY public.ai_content
    ADD CONSTRAINT ai_content_pkey PRIMARY KEY (ai_content_id);



ALTER TABLE ONLY public.analysis_results
    ADD CONSTRAINT analysis_results_pkey PRIMARY KEY (analysis_id);


ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (log_id);


ALTER TABLE ONLY public.data_ingestions
    ADD CONSTRAINT data_ingestions_pkey PRIMARY KEY (batch_id);

ALTER TABLE ONLY public.data_sources
    ADD CONSTRAINT data_sources_file_name_uq UNIQUE (file_name);

ALTER TABLE ONLY public.data_sources
    ADD CONSTRAINT data_sources_pkey PRIMARY KEY (source_id);

ALTER TABLE ONLY public.cart
    ADD CONSTRAINT pk_cart PRIMARY KEY (id);

ALTER TABLE ONLY public.cart_item
    ADD CONSTRAINT pk_cartitem PRIMARY KEY (id);

ALTER TABLE ONLY public.category
    ADD CONSTRAINT pk_category PRIMARY KEY (id);

ALTER TABLE ONLY public.discount
    ADD CONSTRAINT pk_discount PRIMARY KEY (id);

ALTER TABLE ONLY public.discount_target
    ADD CONSTRAINT pk_discounttarget PRIMARY KEY (discount_id);

ALTER TABLE ONLY public.error_logs
    ADD CONSTRAINT pk_error_logs PRIMARY KEY (id);

ALTER TABLE ONLY public.inventory_trace
    ADD CONSTRAINT pk_inventorytrace PRIMARY KEY (id);

ALTER TABLE ONLY public.item
    ADD CONSTRAINT pk_item PRIMARY KEY (id);

ALTER TABLE ONLY public.item_attribute
    ADD CONSTRAINT pk_itemattribute PRIMARY KEY (id);

ALTER TABLE ONLY public.item_picture
    ADD CONSTRAINT pk_itempicture PRIMARY KEY (id);

ALTER TABLE ONLY public.item_price_history
    ADD CONSTRAINT pk_itempricehistory PRIMARY KEY (id);

ALTER TABLE ONLY public.product
    ADD CONSTRAINT pk_product PRIMARY KEY (id);

ALTER TABLE ONLY public.product_attribute
    ADD CONSTRAINT pk_productattribute PRIMARY KEY (id);

ALTER TABLE ONLY public.saved_later_item
    ADD CONSTRAINT pk_savedlateritem PRIMARY KEY (id);

ALTER TABLE ONLY public.saved_later_list
    ADD CONSTRAINT pk_savedlaterlist PRIMARY KEY (id);

ALTER TABLE ONLY public.user_address
    ADD CONSTRAINT pk_useraddress PRIMARY KEY (id);

ALTER TABLE ONLY public.user_info
    ADD CONSTRAINT pk_userinfo PRIMARY KEY (uuid);

ALTER TABLE ONLY public.wish_item
    ADD CONSTRAINT pk_wishitem PRIMARY KEY (id);

ALTER TABLE ONLY public.wish_list
    ADD CONSTRAINT pk_wishlist PRIMARY KEY (id);

ALTER TABLE ONLY public.processed_data
    ADD CONSTRAINT processed_data_pkey PRIMARY KEY (record_id);

ALTER TABLE ONLY public.processed_data
    ADD CONSTRAINT processed_data_raw_batch_uq UNIQUE (raw_id, batch_id);

ALTER TABLE ONLY public.raw_data
    ADD CONSTRAINT raw_data_checksum_uq UNIQUE (checksum);

ALTER TABLE ONLY public.raw_data
    ADD CONSTRAINT raw_data_pkey PRIMARY KEY (raw_id);


ALTER TABLE ONLY public.item_attribute
    ADD CONSTRAINT uc_a0a2f07c05463ca91f7c28114 UNIQUE (item_id, attribute_id);


ALTER TABLE ONLY public.cart
    ADD CONSTRAINT uc_cart_user UNIQUE (user_id);

ALTER TABLE ONLY public.category
    ADD CONSTRAINT uc_category_name UNIQUE (name);

ALTER TABLE ONLY public.item
    ADD CONSTRAINT uc_item_sku UNIQUE (sku);

ALTER TABLE ONLY public.product
    ADD CONSTRAINT uc_product_name UNIQUE (name);

ALTER TABLE ONLY public.saved_later_list
    ADD CONSTRAINT uc_savedlaterlist_user UNIQUE (user_id);

ALTER TABLE ONLY public.user_info
    ADD CONSTRAINT uc_userinfo_email UNIQUE (email);

ALTER TABLE ONLY public.wish_list
    ADD CONSTRAINT uc_wishlist_tittle UNIQUE (tittle);

ALTER TABLE ONLY public.cart_item
    ADD CONSTRAINT uk_cart_product UNIQUE (cart_id, variant_id);

ALTER TABLE ONLY public.discount_target
    ADD CONSTRAINT uk_discount_target_target_id UNIQUE (discount_id);

ALTER TABLE ONLY public.saved_later_item
    ADD CONSTRAINT uk_saved_list_item UNIQUE (list_id, item_id);

ALTER TABLE ONLY public.wish_list
    ADD CONSTRAINT uk_wish_list_user UNIQUE (user_id, principal_list);



ALTER TABLE ONLY public.wish_item
    ADD CONSTRAINT uk_with_list_product UNIQUE (wish_list_id, item_id);


ALTER TABLE ONLY public.analysis_results
    ADD CONSTRAINT uq_analysis_type_name_version UNIQUE (analysis_type, model_name, model_version);



CREATE INDEX data_ingestions_status_idx ON public.data_ingestions USING btree (status) WHERE ((status)::text = ANY ((ARRAY['processing'::character varying, 'failed'::character varying])::text[]));



CREATE INDEX idx_discount_dates ON public.discount USING btree (start_date, end_date);



CREATE INDEX idx_item_price ON public.item USING btree (base_price);


CREATE INDEX idx_item_product ON public.item USING btree (product_id);


CREATE INDEX idx_product_category ON public.product USING btree (category_id);



CREATE INDEX idx_product_created_at ON public.product USING btree (created_at DESC);


CREATE UNIQUE INDEX idx_product_name ON public.product USING btree (name);


CREATE INDEX idx_user_info_email ON public.user_info USING btree (email);

CREATE INDEX processed_data_batch_id_idx ON public.processed_data USING btree (batch_id);



CREATE INDEX processed_data_category_date_idx ON public.processed_data USING btree (category, event_date DESC) WHERE (category IS NOT NULL);


CREATE INDEX processed_data_event_date_idx ON public.processed_data USING btree (event_date);



CREATE INDEX processed_data_event_date_partial_idx ON public.processed_data USING btree (event_date) WHERE (event_date IS NOT NULL);


CREATE UNIQUE INDEX processed_data_raw_batch_uidx ON public.processed_data USING btree (raw_id, batch_id);


CREATE INDEX processed_data_status_idx ON public.processed_data USING btree (status);


CREATE INDEX raw_data_batch_id_idx ON public.raw_data USING btree (batch_id);


CREATE UNIQUE INDEX raw_data_checksum_key ON public.raw_data USING btree (checksum);


CREATE UNIQUE INDEX uq_data_sources_file_name ON public.data_sources USING btree (file_name);



CREATE UNIQUE INDEX uq_processed_raw_batch ON public.processed_data USING btree (raw_id, batch_id);


CREATE UNIQUE INDEX uq_raw_data_checksum ON public.raw_data USING btree (checksum);


ALTER TABLE ONLY public.ai_content
    ADD CONSTRAINT fk_ai_processed FOREIGN KEY (record_id) REFERENCES public.processed_data(record_id);


ALTER TABLE ONLY public.analysis_results
    ADD CONSTRAINT fk_analysis_input FOREIGN KEY (input_data_id) REFERENCES public.processed_data(record_id);


ALTER TABLE ONLY public.cart
    ADD CONSTRAINT fk_cart_on_user FOREIGN KEY (user_id) REFERENCES public.user_info(uuid);


ALTER TABLE ONLY public.cart_item
    ADD CONSTRAINT fk_cartitem_on_cart FOREIGN KEY (cart_id) REFERENCES public.cart(id);


ALTER TABLE ONLY public.cart_item
    ADD CONSTRAINT fk_cartitem_on_variant FOREIGN KEY (variant_id) REFERENCES public.item(id);


ALTER TABLE ONLY public.category
    ADD CONSTRAINT fk_category_on_parent FOREIGN KEY (parent_id) REFERENCES public.category(id);


ALTER TABLE ONLY public.discount_target
    ADD CONSTRAINT fk_discounttarget_on_discount FOREIGN KEY (discount_id) REFERENCES public.discount(id);



ALTER TABLE ONLY public.data_ingestions
    ADD CONSTRAINT fk_ingestions_source FOREIGN KEY (source_id) REFERENCES public.data_sources(source_id);


ALTER TABLE ONLY public.inventory_trace
    ADD CONSTRAINT fk_inventorytrace_on_item FOREIGN KEY (item_id) REFERENCES public.item(id);




ALTER TABLE ONLY public.item
    ADD CONSTRAINT fk_item_on_product FOREIGN KEY (product_id) REFERENCES public.product(id);



ALTER TABLE ONLY public.item_attribute
    ADD CONSTRAINT fk_itemattribute_on_attribute FOREIGN KEY (attribute_id) REFERENCES public.product_attribute(id);


ALTER TABLE ONLY public.item_attribute
    ADD CONSTRAINT fk_itemattribute_on_item FOREIGN KEY (item_id) REFERENCES public.item(id);



ALTER TABLE ONLY public.item_picture
    ADD CONSTRAINT fk_itempicture_on_item FOREIGN KEY (item_id) REFERENCES public.item(id);



ALTER TABLE ONLY public.item_price_history
    ADD CONSTRAINT fk_itempricehistory_on_item FOREIGN KEY (item_id) REFERENCES public.item(id);


ALTER TABLE ONLY public.processed_data
    ADD CONSTRAINT fk_processed_batch FOREIGN KEY (batch_id) REFERENCES public.data_ingestions(batch_id);




ALTER TABLE ONLY public.processed_data
    ADD CONSTRAINT fk_processed_raw FOREIGN KEY (raw_id) REFERENCES public.raw_data(raw_id);


ALTER TABLE ONLY public.product
    ADD CONSTRAINT fk_product_on_category FOREIGN KEY (category_id) REFERENCES public.category(id);


ALTER TABLE ONLY public.product_attribute
    ADD CONSTRAINT fk_productattribute_on_product FOREIGN KEY (product_id) REFERENCES public.product(id);



ALTER TABLE ONLY public.raw_data
    ADD CONSTRAINT fk_raw_batch FOREIGN KEY (batch_id) REFERENCES public.data_ingestions(batch_id);


ALTER TABLE ONLY public.raw_data
    ADD CONSTRAINT fk_raw_source FOREIGN KEY (source_id) REFERENCES public.data_sources(source_id);



ALTER TABLE ONLY public.saved_later_item
    ADD CONSTRAINT fk_savedlateritem_on_item FOREIGN KEY (item_id) REFERENCES public.item(id);


ALTER TABLE ONLY public.saved_later_item
    ADD CONSTRAINT fk_savedlateritem_on_list FOREIGN KEY (list_id) REFERENCES public.saved_later_list(id);


ALTER TABLE ONLY public.saved_later_list
    ADD CONSTRAINT fk_savedlaterlist_on_user FOREIGN KEY (user_id) REFERENCES public.user_info(uuid);


ALTER TABLE ONLY public.user_address
    ADD CONSTRAINT fk_useraddress_on_user FOREIGN KEY (user_id) REFERENCES public.user_info(uuid);




ALTER TABLE ONLY public.wish_item
    ADD CONSTRAINT fk_wishitem_on_item FOREIGN KEY (item_id) REFERENCES public.item(id);



ALTER TABLE ONLY public.wish_item
    ADD CONSTRAINT fk_wishitem_on_wish_list FOREIGN KEY (wish_list_id) REFERENCES public.wish_list(id);



ALTER TABLE ONLY public.wish_list
    ADD CONSTRAINT fk_wishlist_on_user FOREIGN KEY (user_id) REFERENCES public.user_info(uuid);


ALTER TABLE ONLY public.processed_data
    ADD CONSTRAINT fk_processed_user FOREIGN KEY (user_id) REFERENCES public.user_info(uuid);

ALTER TABLE ONLY public.processed_data
    ADD CONSTRAINT fk_processed_product FOREIGN KEY (product_id) REFERENCES public.product(id);


