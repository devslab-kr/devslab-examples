# easy-paging-sb4-keyset-demo

**English** · [한국어](README.ko.md)

> ✨ **Spring Boot 4 line.** This demo runs against the active `0.5.x` line of `easy-paging-spring-boot-starter` (Spring Boot 4 / Spring Framework 7 / Jackson 3). For the Spring Boot 3.3–3.5 maintenance equivalent, see [`easy-paging-keyset-demo`](../easy-paging-keyset-demo/).

Cursor (keyset) pagination example for [`easy-paging-spring-boot-starter`](https://github.com/devslab-kr/easy-paging-spring-boot-starter) — the strategy you want for unbounded time-series streams (logs, audit events, location pings) where `OFFSET` would slow down with depth and `COUNT(*)` is wasted work.

Companion to [`easy-paging-demo`](../easy-paging-demo/) — which covers traditional offset pagination — but with a different annotation, return type, and `WHERE`-clause pattern. Same H2 in-memory database, no external services.

## Prerequisites

- JDK 21+
- Nothing else.

## Run

```bash
cd easy-paging-keyset-demo
./gradlew bootRun
```

The app comes up on `http://localhost:8080`. H2 is seeded with **300 location pings** for one fixed worker UUID (`00000000-0000-0000-0000-000000000001`).

## Try it

### First page (no cursor)

```bash
curl 'http://localhost:8080/locations?workerId=00000000-0000-0000-0000-000000000001&size=10'
```

```json
{
  "content": [
    { "id": 300, "workerId": "00000000-...", "time": "2026-05-23T05:00:00Z", "lat": 37.5965, "lng": 127.0080 },
    { "id": 299, ... },
    ...
  ],
  "size": 10,
  "nextCursor": "eyJrIjp7InRpbWUiOiIyMDI2LTA1LTIzVDA0OjUxOjAwWiIsImlkIjoyOTF9LCJkIjoiRk9SV0FSRCJ9",
  "prevCursor": null,
  "hasNext": true,
  "hasPrev": false
}
```

The `content` is ordered by `(time DESC, id DESC)` so the newest row appears first. The `nextCursor` is a Base64-encoded JSON payload — it's safe to inspect when no signing secret is set (see below).

### Walk the stream

Pass `nextCursor` back as `?cursor=…`:

```bash
curl 'http://localhost:8080/locations?workerId=00000000-0000-0000-0000-000000000001&size=10&cursor=<nextCursor-from-previous-response>'
```

Keep going until `hasNext` is `false`. With 300 seeded rows and `size=10`, that's exactly 30 pages — and every `id` from 1 to 300 will appear exactly once across the walk. (The `LocationControllerTest` asserts this property programmatically.)

### Size clamping

```bash
# Controller has @KeysetPaginate(maxSize = 200) — ?size=9999 clamps to 200
curl 'http://localhost:8080/locations?workerId=00000000-0000-0000-0000-000000000001&size=9999' | jq '.size'
# → 200
```

## Cursor signing (read this before production)

By default the demo runs with **no cursor signing**:

```yaml
easy-paging:
  keyset:
    cursor-secret: ${EASY_PAGING_CURSOR_SECRET:}
```

This is fine for local exploration — you can decode the cursor with `base64 -d` and see exactly what's in it, which makes the moving parts obvious. **But in production a missing secret is a vulnerability**: a malicious client can forge a cursor to seek past rows that they shouldn't see (e.g. by tampering with a tenant key embedded in the cursor).

Run the demo with a secret to see signed cursors:

```bash
EASY_PAGING_CURSOR_SECRET='a-long-random-string-from-your-secrets-manager' ./gradlew bootRun
```

Signed cursors look the same in normal use, but become `<payload>.<hmac>` and any tampered cursor is rejected.

## What to read

| File | Why |
| --- | --- |
| `build.gradle.kts` | the only dependency added beyond `spring-boot-starter-web` is `kr.devslab:easy-paging-spring-boot-starter:0.4.0` (same as the offset demo) |
| `location/LocationController.java` | the contract: `@KeysetPaginate(keys = {"time", "id"}, ...)` plus a `KeysetRequest` parameter resolved automatically by the starter |
| `location/LocationService.java` | shows the **size + 1** trick (mapper fetches one extra row so `KeysetPage.build` can set `hasNext` correctly) and the `keyExtractor` lambda that becomes the next cursor |
| `location/LocationMapper.java` + `resources/mapper/LocationMapper.xml` | the composite-key seek predicate — `time < ? OR (time = ? AND id < ?)` is what makes the walk stable when timestamps collide |
| `resources/schema.sql` | covering index `(worker_id, time DESC, id DESC)` matches the query's ORDER BY so the page lookup stays O(log N) per page on a real DB |

## Verify the build

```bash
./gradlew build
```

The smoke test boots the app, walks the entire cursor chain across 4 pages, and asserts the **no-overlap / no-gaps** invariant — the property keyset pagination actually has to guarantee.

## How this differs from `easy-paging-demo`

| | `easy-paging-demo` (offset) | `easy-paging-keyset-demo` (this) |
|---|---|---|
| Annotation | `@AutoPaginate` | `@KeysetPaginate(keys = {...})` |
| Return type | `PageResponse<T>` | `KeysetPage<T>` |
| Mapper SQL | plain `SELECT` (no LIMIT/OFFSET — aspect injects) | explicit `WHERE` seek clause + `LIMIT size+1` |
| `totalElements` | yes | no (avoiding `COUNT(*)` is half the point) |
| Best for | finite paginated lists with totals | unbounded streams, append-only tables |
| Performance at depth | slows with page number | constant per page |
