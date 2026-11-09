-- Etapa 3: productos y existencias.
-- Un producto pertenece a una categoria (obligatoria) y, opcionalmente, a un proveedor.

CREATE TABLE product (
    id          BIGSERIAL     PRIMARY KEY,
    sku         VARCHAR(60)   NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    description VARCHAR(500),
    category_id BIGINT        NOT NULL,
    supplier_id BIGINT,
    unit        VARCHAR(30)   NOT NULL DEFAULT 'unidad',
    unit_price  NUMERIC(12,2),
    stock       INTEGER       NOT NULL DEFAULT 0,
    min_stock   INTEGER       NOT NULL DEFAULT 0,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_sku       UNIQUE (sku),
    CONSTRAINT fk_product_category  FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_product_supplier  FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    CONSTRAINT chk_product_stock     CHECK (stock >= 0),
    CONSTRAINT chk_product_min_stock CHECK (min_stock >= 0),
    CONSTRAINT chk_product_price     CHECK (unit_price IS NULL OR unit_price >= 0)
);

CREATE INDEX idx_product_category ON product (category_id);
CREATE INDEX idx_product_supplier ON product (supplier_id);
