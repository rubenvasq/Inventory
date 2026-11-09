-- Etapa 4: movimientos de inventario (libro mayor inmutable).
-- Cada fila registra un cambio de existencias de un producto y conserva el stock
-- anterior y resultante para trazabilidad. Los movimientos no se editan ni borran.

CREATE TABLE stock_movement (
    id             BIGSERIAL    PRIMARY KEY,
    product_id     BIGINT       NOT NULL,
    movement_type  VARCHAR(20)  NOT NULL,
    quantity       INTEGER      NOT NULL,
    previous_stock INTEGER      NOT NULL,
    new_stock      INTEGER      NOT NULL,
    note           VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_movement_product  FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_movement_type     CHECK (movement_type IN ('IN', 'OUT', 'ADJUSTMENT')),
    CONSTRAINT chk_movement_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_movement_prev     CHECK (previous_stock >= 0),
    CONSTRAINT chk_movement_new      CHECK (new_stock >= 0)
);

CREATE INDEX idx_movement_product ON stock_movement (product_id);
CREATE INDEX idx_movement_created ON stock_movement (created_at);
