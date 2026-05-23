# easy-paging-demo

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
| `build.gradle.kts` | the only dependency the demo adds beyond `spring-boot-starter-web` is `kr.devslab:easy-paging-spring-boot-starter:0.4.0` |
| `report/ReportController.java` | the entire pagination contract is the `@AutoPaginate(maxSize = 50)` annotation and the `PageResponse<Report>` return type |
| `report/ReportMapper.java` + `resources/mapper/ReportMapper.xml` | plain `SELECT` — no `LIMIT`, no `OFFSET`, no `COUNT`. The aspect injects all of that at runtime |
| `resources/application.yml` | `easy-paging` global caps and defaults |

The starter's full feature set (keyset pagination, custom response shapes, WebFlux/R2DBC support) is documented at **[easy-paging.devslab.kr](https://easy-paging.devslab.kr/)** — this demo intentionally covers only the basics. Follow-up demos under this repo will add the advanced scenarios.

## Verify the build

```bash
./gradlew build
```

Runs the smoke test in `ReportControllerTest`, which boots the app, hits `/reports`, and asserts the pagination envelope shape and `maxSize` clamping.
