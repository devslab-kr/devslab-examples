# api-log-mybatis-demo

**English** · [한국어](README.ko.md)

> ✨ **MyBatis backend.** This demo wires the [`api-log`](https://github.com/devslab-kr/api-log) starter to **MyBatis**. For the JPA backend variant see [`api-log-jpa-demo`](../api-log-jpa-demo/); for the reactive/R2DBC variant see [`api-log-r2dbc-demo`](../api-log-r2dbc-demo/).

Runnable example for [`api-log`](https://github.com/devslab-kr/api-log) — event-driven API call logging into a PostgreSQL `api_log` JSONB table, written through a MyBatis mapper.

## What this demo shows

- Every outbound HTTP call made via `RestApiClientUtil` produces lifecycle events (INITIATED → SUCCESS or INITIATED → ERROR).
- The starter's listener consumes those events **on a separate thread** and persists them through the auto-registered `ApiLogMapper`.
- The `api_log` table is created automatically at startup (`api.log.schema.management=BUILTIN`).
- The demo is single-process: the same app exposes both `/upstream/widgets` (the "external service") and `/client/widgets` (the caller). The caller's base URL points back at `localhost`, so the whole pipeline is observable without a second service.

## Prerequisites

- JDK 21+
- **Docker** (Docker Desktop or any Docker-compatible runtime)

## Get just this demo

Each demo is a standalone Gradle project, so you can grab this one folder without
cloning the whole `devslab-examples` repo.

**With git (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set api-log-mybatis-demo
cd api-log-mybatis-demo
```

**Without git (folder only):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/api-log-mybatis-demo
cd api-log-mybatis-demo
```

## Run

```bash
cd api-log-mybatis-demo
docker compose up -d db
./gradlew bootRun
```

The app comes up on `http://localhost:8080`. When you're done:

```bash
docker compose down       # stop the container
docker compose down -v    # ...and drop the volume (clean slate next time)
```

## Try it

```bash
# Happy path - produces INITIATED + SUCCESS rows in api_log
curl 'http://localhost:8080/client/widgets/123'

# Async variant - same logging, called via CompletableFuture
curl 'http://localhost:8080/client/widgets/123/async'

# Error path - the upstream throws on id=999, producing an ERROR row
curl -i 'http://localhost:8080/client/widgets/999'

# POST - body shows up in api_log.payload (as JSON text, since payload is JSONB)
curl -X POST 'http://localhost:8080/client/widgets' \
     -H 'Content-Type: application/json' \
     -d '{"name":"Sprocket-7","sku":"SKU-7","price":19.99}'

# Explicit requestId - the typical retry-correlation pattern
curl -X POST 'http://localhost:8080/client/widgets/with-request-id/123'
curl 'http://localhost:8080/api-log/by-request/demo-fixed-rid' | jq

# Read the log
curl 'http://localhost:8080/api-log/recent'           | jq
curl 'http://localhost:8080/api-log/by-event/SUCCESS' | jq
curl 'http://localhost:8080/api-log/by-event/ERROR'   | jq
```

## Architecture

```
                       (this same JVM)
                  +---------------------+
   curl --GET-->  | ClientController    |
                  |   /client/widgets   |
                  +---------------------+
                            |
                            | uses
                            v
                  +---------------------+
                  | RestApiClientUtil   |  (api-log-core)
                  +---------------------+
                       |           |
              HTTP --> |           | --> ApplicationEventPublisher
                       v                         |
              +-----------------+                | ApiCallInitiatedEvent
              | UpstreamCtrl    |                | ApiCallSuccessEvent
              | /upstream/...   |                | ApiCallErrorEvent
              +-----------------+                |
                                                 v
                                       +--------------------+
                                       | ApiEventListener   |  (api-log-core,
                                       |  (async thread)    |   listener thread)
                                       +--------------------+
                                                 |
                                                 v
                                       +--------------------+
                                       | MybatisApiLogWriter|  (api-log-mybatis)
                                       +--------------------+
                                                 |
                                                 v
                                       +--------------------+
                                       | ApiLogMapper       |  (MyBatis @Mapper,
                                       |   INSERT api_log   |   auto-wired)
                                       +--------------------+
                                                 |
                                                 v
                                       +--------------------+
                                       |  PostgreSQL JSONB  |
                                       +--------------------+
```

## What to read

| File | Why |
| --- | --- |
| `build.gradle.kts` | adds `api-log-core` + `api-log-mybatis` + `mybatis-spring-boot-starter:4.0.1` — that's the whole starter wiring |
| `src/main/resources/application.yml` | `api.log.enabled` + `api.log.schema.management=BUILTIN` + `mybatis.mapper-locations` |
| `widget/ClientController.java` | every kind of call (sync/async/typed/POST/PUT/DELETE/explicit-requestId) routed through `RestApiClientUtil` |
| `widget/ApiLogController.java` | reader endpoints — uses the starter's `ApiLogMapper.findByRequestId` AND the demo's own `ApiLogQueryMapper` for "recent N" / "by event type" |
| `mapper/ApiLogQueryMapper.xml` | shows the `payload::text AS payload` cast pattern needed to read JSONB columns into MyBatis `String` properties |
| `src/test/.../ApiLogLifecycleIT.java` | end-to-end test against a real Postgres Testcontainer — uses Awaitility to wait on the async listener |

## How testing works (Docker Compose vs Testcontainers)

Same two-path setup as the other demos in this repo:

| Path | When it runs | What it uses |
| --- | --- | --- |
| `docker compose up -d db` | When a **human** wants to `bootRun` against a long-lived DB | `docker-compose.yml` in this directory — published port 5432, named volume `pgdata` |
| Testcontainers in `ApiLogLifecycleIT` | When `./gradlew test` runs (locally or in CI) | An **ephemeral** `postgres:16-alpine` container on a random port |

`@ServiceConnection` (Spring Boot 3.1+) auto-rewires `spring.datasource.url` to the Testcontainers instance — no `application-test.yml`, no `@DynamicPropertySource`.

## Production notes

A few things in this demo are tuned for "easy to run" and would be different in a real service:

- **Schema management.** `api.log.schema.management=BUILTIN` runs the starter's bundled DDL on every boot (idempotent — `CREATE TABLE IF NOT EXISTS`). In production set it to `EXTERNAL` and let Flyway or Liquibase own the migration.
- **Mapper scan.** The demo's `@MapperScan` covers only `kr.devslab.examples.apilogmybatis`. **Do not** add `kr.devslab.apilog.mybatis.mapper` to your scan — the starter's auto-config already registers `ApiLogMapper` and scanning it twice will produce conflicting bean definitions.
- **`map-underscore-to-camel-case`** in `application.yml` lets the demo's custom `ApiLogQueryMapper` map snake_case columns onto the camelCase fields of `ApiLogRow` without spelling each one out in the resultMap. The starter's own mapper uses explicit `AS camelCase` aliases for the same reason — pick whichever pattern fits your codebase.
- **Self-loopback.** Real apps point `api-log-demo.upstream-base-url` at a different service. The loopback exists so the demo runs in a single process.

## Verify the build

```bash
./gradlew build
```

This compiles, runs `ApiLogLifecycleIT` against an ephemeral Testcontainers Postgres, and produces a runnable jar. First run pulls `postgres:16-alpine` (~80 MB); subsequent runs use the cached image.
