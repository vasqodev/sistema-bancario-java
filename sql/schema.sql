-- ─────────────────────────────────────────────────────────────────────────────
-- sistema-bancario-java — esquema y datos semilla (BD `banco`)
--
-- ⚠ DUPLICACIÓN INTENCIONAL DE ESQUEMA ⚠
-- Las tablas definidas aquí se replican en sql/02-test-db.sql (BD `banco_test`).
-- Cualquier cambio de estructura (columnas, tipos, CHECKs, FKs, índices)
-- DEBE aplicarse en AMBOS archivos para que los tests no diverjan de prod.
--
-- Este archivo se ejecuta automáticamente la primera vez que arranca el
-- contenedor de Postgres (está montado en /docker-entrypoint-initdb.d/).
-- Para volver a aplicarlo desde cero: `docker compose down -v && docker compose up -d`
-- (el flag -v borra el volumen de datos).
--
-- Conexión local desde el host: psql -h localhost -p 15432 -U banco -d banco
-- ─────────────────────────────────────────────────────────────────────────────


-- ── Tablas ───────────────────────────────────────────────────────────────────

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
    -- NUMERIC(19,4) para dinero: 15 dígitos enteros + 4 decimales, sin pérdida de precisión.
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
    -- Una transferencia no puede ser a la misma cuenta.
    CHECK (cuenta_origen_id <> cuenta_destino_id)
);

CREATE INDEX idx_transaccion_origen  ON transaccion(cuenta_origen_id);
CREATE INDEX idx_transaccion_destino ON transaccion(cuenta_destino_id);


-- ── Datos semilla ────────────────────────────────────────────────────────────
-- 3 clientes, cada uno con 1 cuenta CORRIENTE y 1 cuenta AHORRO.
-- Los saldos representan el estado actual; las transferencias semilla son
-- ilustrativas para que el historial no aparezca vacío al abrir la app.

INSERT INTO cliente (rut, nombre, apellido, email, telefono) VALUES
    ('11.111.111-1', 'María',   'González', 'maria.gonzalez@example.cl', '+56 9 1111 1111'),
    ('22.222.222-2', 'Carlos',  'Pérez',    'carlos.perez@example.cl',   '+56 9 2222 2222'),
    ('33.333.333-3', 'Javiera', 'Soto',     'javiera.soto@example.cl',   '+56 9 3333 3333');

INSERT INTO cuenta (numero_cuenta, tipo, saldo, cliente_id) VALUES
    ('CTA-1001', 'CORRIENTE',  500000.0000, 1),
    ('CTA-1002', 'AHORRO',    1200000.0000, 1),
    ('CTA-2001', 'CORRIENTE',  150000.0000, 2),
    ('CTA-2002', 'AHORRO',     750000.0000, 2),
    ('CTA-3001', 'CORRIENTE',  300000.0000, 3),
    ('CTA-3002', 'AHORRO',     950000.0000, 3);

INSERT INTO transaccion (cuenta_origen_id, cuenta_destino_id, monto, estado) VALUES
    (1, 3,  50000.0000, 'EXITOSA'),
    (3, 5,  20000.0000, 'EXITOSA'),
    (5, 1, 100000.0000, 'EXITOSA');
