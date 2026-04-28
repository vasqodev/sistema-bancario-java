# sistema-bancario-java

> **Estado:** en construcción. Fases 1 y 2 completas; UI Swing y demo de
> concurrencia pendientes (Fases 3-5). README definitivo en Fase 5.

## Descripción

Aplicación Java de escritorio que simula la gestión de clientes, cuentas y
transferencias bancarias. Proyecto de portafolio basado en las tres
asignaturas de Java de Duoc UC: Fundamentos, POO I y POO II.

## Estado actual

### ✅ Fase 1 — Modelo y persistencia
- Jerarquía polimórfica: `Cuenta` abstracta + `CuentaCorriente` / `CuentaAhorro`,
  con `calcularInteres()` distinto por subclase.
- Patrón DAO: interfaces (`ClienteDAO`, `CuentaDAO`, `TransaccionDAO`) e
  implementaciones JDBC puras.
- `PreparedStatement` en todas las queries (anti SQL injection).
- `BigDecimal` con `RoundingMode.HALF_EVEN` (banker's rounding) para dinero.
- 6 excepciones de dominio nominales (`SaldoInsuficienteException`,
  `CuentaInactivaException`, `MontoInvalidoException`, `CuentaNoExisteException`,
  `ClienteNoExisteException`, `ClienteTieneCuentasException`).
- `DemoDAO` como smoke test que imprime el estado completo de la BD.

### ✅ Fase 2 — Servicios y concurrencia
- `TransferenciaService` con `ReentrantLock` por cuenta en
  `ConcurrentHashMap<Long, ReentrantLock>`.
- **Lock-ordering** por id ascendente para prevenir deadlock A↔B.
- Transacciones JDBC (`setAutoCommit(false)` / `commit` / `rollback`) sobre una
  única `Connection` para débito + crédito + INSERT en `transaccion`, vía
  sobrecarga transaccional en los DAOs.
- `ClienteService` traduce `SQLState 23503` (FK violation) en
  `ClienteTieneCuentasException` con mensaje accionable.
- `CuentaService` valida `clienteId` antes de insertar y lanza
  `ClienteNoExisteException` si no existe.
- **9 tests JUnit 5 verdes**, BD aislada `banco_test`:
  - 50 transferencias concurrentes preservan el saldo total (`AtomicInteger`).
  - 60 transferencias en direcciones opuestas no se bloquean (`@Timeout(5s)`).
  - Rollback verificado bajo saldo insuficiente (sin tocar la BD).
  - Auto-transferencia rechazada antes de adquirir locks.
  - Caminos felices y errores de validación de los servicios CRUD.

### ⏳ Fase 3 — UI Swing (pendiente)
- `VentanaPrincipal` con `JTabbedPane` (Clientes / Cuentas / Transferencias).
- CRUDs visuales para `Cliente` y `Cuenta`.
- `JDialog` modal para transferencia con validación previa.
- Botón "Aplicar interés mensual" para demostrar polimorfismo en vivo.

### ⏳ Fase 4 — Demo de concurrencia visual (pendiente)
- Panel con N hilos / M transferencias configurables.
- Log en tiempo real de éxitos y rechazos.
- Verificación visual de saldo total preservado.

### ⏳ Fase 5 — Pulido final (pendiente)
- README definitivo con capturas de la UI.
- Documentación expandida (Javadoc en clases públicas).
- Verificación end-to-end desde clon limpio.

## Stack técnico

- **Java 21 LTS** (text blocks, switch expressions, pattern matching)
- **Maven** (compiler, surefire, exec plugins)
- **PostgreSQL 16** en Docker
- **JDBC puro** — sin ORM, sin Spring; toda la persistencia se ve y se entiende
- **Java Swing** (a partir de Fase 3)
- **JUnit 5** Jupiter

## Estructura de paquetes

```
cl.rvasquez.banco
├── modelo/        Cliente, Cuenta (abstract), CuentaCorriente, CuentaAhorro,
│                  Transaccion, TipoCuenta, EstadoTransaccion
├── dao/           Interfaces ClienteDAO, CuentaDAO, TransaccionDAO
│   └── impl/      Implementaciones JDBC (xxxDAOImpl)
├── servicio/      ClienteService, CuentaService, TransferenciaService
├── excepcion/     6 excepciones de dominio nominales (extends RuntimeException)
├── util/          ConexionBD (lectura de properties + override por env vars)
├── Main           Punto de entrada (Fase 0: SELECT 1)
└── DemoDAO        Smoke test de la capa de datos (Fase 1)
```

## Decisiones técnicas notables

- **`PreparedStatement` siempre.** No hay concatenación de SQL en ningún DAO,
  ni siquiera en queries triviales — política única para que sea trivial de
  auditar.
- **Cláusula `RETURNING` de PostgreSQL.** Insertar y rescatar `id`/`fecha` en
  un solo round-trip, sin `getGeneratedKeys()`.
- **`BigDecimal` con escala 4 y `HALF_EVEN`.** Schema usa `NUMERIC(19,4)`;
  el redondeo bancario evita el sesgo del `HALF_UP` clásico.
- **Excepciones de dominio nominales.** Cada regla de negocio rota tiene su
  propia clase en `excepcion/`. La vista las captura por tipo, sin parsear
  mensajes ni inspeccionar `SQLException`.
- **Lock-ordering por id ascendente.** Antes de operar sobre dos cuentas, el
  servicio toma siempre primero el lock del id menor. Esto convierte el
  deadlock clásico A→B vs B→A en simple contención.
- **Sobrecarga transaccional en los DAOs.** Cada DAO expone un método
  autónomo (abre y cierra su `Connection`) y una variante que recibe una
  `Connection` externa y NO la cierra. La segunda permite que el servicio
  componga varias operaciones bajo una misma transacción.
- **Encapsulación reforzada en `Cuenta`.** El campo `saldo` no tiene setter
  público: solo se modifica vía `aplicarDebito` / `aplicarCredito`, que
  validan saldo, monto y estado activo antes de mutar.
- **Aislamiento de tests.** El `surefire` pasa
  `POSTGRES_DB=banco_test` como env var, y `sql/02-test-db.sql` define ese
  schema sin datos semilla. Cada test trunca y siembra sus propios datos.

## Configuración

Para desarrollo local no necesitas configurar nada: `docker compose up -d`
funciona con valores por defecto definidos tanto en `docker-compose.yml`
como en `src/main/resources/application.properties`.

Si quieres personalizar las credenciales (por ejemplo, para no usar
`banco/banco/banco` en una demo pública), copia la plantilla y edítala:

```bash
cp .env.example .env
# edita .env con tus valores
```

Tanto Compose como la aplicación Java leen las mismas variables:

| Variable            | Default | Uso                                |
|---------------------|---------|------------------------------------|
| `POSTGRES_DB`       | `banco` | Nombre de la base de datos         |
| `POSTGRES_USER`     | `banco` | Usuario de Postgres                |
| `POSTGRES_PASSWORD` | `banco` | Contraseña                         |

El archivo `.env` está en `.gitignore` y nunca se commitea; `.env.example`
sirve solo como plantilla.

Postgres queda expuesto en el host en el puerto **15432** (saltamos fuera
del rango 543x para evitar choques con otros Postgres locales). Para
inspeccionar la base con un cliente externo:

```bash
psql -h localhost -p 15432 -U banco -d banco
```

## Verificación rápida

```bash
docker compose up -d

# Smoke test de la capa de datos: lista clientes + cuentas + transacciones,
# imprime intereses calculados polimórficamente (0 para CORRIENTE,
# saldo*0.005 para AHORRO).
mvn -q compile exec:java -Dexec.mainClass=cl.rvasquez.banco.DemoDAO

# Suite de tests contra banco_test (la BD `banco` no se toca).
# Debe imprimir: Tests run: 9, Failures: 0, Errors: 0 — BUILD SUCCESS
mvn test
```

## Origen académico

Proyecto basado en las tres asignaturas de Java cursadas en Duoc UC
(Analista Programador Computacional):

- Fundamentos de Programación en Java
- Desarrollo Orientado a Objetos I
- Desarrollo Orientado a Objetos II
