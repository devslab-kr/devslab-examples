# api-log-jpa-demo

[English](README.md) · **한국어**

> ✨ **JPA 백엔드.** Servlet + JPA + PostgreSQL. R2DBC 버전: [`api-log-r2dbc-demo`](../api-log-r2dbc-demo/). MyBatis 버전: [`api-log-mybatis-demo`](../api-log-mybatis-demo/).

[`api-log`](https://github.com/devslab-kr/api-log) (`api-log-core` + `api-log-jpa` 모듈, v3.0.0)의 실행 가능한 예제 — 아웃바운드 HTTP 호출을 PostgreSQL JSONB 기반 `api_log` 테이블에 감사 로깅.

자동 구성된 `RestApiClientUtil`을 통한 모든 호출은 세 종류의 라이프사이클 이벤트(`INITIATED` → `SUCCESS` 또는 `ERROR`)를 발행하며, api-log 리스너가 비동기로 `api_log` 테이블에 기록한다. 호출자는 write를 기다리지 않음; 감사 행은 `request_id`로 호출과 연결됨.

이 데모는 **self-contained** — 동일한 Spring Boot 앱이 응답하는 upstream 서비스와 호출하는 클라이언트 컨트롤러를 모두 노출하므로, 단일 `bootRun`으로 두 번째 프로세스 없이 전체 라이프사이클을 실행할 수 있음.

## 전제조건

- JDK 21+
- **Docker** (Docker Desktop 또는 호환 런타임)
- 그 외 없음. 로컬 Postgres 설치 불필요. psql 클라이언트 불필요.

## 실행

```bash
cd api-log-jpa-demo

# Postgres 백그라운드로 시작. compose 파일이 localhost:5432에 바인드
docker compose up -d db

# 앱 부팅. api-log 스타터의 BUILTIN 스키마 초기화기가 첫 시작 시
# api_log 테이블 생성 (멱등성 — IF NOT EXISTS)
./gradlew bootRun
```

앱은 `http://localhost:8080`에 뜸. 끝나면:

```bash
docker compose down       # 컨테이너 중지
docker compose down -v    # ... 그리고 볼륨 삭제 (다음에 clean slate)
```

## 시험해보기

### 1. Happy path: GET 후 감사 추적 읽기

```bash
# Self-loopback GET — 클라이언트가 upstream을 호출, 둘 다 같은 JVM
curl 'http://localhost:8080/client/widgets/123'
# → {"id":123,"name":"Widget-123","sku":"SKU-123","price":1230}

# api-log가 해당 호출의 INITIATED + SUCCESS 행을 비동기로 기록
curl 'http://localhost:8080/api-log/recent' | jq '.[0:4] | .[] | {eventType, requestId, endpoint, statusCode}'
# →
#   {"eventType":"SUCCESS",   "requestId":"...uuid...", "endpoint":"http://localhost:8080/upstream/widgets/123", "statusCode":200}
#   {"eventType":"INITIATED", "requestId":"...same...", "endpoint":"http://localhost:8080/upstream/widgets/123", "statusCode":null}
```

### 2. POST: 페이로드가 JSONB로 보존됨

```bash
curl -X POST 'http://localhost:8080/client/widgets' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Gizmo","sku":"SKU-Gizmo","price":19.99}'

# 직렬화된 요청 바디가 api_log.payload (JSONB)에 그대로 저장됨
curl 'http://localhost:8080/api-log/recent' | jq '.[0:2] | .[] | {eventType, payload}'
```

### 3. Error path: 5xx는 ERROR 행이 됨

```bash
# /upstream/widgets/999는 일부러 500을 던지도록 와이어링됨
curl -i 'http://localhost:8080/client/widgets/999'
# → HTTP/1.1 500 ...

# api-log가 upstream 응답 바디를 api_log.error_message에 캡처
curl 'http://localhost:8080/api-log/by-event/ERROR' | jq '.[0] | {eventType, statusCode, errorMessage}'
```

### 4. 명시적 requestId: 논리적 호출 그룹 상관관계

```bash
# /with-request-id 엔드포인트는 코어 send(HttpMethod, ApiRequest) 오버로드를 통해
# requestId="demo-fixed-rid"를 전달. 실제 재시도 시나리오에서는 모든 시도에 같은
# id를 재사용해서 전체 시퀀스가 단일 키 아래 그룹핑되도록 함.
curl -X POST 'http://localhost:8080/client/widgets/with-request-id/123'

curl 'http://localhost:8080/api-log/by-request/demo-fixed-rid' | jq '. | length'
# → 2 (INITIATED + SUCCESS)
```

## 아키텍처

```
+--------------+         +--------------------+         +----------------------+
|  curl /      |  HTTP   |  ClientController  |  HTTP   |  UpstreamController  |
|  test client | ------> |  (이 앱)            | ------> |  (같은 JVM)           |
+--------------+         +---------+----------+         +----------------------+
                                   |
                                   | 호출
                                   v
                         +-----------------------+
                         |   RestApiClientUtil   |   (api-log-core가 자동 구성)
                         +---------+-------------+
                                   |
                                   | 동기로 이벤트 발행
                                   v
                         +------------------------------+
                         |  ApplicationEventPublisher   |   ApiCallInitiatedEvent / SuccessEvent / ErrorEvent
                         +---------+--------------------+
                                   |
                                   | (@Async executor로 리스너에 전달 — JDK 21+에서 virtual thread)
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
                         |  PostgreSQL api_log |   (BUILTIN 초기화기가 boot에서 테이블 생성)
                         +---------------------+
```

HTTP 호출자는 DB를 기다리지 않음. 리스너의 `@Async` hop과 writer의 `REQUIRES_NEW` propagation이 함께:

- writer 예외가 컨슈머의 트랜잭션을 롤백할 수 없음
- 컨슈머의 트랜잭션 롤백이 이미 나간 호출의 이미 기록된 행을 삭제할 수 없음

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | `kr.devslab:api-log-core:3.0.0` + `kr.devslab:api-log-jpa:3.0.0` 선언; JPA 백엔드가 `spring-boot-starter-data-jpa`와 `org.postgresql:postgresql`을 끌어옴 |
| `src/main/resources/application.yml` | `api.log.schema.management=BUILTIN` — 스타터가 boot에서 `api_log` 테이블 생성. 실제 앱에선 `NONE` (DDL 직접 적용) 또는 `FLYWAY` (Flyway가 소유) 사용 |
| `widget/ClientController.java` | `RestApiClientUtil` 한 번 생성자 주입; 모든 메서드가 그걸 거치므로 모든 아웃바운드 호출이 균일하게 로깅됨 |
| `widget/UpstreamController.java` | self-loopback의 "외부 서비스" 반쪽 — 같은 JVM, 다른 라우트 — id=999에 의도적 500 포함 |
| `widget/ApiLogController.java` | 자동 등록된 `ApiLogRepository`를 통한 `api_log` 읽기 전용 뷰; 스타터는 reporting API를 제공하지 않으므로 데모용 최소한 |
| `src/test/.../ApiLogLifecycleIT.java` | 전체 라이프사이클 통합 테스트: 랜덤 포트에 실제 HTTP, Awaitility가 비동기 write를 폴, `@ServiceConnection` 통한 Testcontainers Postgres |

## 테스트 동작 방식 (Docker Compose vs Testcontainers)

이 데모엔 두 Docker 경로가 의도적으로 분리되어 있음:

| 경로 | 언제 실행 | 무엇 사용 |
| --- | --- | --- |
| `docker compose up -d db` | **사람**이 장수명 DB 상대 `bootRun` 할 때 | 이 디렉토리의 `docker-compose.yml` — 5432 포트 publish, named volume `pgdata` |
| `ApiLogLifecycleIT`의 Testcontainers | `./gradlew test` 실행 시 (로컬/CI) | **단명** `postgres:16-alpine` 컨테이너, 임의 포트, 테스트 클래스마다 시작/종료 |

통합 테스트는 Spring Boot 3.1+의 [`@ServiceConnection`](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html#testing.testcontainers.service-connections)을 사용 — Testcontainers 인스턴스로 `spring.datasource.url`을 자동 재배선. `application-test.yml` 없음, `@DynamicPropertySource` 없음.

## 프로덕션 노트

- **`api_log` 스키마**: 데모는 `api.log.schema.management=BUILTIN`을 사용 — 매 boot마다 번들된 `V1.0__create_api_log.sql` 실행 (`CREATE TABLE IF NOT EXISTS`로 멱등성). **프로덕션에선** `FLYWAY` (스타터가 `classpath:db/api-log`를 Flyway 위치에 append, 마이그레이션이 본인 마이그레이션과 함께 `flyway_schema_history`에 기록됨) 또는 `NONE` (Liquibase 등으로 DDL 직접 적용) 권장.
- **`RestApiClientUtil`**: 스타터가 단일 자동 구성 `RestClient` bean을 노출하고 wrapping함. 앱에 자체 `RestClient`가 이미 구성되어 있다면 (auth header, custom timeout, mTLS), `@Bean`으로 선언 — 스타터의 `@ConditionalOnMissingBean`이 본인 것에 위임. `RestApiClientUtil`은 본인 클라이언트를 사용하면서도 여전히 이벤트를 emit.
- **비동기 리스너**: 이벤트는 `ApiLogEvent-` executor에서 처리됨 — `spring.threads.virtual.enabled=true`일 때 virtual thread (JDK 21+ 권장), 그 외 platform-thread pool. 각 write는 `@Retryable(maxAttempts = 3)`로 wrapping되어 일시적 DB blip이 로그 행을 떨어뜨리지 않음.

## 빌드 검증

```bash
./gradlew build
```

`ApiLogLifecycleIT`가 단명 Testcontainers Postgres 상대로 실행됨. 첫 실행은 `postgres:16-alpine` 이미지 (~80MB) 풀; 이후 실행은 캐시된 이미지로 몇 초 안에 완료.
