CREATE SEQUENCE IF NOT EXISTS discount_target_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS error_logs_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS item_base_price_change_track_seq START WITH 1 INCREMENT BY 100;

CREATE SEQUENCE IF NOT EXISTS order_adjustment_seq START WITH 1 INCREMENT BY 10;

CREATE SEQUENCE IF NOT EXISTS price_calculation_history_seq START WITH 1 INCREMENT BY 100;

CREATE SEQUENCE IF NOT EXISTS user_address_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE cart
(
    id          UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    last_update TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_cart PRIMARY KEY (id)
);

CREATE TABLE cart_item
(
    id             UUID                     NOT NULL,
    cart_id        UUID,
    item_id        UUID                     NOT NULL,
    saved_quantity INTEGER,
    last_update    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_cartitem PRIMARY KEY (id)
);

CREATE TABLE category
(
    id         UUID                     NOT NULL,
    name       VARCHAR(100)             NOT NULL,
    parent_id  UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (id)
);

CREATE TABLE credit_transaction
(
    id              UUID                     NOT NULL,
    idempotency_key UUID                     NOT NULL,
    user_id         UUID                     NOT NULL,
    actor_user_id   UUID,
    type            VARCHAR(255)             NOT NULL,
    delta           DECIMAL(12, 2)           NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_credittransaction PRIMARY KEY (id)
);

CREATE TABLE cupon
(
    id             UUID                        NOT NULL,
    name           VARCHAR(255)                NOT NULL,
    type           VARCHAR(255)                NOT NULL,
    user_id        UUID                        NOT NULL,
    decimal_factor DECIMAL                     NOT NULL,
    active         BOOLEAN                     NOT NULL,
    stackable      BOOLEAN                     NOT NULL,
    used_at        TIMESTAMP WITHOUT TIME ZONE,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_cupon PRIMARY KEY (id)
);

CREATE TABLE discount
(
    id             UUID                     NOT NULL,
    name           VARCHAR(255)             NOT NULL,
    decimal_value  DECIMAL(5, 4)            NOT NULL,
    decimal_factor DECIMAL(5, 4)            NOT NULL,
    stackable      BOOLEAN                  NOT NULL,
    start_date     TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    active         BOOLEAN                  NOT NULL,
    last_update    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_discount PRIMARY KEY (id)
);

CREATE TABLE discount_target
(
    id          BIGINT NOT NULL,
    discount_id UUID   NOT NULL,
    category_id UUID,
    product_id  UUID,
    item_id     UUID,
    CONSTRAINT pk_discounttarget PRIMARY KEY (id)
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

CREATE TABLE inventory_trace
(
    id                 UUID NOT NULL,
    item_id            UUID NOT NULL,
    physical_balance   INTEGER,
    physical_delta     INTEGER,
    available_balance  INTEGER,
    available_delta    INTEGER,
    redundancy_balance INTEGER,
    redundancy_delta   INTEGER,
    reserved_balance   INTEGER,
    reserved_delta     INTEGER,
    timestamp          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_inventorytrace PRIMARY KEY (id)
);

CREATE TABLE invitation_trace
(
    id              UUID                     NOT NULL,
    inviting_id     UUID                     NOT NULL,
    invited_uuid    UUID                     NOT NULL,
    invitation_code VARCHAR(255)             NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    burned_at       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_invitationtrace PRIMARY KEY (id)
);

CREATE TABLE item
(
    id            UUID                     NOT NULL,
    sku           VARCHAR(255)             NOT NULL,
    product_id    UUID,
    base_price    DECIMAL(12, 2)           NOT NULL,
    logical_limit INTEGER                  NOT NULL,
    visible       BOOLEAN                  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
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

CREATE TABLE item_base_price_change_track
(
    id                     BIGINT                   NOT NULL,
    item_id                UUID                     NOT NULL,
    new_price              DECIMAL(12, 2)           NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    checked_by_transaction UUID,
    CONSTRAINT pk_itembasepricechangetrack PRIMARY KEY (id)
);

CREATE TABLE item_creation_track
(
    item_id                UUID                     NOT NULL,
    checked_by_transaction UUID,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_itemcreationtrack PRIMARY KEY (item_id)
);

CREATE TABLE item_picture
(
    id          UUID NOT NULL,
    item_id     UUID NOT NULL,
    url         TEXT NOT NULL,
    image_order INTEGER,
    CONSTRAINT pk_itempicture PRIMARY KEY (id)
);

CREATE TABLE order_adjustment
(
    id       BIGINT         NOT NULL,
    order_id UUID           NOT NULL,
    label    VARCHAR(255)   NOT NULL,
    amount   DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_orderadjustment PRIMARY KEY (id)
);

CREATE TABLE order_detail
(
    id             UUID                     NOT NULL,
    checkout_token UUID                     NOT NULL,
    user_id        UUID                     NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_orderdetail PRIMARY KEY (id)
);

CREATE TABLE order_item
(
    id         UUID           NOT NULL,
    order_id   UUID           NOT NULL,
    item_id    UUID           NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    amount     INTEGER        NOT NULL,
    CONSTRAINT pk_orderitem PRIMARY KEY (id)
);

CREATE TABLE order_timeline
(
    id              UUID                                   NOT NULL,
    order_id        UUID                                   NOT NULL,
    event           VARCHAR(255)                           NOT NULL,
    additional_info TEXT,
    timestamp       TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT pk_ordertimeline PRIMARY KEY (id)
);

CREATE TABLE point_of_sale_order
(
    order_id       UUID NOT NULL,
    seller_user_id UUID NOT NULL,
    CONSTRAINT pk_pointofsaleorder PRIMARY KEY (order_id)
);

CREATE TABLE price_calculation_history
(
    id               INTEGER                  NOT NULL,
    transaction_id   UUID                     NOT NULL,
    item_id          UUID                     NOT NULL,
    base_price       DECIMAL(12, 2)           NOT NULL,
    final_price      DECIMAL(12, 2)           NOT NULL,
    discount_decimal DECIMAL(5, 4)            NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_pricecalculationhistory PRIMARY KEY (id)
);

CREATE TABLE pricing_transaction_status
(
    transaction_id UUID                                   NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    calculated_at  TIMESTAMP WITH TIME ZONE,
    indexed_at     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_pricingtransactionstatus PRIMARY KEY (transaction_id)
);

CREATE TABLE product
(
    id          UUID                     NOT NULL,
    category_id UUID                     NOT NULL,
    brand       VARCHAR(255)             NOT NULL,
    name        VARCHAR(255)             NOT NULL,
    description TEXT                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_product PRIMARY KEY (id)
);

CREATE TABLE product_attribute
(
    id            UUID         NOT NULL,
    product_id    UUID         NOT NULL,
    attribute_key VARCHAR(255) NOT NULL,
    CONSTRAINT pk_productattribute PRIMARY KEY (id)
);

CREATE TABLE saved_later_item
(
    id         UUID                     NOT NULL,
    list_id    UUID                     NOT NULL,
    item_id    UUID                     NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_savedlateritem PRIMARY KEY (id)
);

CREATE TABLE saved_later_list
(
    id          UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    last_update TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_savedlaterlist PRIMARY KEY (id)
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
    longitude                  DECIMAL(11, 8),
    CONSTRAINT pk_useraddress PRIMARY KEY (id)
);

CREATE TABLE user_credit
(
    user_id      UUID                        NOT NULL,
    balance      DECIMAL                     NOT NULL,
    version      BIGINT,
    last_updated TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_usercredit PRIMARY KEY (user_id)
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

CREATE TABLE wish_item
(
    id           UUID                     NOT NULL,
    wish_list_id UUID                     NOT NULL,
    item_id      UUID                     NOT NULL,
    last_update  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_wishitem PRIMARY KEY (id)
);

CREATE TABLE wish_list
(
    id             UUID                     NOT NULL,
    user_id        UUID                     NOT NULL,
    tittle         VARCHAR(255)             NOT NULL,
    description    TEXT,
    principal_list BOOLEAN                  NOT NULL,
    last_update    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_wishlist PRIMARY KEY (id)
);

ALTER TABLE item_attribute
    ADD CONSTRAINT uc_a0a2f07c05463ca91f7c28114 UNIQUE (item_id, attribute_id);

ALTER TABLE cart
    ADD CONSTRAINT uc_cart_user UNIQUE (user_id);

ALTER TABLE category
    ADD CONSTRAINT uc_category_name UNIQUE (name);

ALTER TABLE credit_transaction
    ADD CONSTRAINT uc_credittransaction_idempotencykey UNIQUE (idempotency_key);

ALTER TABLE invitation_trace
    ADD CONSTRAINT uc_invitationtrace_invited_uuid UNIQUE (invited_uuid);

ALTER TABLE item
    ADD CONSTRAINT uc_item_sku UNIQUE (sku);

ALTER TABLE saved_later_list
    ADD CONSTRAINT uc_savedlaterlist_user UNIQUE (user_id);

ALTER TABLE user_info
    ADD CONSTRAINT uc_userinfo_email UNIQUE (email);

ALTER TABLE wish_list
    ADD CONSTRAINT uc_wishlist_tittle UNIQUE (tittle);

ALTER TABLE cart_item
    ADD CONSTRAINT uk_cart_product UNIQUE (cart_id, item_id);

ALTER TABLE saved_later_item
    ADD CONSTRAINT uk_saved_list_item UNIQUE (list_id, item_id);

ALTER TABLE wish_list
    ADD CONSTRAINT uk_wish_list_user UNIQUE (user_id, principal_list);

ALTER TABLE wish_item
    ADD CONSTRAINT uk_with_list_product UNIQUE (wish_list_id, item_id);

CREATE INDEX idx_discount_active ON discount (active);

CREATE INDEX idx_discount_dates ON discount (start_date, end_date);

CREATE INDEX idx_item_price ON item (base_price);

CREATE INDEX idx_item_vissible ON item (visible);

CREATE INDEX idx_price_calculation_history_transaction_id ON price_calculation_history (transaction_id);

CREATE INDEX idx_product_created_at ON product (created_at DESC);

CREATE UNIQUE INDEX idx_product_name ON product (name);

CREATE INDEX idx_user_info_email ON user_info (email);

ALTER TABLE cart_item
    ADD CONSTRAINT FK_CARTITEM_ON_CART FOREIGN KEY (cart_id) REFERENCES cart (id);

ALTER TABLE cart_item
    ADD CONSTRAINT FK_CARTITEM_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE cart
    ADD CONSTRAINT FK_CART_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE category
    ADD CONSTRAINT FK_CATEGORY_ON_PARENT FOREIGN KEY (parent_id) REFERENCES category (id);

ALTER TABLE credit_transaction
    ADD CONSTRAINT FK_CREDITTRANSACTION_ON_ACTOR_USER FOREIGN KEY (actor_user_id) REFERENCES user_info (uuid);

ALTER TABLE credit_transaction
    ADD CONSTRAINT FK_CREDITTRANSACTION_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE cupon
    ADD CONSTRAINT FK_CUPON_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE discount_target
    ADD CONSTRAINT FK_DISCOUNTTARGET_ON_DISCOUNT FOREIGN KEY (discount_id) REFERENCES discount (id);

ALTER TABLE inventory_trace
    ADD CONSTRAINT FK_INVENTORYTRACE_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE invitation_trace
    ADD CONSTRAINT FK_INVITATIONTRACE_ON_INVITED_UUID FOREIGN KEY (invited_uuid) REFERENCES user_info (uuid);

ALTER TABLE invitation_trace
    ADD CONSTRAINT FK_INVITATIONTRACE_ON_INVITING FOREIGN KEY (inviting_id) REFERENCES user_info (uuid);

ALTER TABLE item_attribute
    ADD CONSTRAINT FK_ITEMATTRIBUTE_ON_ATTRIBUTE FOREIGN KEY (attribute_id) REFERENCES product_attribute (id);

ALTER TABLE item_attribute
    ADD CONSTRAINT FK_ITEMATTRIBUTE_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE item_base_price_change_track
    ADD CONSTRAINT FK_ITEMBASEPRICECHANGETRACK_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

CREATE INDEX idx_item_base_price_change_track_item_id ON item_base_price_change_track (item_id);

ALTER TABLE item_creation_track
    ADD CONSTRAINT FK_ITEMCREATIONTRACK_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE item_picture
    ADD CONSTRAINT FK_ITEMPICTURE_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE item
    ADD CONSTRAINT FK_ITEM_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES product (id);

CREATE INDEX idx_item_product ON item (product_id);

ALTER TABLE order_adjustment
    ADD CONSTRAINT FK_ORDERADJUSTMENT_ON_ORDER FOREIGN KEY (order_id) REFERENCES order_detail (id);

ALTER TABLE order_detail
    ADD CONSTRAINT FK_ORDERDETAIL_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE order_item
    ADD CONSTRAINT FK_ORDERITEM_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE order_item
    ADD CONSTRAINT FK_ORDERITEM_ON_ORDER FOREIGN KEY (order_id) REFERENCES order_detail (id);

ALTER TABLE order_timeline
    ADD CONSTRAINT FK_ORDERTIMELINE_ON_ORDER FOREIGN KEY (order_id) REFERENCES order_detail (id);

ALTER TABLE point_of_sale_order
    ADD CONSTRAINT FK_POINTOFSALEORDER_ON_ORDER FOREIGN KEY (order_id) REFERENCES order_detail (id);

ALTER TABLE point_of_sale_order
    ADD CONSTRAINT FK_POINTOFSALEORDER_ON_SELLER_USER FOREIGN KEY (seller_user_id) REFERENCES user_info (uuid);

ALTER TABLE price_calculation_history
    ADD CONSTRAINT FK_PRICECALCULATIONHISTORY_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE product_attribute
    ADD CONSTRAINT FK_PRODUCTATTRIBUTE_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES product (id);

CREATE INDEX idx_product_attribute_porudtc_id ON product_attribute (product_id);

ALTER TABLE product
    ADD CONSTRAINT FK_PRODUCT_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES category (id);

CREATE INDEX idx_product_category ON product (category_id);

ALTER TABLE saved_later_item
    ADD CONSTRAINT FK_SAVEDLATERITEM_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE saved_later_item
    ADD CONSTRAINT FK_SAVEDLATERITEM_ON_LIST FOREIGN KEY (list_id) REFERENCES saved_later_list (id);

ALTER TABLE saved_later_list
    ADD CONSTRAINT FK_SAVEDLATERLIST_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE user_address
    ADD CONSTRAINT FK_USERADDRESS_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE user_credit
    ADD CONSTRAINT FK_USERCREDIT_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);

ALTER TABLE wish_item
    ADD CONSTRAINT FK_WISHITEM_ON_ITEM FOREIGN KEY (item_id) REFERENCES item (id);

ALTER TABLE wish_item
    ADD CONSTRAINT FK_WISHITEM_ON_WISH_LIST FOREIGN KEY (wish_list_id) REFERENCES wish_list (id);

ALTER TABLE wish_list
    ADD CONSTRAINT FK_WISHLIST_ON_USER FOREIGN KEY (user_id) REFERENCES user_info (uuid);