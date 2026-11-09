-- Etapa 7: usuarios y roles.
-- "user" es palabra reservada en PostgreSQL, por eso la tabla se llama app_user.

CREATE TABLE app_user (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(60)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    full_name  VARCHAR(150) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_username UNIQUE (username),
    CONSTRAINT chk_user_role    CHECK (role IN ('ADMIN', 'OPERADOR'))
);
