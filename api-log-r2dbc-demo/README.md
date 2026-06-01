# api-log-r2dbc-demo

**English** · [한국어](README.ko.md)

> ✨ **R2DBC backend (Reactive / WebFlux).** This demo runs against the R2DBC variant of [`api-log`](https://github.com/devslab-kr/api-log) (`api-log-core` + `api-log-r2dbc`, both `3.0.0`). For the blocking variants see the sibling demos: [`api-log-jpa-demo`](../api-log-jpa-demo/) and [`api-log-mybatis-demo`](../api-log-mybatis-demo/).

Production-flavoured runnable example for [`api-log`](https://github.com/devslab-kr/api-log) with a fully **non-blocking** HTTP path: WebFlux on the front, `ReactiveApiClientUtil` for the outbound call, and the R2DBC writer persisting into the `api_log` JSONB table via R2DBC's reactive `ConnectionFactory`.

## What this demo shows

- `api-log` writes through R2DBC's reactive `ConnectionFactory` — no JDBC connection pool, no platform-thread blocking for the audit-log write path
- The entire HTTP path (inbound WebFlux request → `ReactiveApiClientUtil` outbound call → response back to the client) stays on Reactor's event loop
- Same `api_log` table shape as the JPA / MyBatis backends — same JSONB columns (`payload`, `response`, `error_message`), same `event_type` lifecycle (`INITIATED` → `SUCCESS` | `ERROR`), so audit consumers can read uniformly across backends
- A **self-loopback** demo topology: one app exposes both the upstream and the client; the client calls the upstream over HTTP through `ReactiveApiClientUtil`, which is enough for `api-log` to write a full INITIATED + SUCCESS / ERROR pair per call
- How to **read** the table from an R2DBC app — the `api-log-r2dbc` artifact only ships a writer; this demo brings its own `DatabaseClient`-backed reader so the read endpoints (`/api-log/recent`, `/api-log/by-request/{requestId}`, `/api-log/by-event/{eventType}`) can show what got persisted

## Prerequisites

- JDK 21+
- **Docker** (Docker Desktop or any Docker-compatible runtime)
- That's it. No local Postgres install. No psql client needed.

## Get just this demo

Each demo is a standalone Gradle project, so you can grab this one folder without
cloning the whole `devslab-examples` repo.

**With git (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set api-log-r2dbc-demo
cd api-log-r2dbc-demo
```

**Without git (folder only):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/api-log-r2dbc-demo
cd api-log-r2dbc-demo
```

## Quickstart

```bash
cd api-log-r2dbc-demo

# Start PostgreSQL in the background. The compose file binds 5432 on localhost.
docker compose up -d db

# Boot the app. ApiLogR2dbcSchemaInitializer creates the api_log table on
# startup (api.log.r2dbc.schema.enabled=true, which is the default).
./gradlew bootRun
```

The app comes up on `http://localhost:8080`. Try it:

```bash
# Happy GET — produces an INITIATED + SUCCESS pair in api_log
curl 'http://localhost:8080/client/widgets/123'

# Error path — produces an INITIATED + ERROR pair
curl -i 'http://localhost:8080/client/widgets/999'

# POST — the request body is captured into the payload JSONB column
curl -X POST -H 'Content-Type: application/json' \
  -d '{"name":"Hyperbolic Cog","sku":"SKU-COG-42"}' \
  'http://localhost:8080/client/widgets'

# Explicit requestId correlation — both rows share request_id=demo-fixed-rid
curl -X POST 'http://localhost:8080/client/widgets/with-request-id/123'

# Inspect what got written
curl 'http://localhost:8080/api-log/recent'
curl 'http://localhost:8080/api-log/by-request/demo-fixed-rid'
curl 'http://localhost:8080/api-log/by-event/SUCCESS'
curl 'http://localhost:8080/api-log/by-event/ERROR'
```

When you're done:

```bash
docker compose down       # stop the container
docker compose down -v    # ...and drop the volume (clean slate next time)
```

## Architecture

```
caller (curl / browser)
        │
        ▼  HTTP (Netty event loop, non-blocking)
┌──────────────────────────────────────────────────┐
│ WebFlux                                          │
│   ClientController                               │
│     └─ ReactiveApiClientUtil.getTyped(...)       │
│           └─ WebClient → /upstream/widgets/{id}  │
│                 │                                │
│                 └─ ApplicationEventPublisher     │
│                       (INITIATED → SUCCESS|ERROR)│
└─────────────────────────────────────┬────────────┘
                                      ▼
                              ApiEventListener
                                      │
                                      ▼
                          R2dbcApiLogWriter
                                      │
                                      ▼   r2dbc:postgresql://...
                              DatabaseClient
                                      │
                                      ▼
                                 PostgreSQL
                                  api_log
                          (id, event_type, request_id,
                           endpoint, payload, response,
                           status_code, error_message,
                           timestamp, retry_count, is_retry)
```

The audit-log write doesn't block the response — `ApplicationEventPublisher` fans the event out to the listener, and the listener performs the R2DBC insert on its own subscriber. The HTTP response to the caller returns as soon as the upstream's response is back.

## Files of interest

| File | Why |
| --- | --- |
| `build.gradle.kts` | adds `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `api-log-core` + `api-log-r2dbc`, and both the R2DBC and JDBC PostgreSQL drivers (the JDBC one is only for Testcontainers' `@ServiceConnection`) |
| `src/main/resources/application.yml` | `spring.r2dbc.url` instead of `spring.datasource.url`; `api.log.r2dbc.schema.enabled=true` instead of `api.log.schema.management=BUILTIN` — R2DBC and the blocking backends use different property names |
| `widget/ClientController.java` | uses `ReactiveApiClientUtil` — `getTyped`, `postTyped`, `putTyped`, `delete`, and a `send(method, ApiRequest)` variant for the explicit-requestId correlation case |
| `widget/UpstreamController.java` | the self-loopback target; `id == 999` returns 5xx so the error path can be exercised |
| `widget/ApiLogReader.java` | `DatabaseClient`-backed reader — the `api-log-r2dbc` artifact ships only a writer, so the demo brings its own SELECT. Casts JSONB columns to `text` so they bind into `String` fields cleanly |
| `src/test/.../ApiLogLifecycleIT.java` | `@Testcontainers` + `@ServiceConnection` for R2DBC + `WebTestClient` for WebFlux + Awaitility to wait out the async listener — five tests covering happy, error, payload preservation, requestId correlation, and "schema is queryable" |

## Production notes

- The reactive schema initializer (`api.log.r2dbc.schema.enabled`, default `true`) creates the `api_log` table on startup if it doesn't exist. **It's fine for a demo, not great for production.** R2DBC has no first-class migration tooling — for a real deployment, run [Flyway](https://flywaydb.org/) against a **separate JDBC connection** at boot time (Flyway works fine alongside an R2DBC runtime — Spring Boot's `flyway.url` / `flyway.user` / `flyway.password` properties are independent of `spring.r2dbc.*`) and disable the reactive initializer with `api.log.r2dbc.schema.enabled=false`.
- Note that the R2DBC backend uses `api.log.r2dbc.schema.enabled`, **not** `api.log.schema.management`. The latter property only applies to the JPA + MyBatis backends; mixing them up is a common first-time-user trap.
- `ReactiveApiClientUtil` builds on top of `WebClient`, which is auto-configured with sensible defaults but no timeout. For production, configure a connect / response timeout on the `WebClient.Builder` your app gets — `ReactiveApiClientUtil`'s constructor takes a `WebClient`, so you can hand it one you've already customized.

## How testing works

`./gradlew test` doesn't read `docker-compose.yml`. Testcontainers spins up an ephemeral `postgres:16-alpine` on a random port and `@ServiceConnection` on the `PostgreSQLContainer` produces both:

- a `DataSourceConnectionDetails` (JDBC, for anything that asks)
- an `R2dbcConnectionDetails` that rewires `spring.r2dbc.url` to the container

…so the app's R2DBC runtime points at the container without an `application-test.yml` or a `@DynamicPropertySource`. CI runs the same way: an Ubuntu runner already has Docker, so the IT suite runs against a real Postgres with no setup.

## Verify the build

```bash
./gradlew build
```

First run pulls the `postgres:16-alpine` image (~80MB); subsequent runs use the cached image.
