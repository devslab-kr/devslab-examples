# devslab-examples

[English](README.md) · **한국어**

[devslab-kr](https://github.com/devslab-kr) Spring Boot 스타터와 라이브러리들의 실행 가능한 예제 모음.

각 서브디렉토리는 자체 Gradle 빌드를 가진 **독립 Spring Boot 앱**입니다. 골라서 `cd` 하고 `./gradlew bootRun` 만 하면 됩니다.

> 💬 질문, 아이디어, 데모 응용 사례 공유는 [**Discussions**](https://github.com/devslab-kr/devslab-examples/discussions)에서 — 영어/한국어 둘 다 환영, 라이브러리 만든 메인테이너가 직접 답변합니다.

## 예제

### easy-paging — Spring Boot 4 (`4.x` 라인)

최신 active 라인. Spring Boot 4 이상 사용 중이면 여기.

| 데모 | 보여주는 것 | Maven Central |
| --- | --- | --- |
| [`easy-paging-sb4-demo`](easy-paging-sb4-demo/) | `@AutoPaginate` 어노테이션 기반 offset 페이지네이션 (Spring Boot 4 + MyBatis + H2) | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter?label=kr.devslab%3Aeasy-paging-spring-boot-starter&versionPrefix=4)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-keyset-demo`](easy-paging-sb4-keyset-demo/) | `@KeysetPaginate` 커서(keyset) 페이지네이션 — composite `(time, id)` 키, 쓰기 중에도 안정, `OFFSET`/`COUNT(*)` 없음 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter?label=kr.devslab%3Aeasy-paging-spring-boot-starter&versionPrefix=4)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-postgres-demo`](easy-paging-sb4-postgres-demo/) | 동일 스타터를 **실제 PostgreSQL**과 — `bootRun`은 Docker Compose, 테스트는 Testcontainers + `@ServiceConnection`, 로컬 DB 설치 불필요 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter?label=kr.devslab%3Aeasy-paging-spring-boot-starter&versionPrefix=4)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-reactive-demo`](easy-paging-sb4-reactive-demo/) | Reactive 스택 — **WebFlux + R2DBC**, `R2dbcOffsetPagingSupport` 사용. MVC 데모와 동일한 JSON 봉투를 `Mono<PageResponse<T>>`로 서빙 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter-reactive?label=kr.devslab%3Aeasy-paging-spring-boot-starter-reactive&versionPrefix=4)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter-reactive) |

### easy-paging — Spring Boot 3 maintenance (`3.x` 라인)

Spring Boot 3.3–3.5 사용 중인 앱용. 스타터의 [`3.x` 브랜치](https://github.com/devslab-kr/easy-paging-spring-boot-starter/tree/3.x)가 SB3 보안 패치를 계속 받고, 이 데모들은 그 라인에 pin 됨.

| 데모 | 보여주는 것 | Maven Central |
| --- | --- | --- |
| [`easy-paging-demo`](easy-paging-demo/) | `@AutoPaginate` 어노테이션 기반 offset 페이지네이션 (Spring Boot 3 + MyBatis + H2) | [![Maven Central 3.x](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter?label=kr.devslab%3Aeasy-paging-spring-boot-starter&versionPrefix=3)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-keyset-demo`](easy-paging-keyset-demo/) | `@KeysetPaginate` 커서 페이지네이션 | [![Maven Central 3.x](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter?label=kr.devslab%3Aeasy-paging-spring-boot-starter&versionPrefix=3)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-postgres-demo`](easy-paging-postgres-demo/) | 실제 PostgreSQL 사용 | [![Maven Central 3.x](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter?label=kr.devslab%3Aeasy-paging-spring-boot-starter&versionPrefix=3)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-reactive-demo`](easy-paging-reactive-demo/) | Reactive 스택 — WebFlux + R2DBC | [![Maven Central 3.x](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter-reactive?label=kr.devslab%3Aeasy-paging-spring-boot-starter-reactive&versionPrefix=3)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter-reactive) |

### ssrf-guard

| 데모 | 보여주는 것 | Maven Central |
| --- | --- | --- |
| [`ssrf-guard-demo`](ssrf-guard-demo/) | SSRF(Server-Side Request Forgery) 방어를 3종 Spring HTTP 클라이언트(RestClient, RestTemplate, WebClient)에 동시 적용 — 모두 같은 `UrlPolicy`. 15가지 공격 매트릭스 엔드포인트, Micrometer 메트릭 포함 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/ssrf-guard?label=kr.devslab%3Assrf-guard)](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard) |
| [`ssrf-guard-springai-demo`](ssrf-guard-springai-demo/) | ⭐ **LLM 에이전트 SSRF 방어 (Spring AI).** 모든 Spring AI `ToolCallback`을 자동으로 wrap해서 LLM이 `fetch_url`을 호출하기 전에 URL 인자를 검증. 가짜 LLM 드라이버로 API 키 없이 오프라인 실행 가능 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/ssrf-guard-springai?label=kr.devslab%3Assrf-guard-springai)](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-springai) |
| [`ssrf-guard-langchain4j-demo`](ssrf-guard-langchain4j-demo/) | ⭐ **LLM 에이전트 SSRF 방어 (LangChain4j).** 자바의 또 다른 메이저 LLM 프레임워크용 — 모든 `ToolExecutor` 빈을 wrap, executor 실행 전에 `ToolExecutionRequest.arguments()` JSON을 검증 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/ssrf-guard-langchain4j?label=kr.devslab%3Assrf-guard-langchain4j)](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-langchain4j) |
| [`ssrf-guard-feign-demo`](ssrf-guard-feign-demo/) | Spring Cloud OpenFeign `RequestInterceptor` — `@FeignClient` 호출에 동일 `UrlPolicy` 적용. 화이트리스트 / 비화이트리스트 `@FeignClient` 2개로 차단 경로 시연 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/ssrf-guard-feign?label=kr.devslab%3Assrf-guard-feign)](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-feign) |
| [`ssrf-guard-jdkhttp-demo`](ssrf-guard-jdkhttp-demo/) | `java.net.http.HttpClient`(Java 11+) 래퍼 — 라이브러리 자체엔 Spring 의존성 없음. `main()`에서 3줄 wiring | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/ssrf-guard-jdkhttp?label=kr.devslab%3Assrf-guard-jdkhttp)](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-jdkhttp) |
| [`ssrf-guard-okhttp-demo`](ssrf-guard-okhttp-demo/) | OkHttp `Interceptor` + `Dns` — Spring 필요 없음. `OkHttpClient.Builder`에 3줄 wiring | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/ssrf-guard-okhttp?label=kr.devslab%3Assrf-guard-okhttp)](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-okhttp) |
| [`ssrf-guard-httpclient5-demo`](ssrf-guard-httpclient5-demo/) | Apache HttpClient 5 — **DNS 시점** SSRF 게이트 (`SafeDnsResolver`) + `SafeRedirectStrategy`. Spring에서 wiring 코드 0줄 (모듈이 자체 자동설정 제공); Spring 없으면 5줄. TOCTOU 차단 방식: 동일 `InetAddress[]`로 검증=연결 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/ssrf-guard-httpclient5?label=kr.devslab%3Assrf-guard-httpclient5)](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-httpclient5) |
| [`ssrf-guard-native-image-demo`](ssrf-guard-native-image-demo/) | ⚡ **GraalVM 네이티브 이미지** 증명. `ssrf-guard:3.1.0` 끌고 `org.graalvm.buildtools.native` plugin 적용, `nativeCompile`이 JVM 빌드와 동일한 12개 공격 패턴을 차단하는 동작하는 네이티브 바이너리를 만든다는 시연. ssrf-guard 3.1.0의 `RuntimeHintsRegistrar` 엔트리가 완전함을 end-to-end 검증 | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/ssrf-guard?label=kr.devslab%3Assrf-guard)](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard) |

### api-log

[api-log](https://github.com/devslab-kr/api-log) 스타터를 통한 비동기 API 호출 로깅 (PostgreSQL JSONB) — 영속 백엔드별 데모 1개씩. 모든 데모가 **self-loopback** 디자인: 같은 앱이 `/upstream/widgets` 엔드포인트 ("호출당하는 서비스")와 `/client/widgets` 엔드포인트 (api-log HTTP 클라이언트 유틸로 upstream 호출) 둘 다 노출. 세 번째 `/api-log` 엔드포인트가 `api_log` 테이블을 읽어줘서 INITIATED → SUCCESS / ERROR / RETRY_ERROR 전체 라이프사이클을 데모 안에서 curl로 바로 확인 가능. 로컬 DB는 Docker Compose, 통합 테스트는 Testcontainers + `@ServiceConnection`.

| 데모 | 보여주는 것 | Maven Central |
| --- | --- | --- |
| [`api-log-jpa-demo`](api-log-jpa-demo/) | **JPA 백엔드** — Spring MVC + `RestApiClientUtil` (블로킹) + `JpaApiLogWriter`. `ApiLogRepository` (Spring Data JPA)로 로그 행을 읽어옴. Servlet/JPA 앱의 가장 흔한 drop-in. | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/api-log-jpa?label=kr.devslab%3Aapi-log-jpa)](https://central.sonatype.com/artifact/kr.devslab/api-log-jpa) |
| [`api-log-mybatis-demo`](api-log-mybatis-demo/) | **MyBatis 백엔드** — Spring MVC + `RestApiClientUtil` + `MybatisApiLogWriter`. 번들 `ApiLogMapper`는 request_id 조회용, `recent` / `by-event` 쿼리는 데모가 커스텀 `ApiLogQueryMapper` (xml) 추가. 이미 MyBatis 쓰고 JPA 안 원하는 팀용. | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/api-log-mybatis?label=kr.devslab%3Aapi-log-mybatis)](https://central.sonatype.com/artifact/kr.devslab/api-log-mybatis) |
| [`api-log-r2dbc-demo`](api-log-r2dbc-demo/) | **R2DBC 백엔드 (리액티브)** — WebFlux + `ReactiveApiClientUtil` (`Mono` 기반) + `R2dbcApiLogWriter`. 리더는 `DatabaseClient`로 `Flux<ApiLogView>` 스트리밍. HTTP 경로 전체 논블로킹; api-log 쓰기도 논블로킹. 요청 경로에 JDBC가 전혀 없는 WebFlux 앱용. | [![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/api-log-r2dbc?label=kr.devslab%3Aapi-log-r2dbc)](https://central.sonatype.com/artifact/kr.devslab/api-log-r2dbc) |

## 컨벤션

- 각 데모는 **독립 Gradle 프로젝트** — 자체 `settings.gradle.kts`, `build.gradle.kts`, `gradlew`를 가짐. 루트 빌드를 공유하지 않으므로 의존성 버전이나 JDK 타겟이 독립적으로 변할 수 있음.
- 각 데모는 자기가 시연하는 스타터의 **최신 stable 릴리즈**에 의존 (`build.gradle.kts`에서 버전 핀). 새 릴리즈가 나오면 Dependabot이 bump.
- 이 repo는 **버전/태그 안 함** — 데모는 발행되는 아티팩트가 아님. `main`이 진실의 소스.
- 각 데모는 자체 `README.md`에 빠른 시작, 전제조건, 스타터가 하는 일의 가이드 포함.

## 새 데모 추가하기

1. `<starter-shortname>-demo/`를 repo 루트에 생성.
2. 기존 데모 (예: `easy-paging-demo/`) 레이아웃을 템플릿으로 복사.
3. 위 표에 데모 링크 + Maven Central 스타터 링크로 행 추가.
4. CI는 `build.gradle.kts` 존재로 새 데모를 자동 감지 — workflow 변경 불필요.

## CI

PR은 **변경된 파일이 있는 데모만** 빌드. `main` push는 **모든 데모** 빌드 (스타터 버전 bump로 인한 drift 잡기 위해).
