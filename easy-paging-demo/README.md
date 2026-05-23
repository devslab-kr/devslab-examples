# easy-paging-demo

**English** · [한국어](README.ko.md)

Runnable example for [`easy-paging-spring-boot-starter`](https://github.com/devslab-kr/easy-paging-spring-boot-starter) — annotation-driven pagination for Spring Boot + MyBatis.

This demo wires the starter into a minimal Spring Boot app with an in-memory H2 database, 137 seeded report rows, and a single `@AutoPaginate`-annotated controller. No external services, no database setup — clone, run, curl.

## Prerequisites

- JDK 21+
- Nothing else. H2 runs in-memory, dependencies download on first build.

## Run

```bash
cd easy-paging-demo
./gradlew bootRun
```

The app comes up on `http://localhost:8080`. The H2 database is created in-memory and pre-seeded with 137 `reports` rows on every startup.

## Try it

### First page (default Spring Data 0-based numbering)
```bash
curl 'http://localhost:8080/reports?page=0&size=5'
```
```json
{
  "content": [ { "id": 1, "title": "Report #1", "createdAt": "..." }, ... ],
  "page": 0,
  "size": 5,
  "totalElements": 137,
  "totalPages": 28,
  "first": true,
  "last": false,
  "empty": false
}
```

### Sorting (validated against SQL injection)
```bash
# Multi-column sort, descending by createdAt then ascending by id
curl 'http://localhost:8080/reports?page=0&size=5&sort=createdAt,desc&sort=id,asc'

# Injection attempts are rejected with HTTP 400
curl -i 'http://localhost:8080/reports?sort=id;DROP%20TABLE%20reports'
```

### Page-size clamping
```bash
# Controller declares @AutoPaginate(maxSize = 50)
# Asking for 9999 is clamped silently to 50
curl 'http://localhost:8080/reports?page=0&size=9999' | jq '.size, .content | length'
# → 50
# → 50
```

### Out-of-range pages
```bash
# Page 999 doesn't exist. With reasonable=true (default), the starter clamps to the last page
curl 'http://localhost:8080/reports?page=999&size=20' | jq '.page, .empty'
# → 6  (the last page index for 137/20)
# → false
```

## What to read

The interesting parts, in order:

| File | Why |
| --- | --- |
| `build.gradle.kts` | the only dependency the demo adds beyond `spring-boot-starter-web` is `kr.devslab:easy-paging-spring-boot-starter:3.0.0` |
| `report/ReportController.java` | the entire pagination contract is the `@AutoPaginate(maxSize = 50)` annotation and the `PageResponse<Report>` return type |
| `report/ReportMapper.java` + `resources/mapper/ReportMapper.xml` | plain `SELECT` — no `LIMIT`, no `OFFSET`, no `COUNT`. The aspect injects all of that at runtime |
| `resources/application.yml` | `easy-paging` global caps and defaults |

## Advanced: replacing the response envelope

Many teams have a company-wide envelope shape — something like `{ ok, data, meta: { page, size, total, pages } }` — that every paginated endpoint is supposed to return. The starter supports two ways to swap the default `PageResponse` for your own shape. This demo includes both, side by side, so you can compare the JSON on the wire and pick the pattern that fits your codebase.

The custom envelope used here is the [`CompanyPage`](src/main/java/kr/devslab/examples/easypaging/envelope/CompanyPage.java) record:

```json
{
  "ok": true,
  "data": [ { "id": 1, "title": "Report #1", "createdAt": "..." }, ... ],
  "meta": { "page": 0, "size": 5, "total": 137, "pages": 28 }
}
```

### Pattern 1 — custom return type + static factory (recommended)

The controller declares its return type as `CompanyPage<Report>` and calls `CompanyPage.from(...)` explicitly:

```bash
curl 'http://localhost:8080/reports/company?page=0&size=5'
```

See [`CompanyPageReportController`](src/main/java/kr/devslab/examples/easypaging/report/CompanyPageReportController.java) — the method body is a single `CompanyPage.from(reports.findAll(), pageable)` call. The `@AutoPaginate` aspect still handles PageHelper setup, sort validation, and size clamping; it just doesn't construct the envelope.

### Pattern 2 — `Object` return + `PageResponseFactory` bean

The controller declares its return type as `Object` and hands back a raw `List<Report>`. A [`PageResponseFactory`](src/main/java/kr/devslab/examples/easypaging/envelope/CompanyEnvelopeConfig.java) bean tells the aspect how to wrap that list into the company envelope:

```bash
curl 'http://localhost:8080/reports/auto-envelope?page=0&size=5'
```

Same JSON output as the `/reports/company` endpoint — the wire shape is identical, the code path is different. See [`AutoEnvelopeReportController`](src/main/java/kr/devslab/examples/easypaging/report/AutoEnvelopeReportController.java).

### Pick one — trade-offs

| | Pattern 1 (custom type + `.from()`) | Pattern 2 (`Object` + factory bean) |
| --- | --- | --- |
| Type safety | full — return type is `CompanyPage<Report>` | none — return type is `Object` |
| Wiring per endpoint | each controller calls `CompanyPage.from(...)` | none — factory defined once |
| Mixing envelopes in the same app | trivial — opt in per endpoint | hard — factory affects every `Object`/`List` return |
| Testing | static method, no Spring needed | requires `@SpringBootTest` (bean must be in context) |
| Best when | one or two endpoints want a custom shape | every paginated endpoint must use the same shape |

The default `/reports` endpoint still returns the starter's `PageResponse<Report>` — explicit `PageResponse<T>` (and `CompanyPage<T>`) return types pass through the aspect untouched, so registering the factory bean doesn't affect them. [`CustomEnvelopeTest`](src/test/java/kr/devslab/examples/easypaging/CustomEnvelopeTest.java) asserts that property end-to-end.

The starter's full feature set (keyset pagination, WebFlux/R2DBC support) is covered by sibling demos in this repo — see the [top-level README](../README.md) for the index.

## Verify the build

```bash
./gradlew build
```

Runs two test classes:
- `ReportControllerTest` — boots the app, hits `/reports`, asserts the default pagination envelope shape and `maxSize` clamping.
- `CustomEnvelopeTest` — exercises `/reports/company` and `/reports/auto-envelope`, asserts both produce the same `CompanyPage` JSON shape, and verifies that the `PageResponseFactory` bean does not bleed into `/reports` (which still returns the default `PageResponse`).
