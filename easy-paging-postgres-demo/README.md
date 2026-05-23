# easy-paging-postgres-demo

**English** · [한국어](README.ko.md)

Production-flavoured runnable example for [`easy-paging-spring-boot-starter`](https://github.com/devslab-kr/easy-paging-spring-boot-starter) against a **real PostgreSQL** instead of H2.

The starter works on whatever JDBC database PageHelper supports (Postgres, MySQL, MariaDB, Oracle, …) — this demo proves it end-to-end on the database most teams actually ship with, and shows the Docker Compose + Testcontainers pattern this repo will use for every "external DB" demo from here on.

## Prerequisites

- JDK 21+
- **Docker** (Docker Desktop or any Docker-compatible runtime)
- That's it. No local Postgres install. No psql client needed.

## Run

```bash
cd easy-paging-postgres-demo

# Start PostgreSQL in the background. The compose file binds 5432 on localhost.
docker compose up -d db

# Boot the app. spring.sql.init recreates the schema and seeds 500 products
# on every start, so you always boot into a known state.
./gradlew bootRun
```

The app comes up on `http://localhost:8080`. When you're done:

```bash
docker compose down       # stop the container
docker compose down -v    # ...and drop the volume (clean slate next time)
```

## Try it

```bash
# Default offset pagination
curl 'http://localhost:8080/products?page=0&size=10'

# Sort
curl 'http://localhost:8080/products?page=0&size=10&sort=price,desc'
curl 'http://localhost:8080/products?page=0&size=10&sort=createdAt,desc&sort=id,asc'

# Filter by category — data is seeded into 5 categories, 100 each
curl 'http://localhost:8080/products?category=books&page=0&size=20' | jq '.totalElements'
# → 100

# Page-size clamping (controller declares @AutoPaginate(maxSize = 100))
curl 'http://localhost:8080/products?size=9999' | jq '.size, (.content | length)'
# → 100
# → 100

# Sort-injection attempts get HTTP 400 before they ever reach the database
curl -i 'http://localhost:8080/products?sort=name;DROP%20TABLE%20products'
```

## How testing works (Docker Compose vs Testcontainers)

There are two Docker paths in this demo, deliberately separate:

| Path | When it runs | What it uses |
| --- | --- | --- |
| `docker compose up -d db` | When a **human** wants to `bootRun` against a long-lived DB | `docker-compose.yml` in this directory — published port 5432, named volume `pgdata` |
| Testcontainers in `ProductControllerIT` | When `./gradlew test` runs (locally or in CI) | An **ephemeral** `postgres:16-alpine` container on a random port, started + torn down per test class |

The integration test uses Spring Boot 3.1+'s [`@ServiceConnection`](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html#testing.testcontainers.service-connections), which auto-rewires `spring.datasource.url` to the Testcontainers instance — no `application-test.yml`, no `@DynamicPropertySource`. The compose file's port and the test's port don't conflict because the test never reads `docker-compose.yml`.

This means CI can run `./gradlew build` on a clean Ubuntu runner with no Postgres pre-installed and the integration tests still hit a real Postgres. The runner already has Docker; Testcontainers handles the rest.

## What to read

The interesting parts, in order:

| File | Why |
| --- | --- |
| `docker-compose.yml` | minimal PG service with a healthcheck, so `docker compose up -d` is a one-liner |
| `build.gradle.kts` | adds `org.postgresql:postgresql` (driver) and the `spring-boot-testcontainers` / `org.testcontainers:postgresql` test deps; everything else is identical to the H2 demo |
| `src/test/.../ProductControllerIT.java` | `@Testcontainers` + `@ServiceConnection` is all the wiring needed; no static initializer block, no `@DynamicPropertySource` |
| `src/main/resources/schema.sql` | uses `BIGSERIAL` and a composite index — PG-native, not H2-portable — to show the starter is happy with real PG schema features |
| `src/main/resources/data.sql` | `generate_series(1, 500)` for deterministic seeding (no `random()` for fields tests assert against) |
| `product/ProductController.java` | the contract is one annotation + `Pageable` — same as the H2 demo. The starter doesn't care which database is underneath |

## Migrations (read this if you're copy-pasting into a real app)

This demo uses `spring.sql.init` with a `DROP TABLE IF EXISTS` + `CREATE TABLE` script that runs on every boot. **Don't do that in production.** Use [Flyway](https://flywaydb.org/) or [Liquibase](https://www.liquibase.org/) — both have Spring Boot starters and both coexist with `easy-paging-spring-boot-starter` without any special configuration. `spring.sql.init` is fine for a learning demo where the goal is "always boot into a known state with no setup steps".

## Verify the build

```bash
./gradlew build
```

This runs `ProductControllerIT` against an ephemeral Testcontainers Postgres. First run pulls the `postgres:16-alpine` image (~80MB); subsequent runs use the cached image and complete in seconds.

## How this differs from `easy-paging-demo`

| | `easy-paging-demo` | `easy-paging-postgres-demo` (this) |
|---|---|---|
| Database | H2 in-memory (no external runtime) | Real PostgreSQL (Docker Compose for `bootRun`, Testcontainers for tests) |
| Schema features used | basic table | `BIGSERIAL`, `NUMERIC(10,2)`, composite index, `generate_series` |
| Mapper logic | always `SELECT *` | optional `WHERE category = ?` via `<if>` |
| Test infra | none beyond MockMvc | Testcontainers + `@ServiceConnection` |
| Best for | "show me the starter in 30 seconds" | "show me it works against a production DB and that tests can integrate cleanly" |
