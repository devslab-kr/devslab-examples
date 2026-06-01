# easy-paging-sb4-reactive-demo

[English](README.md) · **한국어**

> ✨ **Spring Boot 4 라인.** 이 데모는 `easy-paging-spring-boot-starter`의 활성 `0.5.x` 라인(Spring Boot 4 / Spring Framework 7 / Jackson 3)을 사용. Spring Boot 3.3–3.5 maintenance 버전은 [`easy-paging-reactive-demo`](../easy-paging-reactive-demo/) 참조.

[`easy-paging-spring-boot-starter`](https://github.com/devslab-kr/easy-paging-spring-boot-starter)의 Reactive (WebFlux + R2DBC) 실행 가능한 예제. 자매 아티팩트 [`easy-paging-spring-boot-starter-reactive`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter-reactive) 사용.

MVC + MyBatis 데모들 (`easy-paging-demo`, `easy-paging-postgres-demo`)은 AOP aspect로 페이지네이션을 wiring. 그런데 reactive 스택엔 hook할 thread-per-request 컨텍스트가 없어서 reactive 스타터는 다른 형태를 취함: 서비스가 명시적으로 호출하는 헬퍼 (`R2dbcOffsetPagingSupport`). 하지만 **wire 계약은 동일** — 동일한 `PageResponse<T>` JSON 봉투, 동일한 `?page=`/`?size=`/`?sort=` 시맨틱 — 그래서 클라이언트는 엔드포인트 뒤에 어떤 스택이 있는지 알 수 없음.

## 전제조건

- JDK 21+
- **Docker** (`bootRun`과 `./gradlew test` 둘 다)
- 로컬 Postgres 설치 불필요.

## 이 데모만 받기

각 데모는 독립 Gradle 프로젝트라, `devslab-examples` 저장소 전체를 clone하지 않고
이 폴더만 받을 수 있습니다.

**git 사용 (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set easy-paging-sb4-reactive-demo
cd easy-paging-sb4-reactive-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/easy-paging-sb4-reactive-demo
cd easy-paging-sb4-reactive-demo
```

## 실행

```bash
cd easy-paging-reactive-demo

# PostgreSQL을 호스트 5433 포트에 — easy-paging-postgres-demo(5432)와
# 동시에 실행 가능하도록 일부러 다른 포트.
docker compose up -d db

# 앱 부팅. spring.sql.init가 매 시작마다 스키마 재생성 + 500개 article 시드,
# postgres 데모와 동일.
./gradlew bootRun
```

앱은 `http://localhost:8080`에 뜸. `docker compose down` (또는 `docker compose down -v`로 볼륨 삭제)으로 정리.

## 시험해보기

```bash
# 기본 offset 페이지네이션 — totalElements=500, totalPages=50
curl 'http://localhost:8080/articles?page=0&size=10'

# 정렬
curl 'http://localhost:8080/articles?page=0&size=10&sort=publishedAt,desc'
curl 'http://localhost:8080/articles?page=0&size=5&sort=viewCount,desc&sort=id,asc'

# author 필터 — 5명 author에 각 100개씩
curl 'http://localhost:8080/articles?author=alice&page=0&size=20' | jq '.totalElements'
# → 100

# 정렬 인젝션 시도는 여전히 HTTP 400 (검증 로직은 core 스타터에 있고 reactive 쪽이 재사용)
curl -i 'http://localhost:8080/articles?sort=title;DROP%20TABLE%20articles'
```

## postgres 데모와의 차이

wire 동작은 동일. 코드 차이는 모두 reactive 스택과 관련된 것:

| 레이어 | Postgres 데모 | Reactive 데모 (이거) |
| --- | --- | --- |
| Web | `spring-boot-starter-web` (서블릿) | `spring-boot-starter-webflux` |
| DB 접근 | MyBatis + JDBC | Spring Data R2DBC (`R2dbcEntityTemplate`) |
| 페이지네이션 wiring | 컨트롤러의 `@AutoPaginate` aspect | 서비스에서 `R2dbcOffsetPagingSupport.paginate(...)` 명시적 호출 |
| 컨트롤러 반환 타입 | `PageResponse<T>` | `Mono<PageResponse<T>>` |
| 엔티티 매핑 | 수동 `Product` POJO + MyBatis XML resultType | Spring Data Relational `@Table` / `@Column` / `@Id` |
| 매퍼 SQL | XML의 손으로 쓴 `<select>` | 헬퍼가 생성하는 `Query.query(criteria).with(pageable)` |
| 테스트 인프라 | MockMvc + Testcontainers JDBC | WebTestClient + Testcontainers R2DBC |
| 자매 아티팩트 | 없음 (`easy-paging-spring-boot-starter`만) | `easy-paging-spring-boot-starter-reactive` 추가 |

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | `easy-paging-spring-boot-starter-reactive` **와** `spring-boot-starter-{webflux,data-r2dbc}` 둘 다 추가. R2DBC PG 드라이버는 `org.postgresql:r2dbc-postgresql`, JDBC 드라이버 아님 |
| `article/Article.java` | Spring Data Relational 엔티티; camelCase가 snake_case와 다른 필드에만 `@Column` |
| `article/ArticleService.java` | 핵심 줄은 `R2dbcOffsetPagingSupport.paginate(template, Article.class, criteria, pageable)` — 이게 reactive paging 프리미티브 전체 |
| `article/ArticleController.java` | `Mono<PageResponse<Article>>` 반환 타입. **`@AutoPaginate` 없음** — 이유는 파일 안 주석 참고 |
| `src/test/.../ArticleControllerIT.java` | `PostgreSQLContainer`에 `@ServiceConnection` 붙이면 R2DBC `ConnectionFactory` 자동 wiring (`spring-boot-starter-data-r2dbc`와 `testcontainers:r2dbc` 둘 다 classpath에 있어야 동작) |

## 실제 앱에서의 마이그레이션

R2DBC용 `spring.sql.init`은 JDBC와 동일하게 동작하고 학습 데모엔 OK지만 실제 앱에선:
- **Flyway**는 [`flyway-database-postgresql`](https://documentation.red-gate.com/fd/postgresql-184127604.html)을 갖고 v10부터 R2DBC 지원
- **Liquibase**는 R2DBC 앱에서도 JDBC로 동작 — 보통 마이그레이션 전용 별도 `DataSource`를 wiring하고 앱은 런타임에 R2DBC 사용

## 빌드 검증

```bash
./gradlew build
```

`ArticleControllerIT`가 Testcontainers + `@ServiceConnection`으로 단명 PostgreSQL 컨테이너 상대 실행. 첫 실행은 `postgres:16-alpine` 풀 (~80MB); 이후는 캐시 사용.
