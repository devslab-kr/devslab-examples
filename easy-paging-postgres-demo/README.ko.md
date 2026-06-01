# easy-paging-postgres-demo

[English](README.md) · **한국어**

[`easy-paging-spring-boot-starter`](https://github.com/devslab-kr/easy-paging-spring-boot-starter)의 프로덕션 풍 실행 가능한 예제 — H2 대신 **실제 PostgreSQL** 상대.

스타터는 PageHelper가 지원하는 어떤 JDBC DB든 동작 (Postgres, MySQL, MariaDB, Oracle, ...) — 이 데모는 그걸 실제 팀이 가장 많이 쓰는 DB로 end-to-end 검증하고, 이 repo가 앞으로 모든 "외부 DB" 데모에 쓸 Docker Compose + Testcontainers 패턴을 보여줌.

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
git sparse-checkout set easy-paging-postgres-demo
cd easy-paging-postgres-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/easy-paging-postgres-demo
cd easy-paging-postgres-demo
```

## 실행

```bash
cd easy-paging-postgres-demo

# Postgres 백그라운드로 시작. compose 파일이 localhost:5432에 바인드
docker compose up -d db

# 앱 부팅. spring.sql.init가 매 시작마다 스키마 재생성 + 500개 product 시드
# → 항상 알려진 상태로 부팅
./gradlew bootRun
```

앱은 `http://localhost:8080`에 뜸. 끝나면:

```bash
docker compose down       # 컨테이너 중지
docker compose down -v    # ... 그리고 볼륨 삭제 (다음에 clean slate)
```

## 시험해보기

```bash
# 기본 offset 페이지네이션
curl 'http://localhost:8080/products?page=0&size=10'

# 정렬
curl 'http://localhost:8080/products?page=0&size=10&sort=price,desc'
curl 'http://localhost:8080/products?page=0&size=10&sort=createdAt,desc&sort=id,asc'

# 카테고리 필터 — 데이터는 5개 카테고리에 100개씩 시드됨
curl 'http://localhost:8080/products?category=books&page=0&size=20' | jq '.totalElements'
# → 100

# 페이지 크기 clamping (컨트롤러가 @AutoPaginate(maxSize = 100) 선언)
curl 'http://localhost:8080/products?size=9999' | jq '.size, (.content | length)'
# → 100
# → 100

# 정렬 인젝션 시도는 DB에 닿기 전에 HTTP 400
curl -i 'http://localhost:8080/products?sort=name;DROP%20TABLE%20products'
```

## 테스트 동작 방식 (Docker Compose vs Testcontainers)

이 데모엔 두 Docker 경로가 의도적으로 분리되어 있음:

| 경로 | 언제 실행 | 무엇 사용 |
| --- | --- | --- |
| `docker compose up -d db` | **사람**이 장수명 DB 상대 `bootRun` 할 때 | 이 디렉토리의 `docker-compose.yml` — 5432 포트 publish, named volume `pgdata` |
| `ProductControllerIT`의 Testcontainers | `./gradlew test` 실행 시 (로컬/CI) | **단명** `postgres:16-alpine` 컨테이너, 임의 포트, 테스트 클래스마다 시작/종료 |

통합 테스트는 Spring Boot 3.1+의 [`@ServiceConnection`](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html#testing.testcontainers.service-connections)을 사용 — Testcontainers 인스턴스로 `spring.datasource.url`을 자동 재배선. `application-test.yml` 없음, `@DynamicPropertySource` 없음. 테스트가 `docker-compose.yml`을 읽지 않으므로 compose의 포트와 테스트의 포트가 충돌 안 함.

즉 CI는 Postgres 사전 설치 없이 깨끗한 Ubuntu 러너에서 `./gradlew build` 실행 가능. 러너엔 이미 Docker가 있고, 나머지는 Testcontainers가 처리.

## 읽을 만한 파일

흥미로운 부분, 순서대로:

| 파일 | 왜 |
| --- | --- |
| `docker-compose.yml` | healthcheck 포함된 최소 PG 서비스 — `docker compose up -d`가 한 줄짜리 |
| `build.gradle.kts` | `org.postgresql:postgresql` (드라이버) + `spring-boot-testcontainers` / `org.testcontainers:postgresql` 테스트 의존성 추가. 그 외는 H2 데모와 동일 |
| `src/test/.../ProductControllerIT.java` | `@Testcontainers` + `@ServiceConnection` 두 줄로 wiring 완성. 정적 초기화 블록 없음, `@DynamicPropertySource` 없음 |
| `src/main/resources/schema.sql` | `BIGSERIAL`과 복합 인덱스 — PG 네이티브, H2 비호환 — 사용해서 스타터가 실제 PG 스키마 기능과 잘 동작함을 보여줌 |
| `src/main/resources/data.sql` | 결정론적 시딩을 위한 `generate_series(1, 500)` (테스트가 검증하는 필드엔 `random()` 안 씀) |
| `product/ProductController.java` | 계약은 어노테이션 하나 + `Pageable` — H2 데모와 동일. 스타터는 밑이 어떤 DB인지 신경 안 씀 |

## 마이그레이션 (실제 앱으로 복붙할 거면 읽기)

이 데모는 매 부팅마다 `DROP TABLE IF EXISTS` + `CREATE TABLE` 스크립트를 실행하는 `spring.sql.init`을 사용. **프로덕션에선 그러지 마세요.** [Flyway](https://flywaydb.org/)나 [Liquibase](https://www.liquibase.org/) 사용 — 둘 다 Spring Boot 스타터 있고 `easy-paging-spring-boot-starter`와 별도 설정 없이 공존. `spring.sql.init`은 "셋업 단계 없이 항상 알려진 상태로 부팅"이 목적인 학습용 데모에서만 OK.

## 빌드 검증

```bash
./gradlew build
```

`ProductControllerIT`가 단명 Testcontainers Postgres 상대로 실행됨. 첫 실행은 `postgres:16-alpine` 이미지 (~80MB) 풀; 이후 실행은 캐시된 이미지로 몇 초 안에 완료.

## `easy-paging-demo`와의 차이

| | `easy-paging-demo` | `easy-paging-postgres-demo` (이거) |
|---|---|---|
| DB | H2 인메모리 (외부 런타임 없음) | 실제 PostgreSQL (`bootRun`은 Docker Compose, 테스트는 Testcontainers) |
| 스키마 기능 | 기본 테이블 | `BIGSERIAL`, `NUMERIC(10,2)`, 복합 인덱스, `generate_series` |
| 매퍼 로직 | 항상 `SELECT *` | `<if>` 통해 옵셔널 `WHERE category = ?` |
| 테스트 인프라 | MockMvc만 | Testcontainers + `@ServiceConnection` |
| 적합한 경우 | "30초 안에 스타터 보여주기" | "프로덕션 DB 상대로 동작하고 테스트가 깔끔히 통합되는지 보여주기" |
