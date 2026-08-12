CREATE TABLE p_product_stock_movement (
      id UUID NOT NULL,
      product_id UUID NOT NULL,
      reference_id VARCHAR(100) NOT NULL,
      movement_type VARCHAR(20) NOT NULL,
      quantity INTEGER NOT NULL,
      created_at TIMESTAMP NOT NULL,
      PRIMARY KEY (id),
      CONSTRAINT fk_stock_movement_product_id FOREIGN KEY (product_id) REFERENCES p_product(product_id)
);

-- 같은 (상품, 참조ID, 증감타입) 조합의 재고 변경이 중복 적용되지 않도록 강제
CREATE UNIQUE INDEX uq_stock_movement_product_reference_type
    ON p_product_stock_movement (product_id, reference_id, movement_type);
