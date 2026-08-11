CREATE TABLE p_orders (
    id UUID NOT NULL,
    order_number VARCHAR(50) UNIQUE,
    company_id UUID NOT NULL,
    hub_id UUID NOT NULL,
    delivery_id UUID,
    request_note VARCHAR(500),
    delivery_deadline TIMESTAMP,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    PRIMARY KEY (id)
);

CREATE TABLE p_order_items (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES p_orders(id)
);
