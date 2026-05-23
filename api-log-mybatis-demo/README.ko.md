# api-log-mybatis-demo

[English](README.md) · **한국어**

> ✨ **MyBatis 백엔드.** 이 데모는 [`api-log`](https://github.com/devslab-kr/api-log) 스타터를 **MyBatis**로 연결. JPA 백엔드는 [`api-log-jpa-demo`](../api-log-jpa-demo/), 리액티브/R2DBC 백엔드는 [`api-log-r2dbc-demo`](../api-log-r2dbc-demo/) 참조.

[`api-log`](https://github.com/devslab-kr/api-log)의 실행 가능한 예제 — PostgreSQL `api_log` JSONB 테이블에 MyBatis 매퍼로 기록하는 이벤트 기반 API 호출 로깅.

## 이 데모가 보여주는 것

- `RestApiClientUtil`로 나가는 모든 HTTP 호출이 라이프사이클 이벤트 발행 (INITIATED → SUCCESS 또는 INITIATED → ERROR).
- 스타터의 리스너가 그 이벤트를 **별도 스레드**에서 소비하여 자동 등록된 `ApiLogMapper`를 통해 영속화.
- `api_log` 테이블은 시작 시 자동 생성 (`api.log.schema.management=BUILTIN`).
- 단일 프로세스 데모: 같은 앱이 `/upstream/widgets`("외부 서비스" 역할)과 `/client/widgets`(호출자) 둘 다 노출. 호출자의 base URL은 `localhost`를 가리키므로 두 번째 서비스 없이 전체 파이프라인 관찰 가능.

## 전제조건

- JDK 21+
- **Docker** (Docker Desktop 또는 호환 런타임)

## 실행

```bash
cd api-log-mybatis-demo
docker compose up -d db
./gradlew bootRun
```

앱은 `http://localhost:8080`에 뜸. 끝나면:

```bash
docker compose down       # 컨테이너 중지
docker compose down -v    # ... 그리고 볼륨 삭제 (다음에 clean slate)
```

## 시험해보기

```bash
# Happy path - api_log에 INITIATED + SUCCESS 행 생성
curl 'http://localhost:8080/client/widgets/123'

# 비동기 변형 - 같은 로깅, CompletableFuture 경유
curl 'http://localhost:8080/client/widgets/123/async'

# 에러 경로 - upstream이 id=999에 throw, ERROR 행 생성
curl -i 'http://localhost:8080/client/widgets/999'

# POST - 바디는 api_log.payload에 표시됨 (payload가 JSONB이므로 JSON 텍스트로)
curl -X POST 'http://localhost:8080/client/widgets' \
     -H 'Content-Type: application/json' \
     -d '{"name":"Sprocket-7","sku":"SKU-7","price":19.99}'

# 명시적 requestId - 일반적인 retry 상관관계 패턴
curl -X POST 'http://localhost:8080/client/widgets/with-request-id/123'
curl 'http://localhost:8080/api-log/by-request/demo-fixed-rid' | jq

# 로그 읽기
curl 'http://localhost:8080/api-log/recent'           | jq
curl 'http://localhost:8080/api-log/by-event/SUCCESS' | jq
curl 'http://localhost:8080/api-log/by-event/ERROR'   | jq
```

## 아키텍처

```
                        (같은 JVM)
                  +---------------------+
   curl --GET-->  | ClientController    |
                  |   /client/widgets   |
                  +---------------------+
                            |
                            | 사용
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
                                       |  (async 스레드)    |   리스너 스레드)
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
                                       |   INSERT api_log   |   자동 wiring)
                                       +--------------------+
                                                 |
                                                 v
                                       +--------------------+
                                       |  PostgreSQL JSONB  |
                                       +--------------------+
```

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | `api-log-core` + `api-log-mybatis` + `mybatis-spring-boot-starter:4.0.1` 추가 — 스타터 wiring은 이게 전부 |
| `src/main/resources/application.yml` | `api.log.enabled` + `api.log.schema.management=BUILTIN` + `mybatis.mapper-locations` |
| `widget/ClientController.java` | 모든 종류의 호출 (sync/async/typed/POST/PUT/DELETE/explicit-requestId)을 `RestApiClientUtil` 경유 |
| `widget/ApiLogController.java` | 리더 엔드포인트 — 스타터의 `ApiLogMapper.findByRequestId`와 데모 자체의 `ApiLogQueryMapper`("recent N" / "by event type") 둘 다 사용 |
| `mapper/ApiLogQueryMapper.xml` | JSONB 컬럼을 MyBatis `String` 프로퍼티로 읽기 위한 `payload::text AS payload` 캐스트 패턴 |
| `src/test/.../ApiLogLifecycleIT.java` | 실제 Postgres Testcontainer 상대 end-to-end 테스트 — 비동기 리스너를 위해 Awaitility 사용 |

## 테스트 동작 방식 (Docker Compose vs Testcontainers)

이 repo의 다른 데모와 동일한 2-경로 셋업:

| 경로 | 언제 실행 | 무엇 사용 |
| --- | --- | --- |
| `docker compose up -d db` | **사람**이 장수명 DB 상대 `bootRun` 할 때 | 이 디렉토리의 `docker-compose.yml` — 5432 포트 publish, named volume `pgdata` |
| `ApiLogLifecycleIT`의 Testcontainers | `./gradlew test` 실행 시 (로컬/CI) | **단명** `postgres:16-alpine` 컨테이너, 임의 포트 |

`@ServiceConnection` (Spring Boot 3.1+)이 Testcontainers 인스턴스로 `spring.datasource.url`을 자동 재배선 — `application-test.yml` 없음, `@DynamicPropertySource` 없음.

## 프로덕션 노트

이 데모에서 "실행 편의성" 위주로 튠된, 실제 서비스에선 달라질 부분 몇 가지:

- **스키마 관리.** `api.log.schema.management=BUILTIN`은 매 부팅마다 스타터의 번들 DDL 실행 (멱등 — `CREATE TABLE IF NOT EXISTS`). 프로덕션에선 `EXTERNAL`로 설정하고 Flyway 또는 Liquibase가 마이그레이션을 소유하게 할 것.
- **매퍼 스캔.** 데모의 `@MapperScan`은 `kr.devslab.examples.apilogmybatis`만 커버. **`kr.devslab.apilog.mybatis.mapper`를 스캔에 추가하지 말 것** — 스타터의 auto-config가 이미 `ApiLogMapper`를 등록하므로 이중 스캔하면 충돌하는 bean 정의가 생김.
- **`application.yml`의 `map-underscore-to-camel-case`** 가 데모의 커스텀 `ApiLogQueryMapper`로 하여금 snake_case 컬럼을 `ApiLogRow`의 camelCase 필드로 resultMap에 일일이 적지 않고도 매핑 가능하게 함. 스타터 자체 매퍼는 같은 이유로 명시적 `AS camelCase` 별칭을 사용 — 어느 패턴이 코드베이스에 맞는지 선택.
- **Self-loopback.** 실제 앱은 `api-log-demo.upstream-base-url`을 다른 서비스로 가리킴. loopback은 데모를 단일 프로세스로 실행하기 위한 것.

## 빌드 검증

```bash
./gradlew build
```

컴파일 + 단명 Testcontainers Postgres 상대 `ApiLogLifecycleIT` 실행 + 실행 가능한 jar 생성. 첫 실행은 `postgres:16-alpine` (~80 MB) 풀; 이후는 캐시된 이미지 사용.
