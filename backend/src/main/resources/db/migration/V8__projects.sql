-- Etapa 8: proyectos e instalaciones de clientes.
-- Un proyecto agrupa material del inventario que se entrega a una instalacion.
-- Las asignaciones planifican cuanto material se reservara para el proyecto;
-- las entregas descuentan stock y las devoluciones lo reintegran, siempre con
-- un movimiento de inventario vinculado al proyecto.

CREATE TABLE project (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    client      VARCHAR(150) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    start_date  DATE,
    end_date    DATE,
    notes       VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_project_status CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE project_assignment (
    id                 BIGSERIAL   PRIMARY KEY,
    project_id         BIGINT      NOT NULL,
    product_id         BIGINT      NOT NULL,
    assigned_quantity  INTEGER     NOT NULL,
    delivered_quantity INTEGER     NOT NULL DEFAULT 0,
    returned_quantity  INTEGER     NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_assignment_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_assignment_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT uq_assignment_project_product UNIQUE (project_id, product_id),
    CONSTRAINT chk_assignment_assigned  CHECK (assigned_quantity > 0),
    CONSTRAINT chk_assignment_delivered  CHECK (delivered_quantity >= 0 AND delivered_quantity <= assigned_quantity),
    CONSTRAINT chk_assignment_returned   CHECK (returned_quantity >= 0 AND returned_quantity <= delivered_quantity)
);

CREATE INDEX idx_assignment_project ON project_assignment (project_id);
CREATE INDEX idx_assignment_product ON project_assignment (product_id);

-- Vinculo opcional de cada movimiento con el proyecto que lo origino:
-- salidas por entrega de material y entradas por devolucion.
ALTER TABLE stock_movement ADD COLUMN project_id BIGINT REFERENCES project (id);

CREATE INDEX idx_movement_project ON stock_movement (project_id);
