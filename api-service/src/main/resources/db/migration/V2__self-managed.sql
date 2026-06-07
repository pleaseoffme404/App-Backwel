ALTER TABLE user_address
ADD CONSTRAINT uk_user_address_slot
UNIQUE (user_id, slot_index)
DEFERRABLE INITIALLY DEFERRED;


ALTER TABLE item_picture
ADD CONSTRAINT uk_item_picture_order
UNIQUE (item_id, image_order)
DEFERRABLE INITIALLY DEFERRED;

CREATE INDEX idx_item_picture_item_id_order ON item_picture (item_id, image_order ASC);


ALTER TABLE category
ADD CONSTRAINT ck_no_self_parent
CHECK (id <> parent_id);

CREATE TABLE item_product_category_path
(
    item_id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    category_path UUID[] NOT NULL,

    CONSTRAINT fk_item
        FOREIGN KEY (item_id)
        REFERENCES item (id)
        ON DELETE CASCADE,

        FOREIGN KEY (product_id)
        REFERENCES product(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_item_category_path ON item_product_category_path USING GIN (category_path);

CREATE UNIQUE INDEX uk_discount_target_category ON discount_target (discount_id, category_id) WHERE category_id IS NOT NULL;
CREATE UNIQUE INDEX uk_discount_target_product ON discount_target (discount_id, product_id) WHERE product_id IS NOT NULL;
CREATE UNIQUE INDEX uk_discount_target_item ON discount_target (discount_id, item_id) WHERE item_id IS NOT NULL;

-- Create index on the price calculation where discount equals 0
CREATE INDEX idx_price_calculation_history_discount_decimal_eq_zero ON price_calculation_history (discount_decimal) WHERE discount_decimal = 0.00;


CREATE INDEX idx_price_calculation_history_item_date
    ON price_calculation_history (item_id, created_at DESC)
    INCLUDE (base_price, final_price, discount_decimal);


CREATE INDEX idx_pending_price_changes ON item_base_price_change_track (item_id, created_at DESC) WHERE checked_by_transaction IS NULL;

CREATE INDEX idx_pending_item_creations ON item_creation_track (item_id) WHERE checked_by_transaction IS NULL;