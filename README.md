# sistema-bancario-java

> Proyecto en construcción. El README definitivo se completa en la Fase 5;
> esta versión inicial documenta solo lo necesario para configurar y correr
> la verificación de conexión de la Fase 0.

Aplicación de escritorio en Java + Swing que simula la gestión de clientes,
cuentas y transferencias bancarias. Proyecto de portafolio basado en las tres
asignaturas de Java de Duoc UC.

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
| `POSTGRES_DB`       | `banco` | Nombre de la base de datos          |
| `POSTGRES_USER`     | `banco` | Usuario de Postgres                 |
| `POSTGRES_PASSWORD` | `banco` | Contraseña                          |

El archivo `.env` está en `.gitignore` y nunca se commitea; `.env.example`
sirve solo como plantilla.

## Verificación de Fase 0

```bash
docker compose up -d
mvn -q compile exec:java
# Debe imprimir: Conexión OK
```

Postgres queda expuesto en el host en el puerto **15432** (saltamos fuera
del rango 543x para evitar choques con otros Postgres locales). Para
inspeccionar la base con un cliente externo:

```bash
psql -h localhost -p 15432 -U banco -d banco
```
