# api-log-jpa-demo

**English** · [한국어](README.ko.md)

> ✨ **JPA backend.** Servlet + JPA + PostgreSQL. R2DBC variant: [`api-log-r2dbc-demo`](../api-log-r2dbc-demo/). MyBatis variant: [`api-log-mybatis-demo`](../api-log-mybatis-demo/).

Runnable example for [`api-log`](https://github.com/devslab-kr/api-log) (modules `api-log-core` + `api-log-jpa`, v3.0.0) — outbound HTTP audit logging into a PostgreSQL JSONB-backed `api_log` table.

Every call made through the auto-configured `RestApiClientUtil` publishes three lifecycle events (`INITIATED` → `SUCCESS` or `ERROR`) which the api-log listener writes asynchronously into the `api_log` table. The caller doesn't wait for the write; the audit row is correlated to the call by `request_id`.

This demo is **self-contained** — the same Spring Boot app exposes both the upstream service that responds and the client controller that calls it, so a single `bootRun` exercises the full lifecycle without needing a second process.

## Prerequisites

- JDK 21+
- **Docker** (Docker Desktop or any Docker-compatible runtime)
- That's it. No local Postgres install. No psql client needed.

## Run

```bash
cd api-log-jpa-demo

# Start PostgreSQL in the background. The compose file binds 5432 on localhost.
docker compose up -d db

# Boot the app. The api-log starter's BUILTIN schema initializer creates
# the api_log table on first start (idempotent — IF NOT EXISTS).
./gradlew bootRun
```

The app comes up on `http://localhost:8080`. When you're done:

```bash
docker compose down       # stop the container
docker compose down -v    # ...and drop the volume (clean slate next time)
```

## Try it

### 1. Happy path: GET, then read the audit trail

```bash
# Self-loopback GET — client side calls upstream side, both in this JVM.
curl 'http://localhost:8080/client/widgets/123'
# → {"id":123,"name":"Widget-123","sku":"SKU-123","price":1230}

# api-log writes INITIATED + SUCCESS rows for that call (asynchronously).
curl 'http://localhost:8080/api-log/recent' | jq '.[0:4] | .[] | {eventType, requestId, endpoint, statusCode}'
# →
#   {"eventType":"SUCCESS",   "requestId":"...uuid...", "endpoint":"http://localhost:8080/upstream/widgets/123", "statusCode":200}
#   {"eventType":"INITIATED", "requestId":"...same...", "endpoint":"http://localhost:8080/upstream/widgets/123", "statusCode":null}
```

### 2. POST: payload is preserved as JSONB

```bash
curl -X POST 'http://localhost:8080/client/widgets' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Gizmo","sku":"SKU-Gizmo","price":19.99}'

# The serialized request body is stored verbatim in api_log.payload (JSONB).
curl 'http://localhost:8080/api-log/recent' | jq '.[0:2] | .[] | {eventType, payload}'
```

### 3. Error path: 5xx becomes an ERROR row

```bash
# /upstream/widgets/999 is wired to throw 500 on purpose.
curl -i 'http://localhost:8080/client/widgets/999'
# → HTTP/1.1 500 ...

# api-log captures the error with the upstream's body in api_log.error_message.
curl 'http://localhost:8080/api-log/by-event/ERROR' | jq '.[0] | {eventType, statusCode, errorMessage}'
```

### 4. Explicit requestId: correlate a logical group of calls

```bash
# The /with-request-id endpoint passes requestId="demo-fixed-rid" through
# the core send(HttpMethod, ApiRequest) overload. In a real retry scenario
# you'd reuse the same id across all attempts so the full sequence groups
# under one key.
curl -X POST 'http://localhost:8080/client/widgets/with-request-id/123'

curl 'http://localhost:8080/api-log/by-request/demo-fixed-rid' | jq '. | length'
# → 2 (INITIATED + SUCCESS)
```

## Architecture

```
+--------------+         +--------------------+         +----------------------+
|  curl /      |  HTTP   |  ClientController  |  HTTP   |  UpstreamController  |
|  test client | ------> |  (this app)        | ------> |  (same JVM)          |
+--------------+         +---------+----------+         +----------------------+
                                   |
                                   | calls
                                   v
                         +-----------------------+
                         |   RestApiClientUtil   |   (auto-configured by api-log-core)
                         +---------+-------------+
                                   |
                                   | publishes events synchronously
                                   v
                         +------------------------------+
                         |  ApplicationEventPublisher   |   ApiCallInitiatedEvent / SuccessEvent / ErrorEvent
                         +---------+--------------------+
                                   |
                                   | (delivered to listener on @Async executor — virtual thread on JDK 21+)
                                   v
                         +-------------------+
                         |  ApiEventListener |   (api-log-core)
                         +---------+---------+
                                   |
                                   | writer.writeInitiated() / writeSuccess() / writeError()
                                   v
                         +-------------------+
                         |  JpaApiLogWriter  |   (api-log-jpa, @Transactional REQUIRES_NEW)
                         +---------+---------+
                                   |
                                   v
                         +---------------------+
                         |  PostgreSQL api_log |   (BUILTIN initializer created the table on boot)
                         +---------------------+
```

The HTTP caller never waits on the database. The `@Async` hop on the listener and `REQUIRES_NEW` propagation on the writer together guarantee that:

- An exception in the writer can't roll back the consumer's transaction.
- A rollback in the consumer's transaction can't erase already-logged rows for calls that already went out.

## Files of interest

| File | Why |
| --- | --- |
| `build.gradle.kts` | declares `kr.devslab:api-log-core:3.0.0` + `kr.devslab:api-log-jpa:3.0.0`; the JPA backend pulls in `spring-boot-starter-data-jpa` and `org.postgresql:postgresql` |
| `src/main/resources/application.yml` | `api.log.schema.management=BUILTIN` — the starter creates the `api_log` table on boot. Set `NONE` (apply DDL yourself) or `FLYWAY` (let Flyway own it) for real apps |
| `widget/ClientController.java` | one constructor injection of `RestApiClientUtil`; every method funnels through it so every outbound call is logged uniformly |
| `widget/UpstreamController.java` | the "external service" half of the self-loopback — same JVM, separate routes — including a deliberate 500 on id=999 |
| `widget/ApiLogController.java` | read-only view of `api_log` via the auto-registered `ApiLogRepository`; the starter doesn't ship a reporting API, so this is just enough to demo |
| `src/test/.../ApiLogLifecycleIT.java` | full lifecycle integration test: real HTTP on a random port, Awaitility polls for the async writes, Testcontainers Postgres via `@ServiceConnection` |

## How testing works (Docker Compose vs Testcontainers)

There are two Docker paths in this demo, deliberately separate:

| Path | When it runs | What it uses |
| --- | --- | --- |
| `docker compose up -d db` | When a **human** wants to `bootRun` against a long-lived DB | `docker-compose.yml` in this directory — published port 5432, named volume `pgdata` |
| Testcontainers in `ApiLogLifecycleIT` | When `./gradlew test` runs (locally or in CI) | An **ephemeral** `postgres:16-alpine` container on a random port, started + torn down per test class |

The integration test uses Spring Boot 3.1+'s [`@ServiceConnection`](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html#testing.testcontainers.service-connections), which auto-rewires `spring.datasource.url` to the Testcontainers instance — no `application-test.yml`, no `@DynamicPropertySource`.

## Production notes

- **`api_log` schema**: the demo uses `api.log.schema.management=BUILTIN`, which runs the bundled `V1.0__create_api_log.sql` on every boot (idempotent via `CREATE TABLE IF NOT EXISTS`). **For production**, prefer `FLYWAY` (the starter appends `classpath:db/api-log` to Flyway's locations so the migration gets recorded in `flyway_schema_history` alongside your own) or `NONE` (apply the DDL yourself, e.g. via Liquibase or a managed schema pipeline).
- **`RestApiClientUtil`**: the starter exposes a single auto-configured `RestClient` bean and wraps it. If your app already has its own `RestClient` configured (auth headers, custom timeouts, mTLS), declare it as `@Bean` and the starter's `@ConditionalOnMissingBean` will defer to yours — `RestApiClientUtil` will then use your client and still emit the events.
- **Async listener**: events are processed on an `ApiLogEvent-` executor — virtual threads when `spring.threads.virtual.enabled=true` (recommended on JDK 21+), platform-thread pool otherwise. Each write is wrapped in `@Retryable(maxAttempts = 3)` so a transient DB blip doesn't drop a log row.

## Verify the build

```bash
./gradlew build
```

This runs `ApiLogLifecycleIT` against an ephemeral Testcontainers Postgres. First run pulls the `postgres:16-alpine` image (~80MB); subsequent runs use the cached image and complete in seconds.
