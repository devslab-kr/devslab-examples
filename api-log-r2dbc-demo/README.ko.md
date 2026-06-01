# api-log-r2dbc-demo

[English](README.md) · **한국어**

> ✨ **R2DBC 백엔드 (Reactive / WebFlux).** 이 데모는 [`api-log`](https://github.com/devslab-kr/api-log)의 R2DBC 변형(`api-log-core` + `api-log-r2dbc`, 둘 다 `3.0.0`)을 사용. 블로킹 변형은 자매 데모 참조: [`api-log-jpa-demo`](../api-log-jpa-demo/) 및 [`api-log-mybatis-demo`](../api-log-mybatis-demo/).

[`api-log`](https://github.com/devslab-kr/api-log)의 프로덕션 풍 실행 가능한 예제 — HTTP 경로가 전 구간 **non-blocking**: 프런트엔드는 WebFlux, 아웃바운드 호출은 `ReactiveApiClientUtil`, R2DBC 라이터가 R2DBC의 reactive `ConnectionFactory`를 거쳐 `api_log` JSONB 테이블에 영속.

## 이 데모가 보여주는 것

- `api-log`가 R2DBC의 reactive `ConnectionFactory`를 통해 기록 — JDBC 커넥션 풀 없음, 감사 로그 기록 경로가 플랫폼 스레드 블로킹 없음
- 전체 HTTP 경로(인바운드 WebFlux 요청 → `ReactiveApiClientUtil` 아웃바운드 호출 → 클라이언트로 응답)가 Reactor 이벤트 루프 위에 머무름
- JPA / MyBatis 백엔드와 동일한 `api_log` 테이블 스키마 — 동일한 JSONB 컬럼(`payload`, `response`, `error_message`), 동일한 `event_type` 라이프사이클(`INITIATED` → `SUCCESS` | `ERROR`) — 감사 컨슈머는 백엔드 종류와 무관하게 동일하게 읽을 수 있음
- **셀프 루프백** 데모 토폴로지: 한 앱이 upstream과 client 양쪽을 노출. client는 `ReactiveApiClientUtil`을 통해 HTTP로 upstream을 호출하고, 이게 호출당 전체 INITIATED + SUCCESS / ERROR 페어를 기록하는 데 충분
- R2DBC 앱에서 테이블을 **읽는** 방법 — `api-log-r2dbc` 아티팩트는 라이터만 출하; 이 데모는 자체 `DatabaseClient` 기반 reader를 가져와서 read 엔드포인트(`/api-log/recent`, `/api-log/by-request/{requestId}`, `/api-log/by-event/{eventType}`)가 기록된 내용을 보여줌

## 전제조건

- JDK 21+
- **Docker** (Docker Desktop 또는 호환 런타임)
- 그 외 없음. 로컬 Postgres 설치 불필요. psql 클라이언트 불필요.

## 이 데모만 받기

각 데모는 독립 Gradle 프로젝트라, `devslab-examples` 저장소 전체를 clone하지 않고
이 폴더만 받을 수 있습니다.

**git 사용 (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set api-log-r2dbc-demo
cd api-log-r2dbc-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/api-log-r2dbc-demo
cd api-log-r2dbc-demo
```

## 빠른 시작

```bash
cd api-log-r2dbc-demo

# Postgres 백그라운드로 시작. compose 파일이 localhost:5432에 바인드
docker compose up -d db

# 앱 부팅. ApiLogR2dbcSchemaInitializer가 시작 시 api_log 테이블 생성
# (api.log.r2dbc.schema.enabled=true, 기본값).
./gradlew bootRun
```

앱은 `http://localhost:8080`에 뜸. 시험:

```bash
# 정상 GET — api_log에 INITIATED + SUCCESS 페어 생성
curl 'http://localhost:8080/client/widgets/123'

# 에러 경로 — INITIATED + ERROR 페어 생성
curl -i 'http://localhost:8080/client/widgets/999'

# POST — 요청 본문이 payload JSONB 컬럼에 캡처됨
curl -X POST -H 'Content-Type: application/json' \
  -d '{"name":"Hyperbolic Cog","sku":"SKU-COG-42"}' \
  'http://localhost:8080/client/widgets'

# 명시적 requestId 상관관계 — 두 행이 request_id=demo-fixed-rid 공유
curl -X POST 'http://localhost:8080/client/widgets/with-request-id/123'

# 기록된 내용 확인
curl 'http://localhost:8080/api-log/recent'
curl 'http://localhost:8080/api-log/by-request/demo-fixed-rid'
curl 'http://localhost:8080/api-log/by-event/SUCCESS'
curl 'http://localhost:8080/api-log/by-event/ERROR'
```

끝나면:

```bash
docker compose down       # 컨테이너 중지
docker compose down -v    # ... 그리고 볼륨 삭제 (다음에 clean slate)
```

## 아키텍처

```
caller (curl / browser)
        │
        ▼  HTTP (Netty 이벤트 루프, non-blocking)
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

감사 로그 기록은 응답을 막지 않음 — `ApplicationEventPublisher`가 이벤트를 리스너로 fan-out하고, 리스너가 자체 subscriber에서 R2DBC 인서트를 수행. 호출자에 대한 HTTP 응답은 upstream 응답이 돌아오는 즉시 반환.

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `api-log-core` + `api-log-r2dbc`, 그리고 R2DBC와 JDBC PostgreSQL 드라이버 양쪽 추가 (JDBC 드라이버는 Testcontainers의 `@ServiceConnection` 용으로만 사용) |
| `src/main/resources/application.yml` | `spring.datasource.url` 대신 `spring.r2dbc.url`; `api.log.schema.management=BUILTIN` 대신 `api.log.r2dbc.schema.enabled=true` — R2DBC와 블로킹 백엔드는 서로 다른 프로퍼티 이름을 사용 |
| `widget/ClientController.java` | `ReactiveApiClientUtil` 사용 — `getTyped`, `postTyped`, `putTyped`, `delete`, 그리고 명시적 requestId 상관관계 케이스를 위한 `send(method, ApiRequest)` 변형 |
| `widget/UpstreamController.java` | 셀프 루프백 타깃; `id == 999`는 5xx를 반환해서 에러 경로 동작 가능 |
| `widget/ApiLogReader.java` | `DatabaseClient` 기반 reader — `api-log-r2dbc` 아티팩트는 라이터만 출하하므로 데모가 자체 SELECT를 가져옴. JSONB 컬럼을 `text`로 캐스트해 `String` 필드에 깔끔히 바인딩 |
| `src/test/.../ApiLogLifecycleIT.java` | R2DBC용 `@Testcontainers` + `@ServiceConnection` + WebFlux용 `WebTestClient` + 비동기 리스너를 기다리기 위한 Awaitility — happy, error, payload 보존, requestId 상관관계, "스키마 쿼리 가능"의 5개 테스트 |

## 프로덕션 노트

- Reactive 스키마 초기자(`api.log.r2dbc.schema.enabled`, 기본 `true`)는 시작 시 `api_log` 테이블이 없으면 생성. **데모에는 OK, 프로덕션엔 좋지 않음.** R2DBC에는 first-class 마이그레이션 도구가 없음 — 실제 배포에선 부팅 시 [Flyway](https://flywaydb.org/)를 **별도 JDBC 커넥션**으로 실행 (Flyway는 R2DBC 런타임 옆에서 잘 동작 — Spring Boot의 `flyway.url` / `flyway.user` / `flyway.password` 프로퍼티는 `spring.r2dbc.*`와 독립적)하고 `api.log.r2dbc.schema.enabled=false`로 reactive 초기자를 비활성화.
- R2DBC 백엔드는 `api.log.schema.management`가 **아니라** `api.log.r2dbc.schema.enabled`를 사용한다는 점에 유의. 후자 프로퍼티는 JPA + MyBatis 백엔드에만 적용; 둘을 헷갈리는 게 초보자가 흔히 빠지는 함정.
- `ReactiveApiClientUtil`은 `WebClient` 위에 빌드 — 자동 구성되지만 타임아웃 없음. 프로덕션에선 앱이 받는 `WebClient.Builder`에 connect / response 타임아웃을 구성할 것 — `ReactiveApiClientUtil`의 생성자는 `WebClient`를 받으므로, 이미 커스터마이즈한 것을 넘길 수 있음.

## 테스트 동작 방식

`./gradlew test`는 `docker-compose.yml`을 읽지 않음. Testcontainers가 단명 `postgres:16-alpine`을 임의 포트로 띄우고, `PostgreSQLContainer`의 `@ServiceConnection`이 양쪽을 생성:

- `DataSourceConnectionDetails` (JDBC, 요청하는 모든 것에)
- `spring.r2dbc.url`을 컨테이너로 재배선하는 `R2dbcConnectionDetails`

…그래서 앱의 R2DBC 런타임이 `application-test.yml`이나 `@DynamicPropertySource` 없이 컨테이너를 가리킴. CI도 동일하게 실행: Ubuntu 러너엔 이미 Docker가 있으므로 IT 스위트는 셋업 없이 실제 Postgres 상대로 실행.

## 빌드 검증

```bash
./gradlew build
```

첫 실행은 `postgres:16-alpine` 이미지(~80MB) 풀; 이후 실행은 캐시된 이미지 사용.
