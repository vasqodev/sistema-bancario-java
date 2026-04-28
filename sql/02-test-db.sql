-- ─────────────────────────────────────────────────────────────────────────────
-- sistema-bancario-java — base de datos de tests (banco_test)
--
-- ⚠ DUPLICACIÓN INTENCIONAL DE ESQUEMA ⚠
-- Las tablas definidas aquí deben mantenerse idénticas a las de schema.sql.
-- Cualquier cambio de estructura (columnas, tipos, CHECKs, FKs, índices)
-- DEBE replicarse en AMBOS archivos. No hay seed: cada test crea sus propios
-- datos en @BeforeEach para garantizar aislamiento.
--
-- Este archivo se ejecuta automáticamente la primera vez que arranca el
-- contenedor de Postgres (está montado en /docker-entrypoint-initdb.d/).
-- Para reaplicarlo en un contenedor ya iniciado sin perder los datos de
-- `banco`, ejecutar manualmente:
--   docker exec -i banco-postgres psql -U banco -d postgres < sql/02-test-db.sql
-- ─────────────────────────────────────────────────────────────────────────────

DROP DATABASE IF EXISTS banco_test;
CREATE DATABASE banco_test;

\connect banco_test


-- ── Tablas (idénticas a schema.sql, sin datos semilla) ───────────────────────

CREATE TABLE cliente (
    id              BIGSERIAL    PRIMARY KEY,
    rut             VARCHAR(20)  NOT NULL UNIQUE,
    nombre          VARCHAR(80)  NOT NULL,
    apellido        VARCHAR(80)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    telefono        VARCHAR(20),
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE cuenta (
    id              BIGSERIAL      PRIMARY KEY,
    numero_cuenta   VARCHAR(20)    NOT NULL UNIQUE,
    tipo            VARCHAR(15)    NOT NULL CHECK (tipo IN ('CORRIENTE', 'AHORRO')),
    saldo           NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (saldo >= 0),
    cliente_id      BIGINT         NOT NULL REFERENCES cliente(id) ON DELETE RESTRICT,
    fecha_apertura  TIMESTAMP      NOT NULL DEFAULT NOW(),
    activa          BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_cuenta_cliente ON cuenta(cliente_id);

CREATE TABLE transaccion (
    id                BIGSERIAL      PRIMARY KEY,
    cuenta_origen_id  BIGINT         NOT NULL REFERENCES cuenta(id),
    cuenta_destino_id BIGINT         NOT NULL REFERENCES cuenta(id),
    monto             NUMERIC(19, 4) NOT NULL CHECK (monto > 0),
    fecha             TIMESTAMP      NOT NULL DEFAULT NOW(),
    estado            VARCHAR(15)    NOT NULL CHECK (estado IN ('EXITOSA', 'RECHAZADA')),
    CHECK (cuenta_origen_id <> cuenta_destino_id)
);

CREATE INDEX idx_transaccion_origen  ON transaccion(cuenta_origen_id);
CREATE INDEX idx_transaccion_destino ON transaccion(cuenta_destino_id);
