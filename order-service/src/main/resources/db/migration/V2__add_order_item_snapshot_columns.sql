ALTER TABLE p_order_items
    ADD COLUMN product_name VARCHAR(255),
    ADD COLUMN unit_price BIGINT;

UPDATE p_order_items
SET product_name = ''
WHERE product_name IS NULL;

UPDATE p_order_items
SET unit_price = 0
WHERE unit_price IS NULL;

ALTER TABLE p_order_items
    ALTER COLUMN product_name SET NOT NULL,
    ALTER COLUMN unit_price SET NOT NULL;
