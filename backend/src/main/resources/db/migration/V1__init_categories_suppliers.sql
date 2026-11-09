-- Etapa 2: dominio base. Tablas de categorias y proveedores.
-- Flyway es la unica fuente de verdad del esquema (Hibernate solo valida).

CREATE TABLE category (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_category_name UNIQUE (name)
);

CREATE TABLE supplier (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    contact_name VARCHAR(150),
    email        VARCHAR(150),
    phone        VARCHAR(50),
    address      VARCHAR(255),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_supplier_name UNIQUE (name)
);
