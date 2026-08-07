CREATE TABLE p_company (
   company_id UUID NOT NULL,
   name VARCHAR(255) NOT NULL,
   company_type VARCHAR(20) NOT NULL,
   hub_id UUID NOT NULL,
   address VARCHAR(255) NOT NULL,
   created_at TIMESTAMP,
   created_by UUID,
   updated_at TIMESTAMP,
   updated_by UUID,
   deleted_at TIMESTAMP,
   deleted_by UUID,
   PRIMARY KEY (company_id)
);

CREATE TABLE p_product (
   product_id UUID NOT NULL,
   name VARCHAR(255) NOT NULL,
   company_id UUID NOT NULL,
   hub_id UUID NOT NULL,
   price BIGINT NOT NULL,
   stock_quantity BIGINT NOT NULL,
   created_at TIMESTAMP,
   created_by UUID,
   updated_at TIMESTAMP,
   updated_by UUID,
   deleted_at TIMESTAMP,
   deleted_by UUID,
   PRIMARY KEY (product_id),
   CONSTRAINT fk_product_company_id FOREIGN KEY (company_id) REFERENCES p_company(company_id)
);