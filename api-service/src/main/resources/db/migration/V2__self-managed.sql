ALTER TABLE user_address
ADD CONSTRAINT uk_user_address_slot
UNIQUE (user_id, slot_index)
DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE category
ADD CONSTRAINT ck_no_self_parent
CHECK (id <> parent_id);

-- Crear una tabla pre-calculada con el category path de cada producto
CREATE TABLE product_category_path
(
    product_id    UUID PRIMARY KEY,
    category_path UUID[] NOT NULL,

    CONSTRAINT fk_product
        FOREIGN KEY (product_id)
        REFERENCES product (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_product_category_path ON product_category_path USING GIN (category_path);