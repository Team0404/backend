ALTER TABLE p_order_items
    ADD COLUMN stock_operation_id UUID;

UPDATE p_order_items
SET stock_operation_id = id
WHERE stock_operation_id IS NULL;

ALTER TABLE p_order_items
    ALTER COLUMN stock_operation_id SET NOT NULL;

ALTER TABLE p_order_items
    ADD CONSTRAINT uq_order_items_stock_operation_id UNIQUE (stock_operation_id);
