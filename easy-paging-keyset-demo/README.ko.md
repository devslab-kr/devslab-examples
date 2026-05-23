# easy-paging-keyset-demo

[English](README.md) · **한국어**

[`easy-paging-spring-boot-starter`](https://github.com/devslab-kr/easy-paging-spring-boot-starter)의 커서(keyset) 페이지네이션 예제 — `OFFSET`이 깊이에 따라 느려지고 `COUNT(*)`도 낭비인 무한 시계열 스트림 (로그, audit 이벤트, location ping)에 쓰는 전략.

전통적인 offset 페이지네이션을 다루는 [`easy-paging-demo`](../easy-paging-demo/)의 자매 데모 — 어노테이션, 반환 타입, `WHERE` 절 패턴이 다름. 동일한 H2 인메모리 DB, 외부 서비스 없음.

## 전제조건

- JDK 21+
- 그 외 없음.

## 실행

```bash
cd easy-paging-keyset-demo
./gradlew bootRun
```

앱은 `http://localhost:8080`에 뜸. H2는 고정 worker UUID (`00000000-0000-0000-0000-000000000001`)에 대한 **300개 location ping**으로 시드됨.

## 시험해보기

### 첫 페이지 (커서 없음)

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

`content`는 `(time DESC, id DESC)` 순서 — 가장 최근 행이 맨 위. `nextCursor`는 Base64 인코딩된 JSON payload — 서명 시크릿이 없으면 디코딩해서 내용 확인 가능 (아래 참고).

### 스트림 walking

`nextCursor`를 `?cursor=…`로 넘기기:

```bash
curl 'http://localhost:8080/locations?workerId=00000000-0000-0000-0000-000000000001&size=10&cursor=<이전 응답의 nextCursor>'
```

`hasNext`가 `false`가 될 때까지 반복. 300행 / `size=10`이면 정확히 30페이지 — walk 동안 1~300 모든 `id`가 정확히 한 번씩 등장. (`LocationControllerTest`가 이 property를 프로그래매틱하게 검증.)

### 크기 clamping

```bash
# 컨트롤러가 @KeysetPaginate(maxSize = 200) — ?size=9999 → 200으로 clamping
curl 'http://localhost:8080/locations?workerId=00000000-0000-0000-0000-000000000001&size=9999' | jq '.size'
# → 200
```

## 커서 서명 (프로덕션 가기 전에 읽기)

데모는 기본적으로 **커서 서명 없이** 실행:

```yaml
easy-paging:
  keyset:
    cursor-secret: ${EASY_PAGING_CURSOR_SECRET:}
```

로컬 탐색에는 OK — `base64 -d`로 커서 내용을 그대로 볼 수 있어서 동작 원리가 명확해짐. **하지만 프로덕션에서 시크릿 누락은 취약점**: 악의적 클라이언트가 커서를 조작해서 (커서에 박힌 tenant key 등을 변조해서) 보면 안 되는 행을 seek 가능.

서명된 커서로 실행:

```bash
EASY_PAGING_CURSOR_SECRET='a-long-random-string-from-your-secrets-manager' ./gradlew bootRun
```

정상 사용에선 동일해 보이지만 서명된 커서는 `<payload>.<hmac>` 형태가 되고 변조된 커서는 거부됨.

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | `spring-boot-starter-web` 외 추가 의존성은 `kr.devslab:easy-paging-spring-boot-starter:3.0.0` (offset 데모와 동일) |
| `location/LocationController.java` | 계약: `@KeysetPaginate(keys = {"time", "id"}, ...)` + 스타터가 자동 resolve하는 `KeysetRequest` 파라미터 |
| `location/LocationService.java` | **size + 1** 트릭 (매퍼가 한 행 더 가져와서 `KeysetPage.build`가 `hasNext`를 정확히 설정) + 다음 커서가 되는 `keyExtractor` 람다 |
| `location/LocationMapper.java` + `resources/mapper/LocationMapper.xml` | 복합 키 seek predicate — `time < ? OR (time = ? AND id < ?)`. 타임스탬프가 겹쳐도 walk를 안정적으로 만드는 핵심 |
| `resources/schema.sql` | 쿼리의 ORDER BY와 일치하는 covering 인덱스 `(worker_id, time DESC, id DESC)`. 실제 DB에서 페이지당 O(log N) 유지 |

## 빌드 검증

```bash
./gradlew build
```

스모크 테스트가 앱 부팅 후 전체 커서 체인을 4페이지 walk하면서 **중복/누락 없음** 불변식 — keyset 페이지네이션이 실제로 보장해야 하는 property — 검증.

## `easy-paging-demo`와의 차이

| | `easy-paging-demo` (offset) | `easy-paging-keyset-demo` (이거) |
|---|---|---|
| 어노테이션 | `@AutoPaginate` | `@KeysetPaginate(keys = {...})` |
| 반환 타입 | `PageResponse<T>` | `KeysetPage<T>` |
| 매퍼 SQL | 평범한 `SELECT` (LIMIT/OFFSET 없음 — aspect가 주입) | 명시적 `WHERE` seek 절 + `LIMIT size+1` |
| `totalElements` | 있음 | 없음 (`COUNT(*)` 피하는 게 keyset의 핵심) |
| 적합한 경우 | totals가 있는 유한 paginated 리스트 | 무한 스트림, append-only 테이블 |
| 깊이별 성능 | 페이지 번호에 따라 느려짐 | 페이지마다 일정 |
