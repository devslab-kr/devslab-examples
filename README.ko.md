# devslab-examples

[English](README.md) · **한국어**

[devslab-kr](https://github.com/devslab-kr) Spring Boot 스타터와 라이브러리들의 실행 가능한 예제 모음.

각 서브디렉토리는 자체 Gradle 빌드를 가진 **독립 Spring Boot 앱**입니다. 골라서 `cd` 하고 `./gradlew bootRun` 만 하면 됩니다.

> 💬 질문, 아이디어, 데모 응용 사례 공유는 [**Discussions**](https://github.com/devslab-kr/devslab-examples/discussions)에서 — 영어/한국어 둘 다 환영, 라이브러리 만든 메인테이너가 직접 답변합니다.

## 예제

### easy-paging — Spring Boot 4 (`0.5.x` 라인)

최신 active 라인. Spring Boot 4 이상 사용 중이면 여기.

| 데모 | 보여주는 것 | Maven Central 좌표 |
| --- | --- | --- |
| [`easy-paging-sb4-demo`](easy-paging-sb4-demo/) | `@AutoPaginate` 어노테이션 기반 offset 페이지네이션 (Spring Boot 4 + MyBatis + H2) | [`kr.devslab:easy-paging-spring-boot-starter:0.5.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-keyset-demo`](easy-paging-sb4-keyset-demo/) | `@KeysetPaginate` 커서(keyset) 페이지네이션 — composite `(time, id)` 키, 쓰기 중에도 안정, `OFFSET`/`COUNT(*)` 없음 | [`kr.devslab:easy-paging-spring-boot-starter:0.5.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-postgres-demo`](easy-paging-sb4-postgres-demo/) | 동일 스타터를 **실제 PostgreSQL**과 — `bootRun`은 Docker Compose, 테스트는 Testcontainers + `@ServiceConnection`, 로컬 DB 설치 불필요 | [`kr.devslab:easy-paging-spring-boot-starter:0.5.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-reactive-demo`](easy-paging-sb4-reactive-demo/) | Reactive 스택 — **WebFlux + R2DBC**, `R2dbcOffsetPagingSupport` 사용. MVC 데모와 동일한 JSON 봉투를 `Mono<PageResponse<T>>`로 서빙 | [`kr.devslab:easy-paging-spring-boot-starter-reactive:0.5.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter-reactive) |

### easy-paging — Spring Boot 3 maintenance (`0.4.x` 라인)

Spring Boot 3.3–3.5 사용 중인 앱용. 스타터의 [`0.4.x` 브랜치](https://github.com/devslab-kr/easy-paging-spring-boot-starter/tree/0.4.x)가 SB3 보안 패치를 계속 받고, 이 데모들은 그 라인에 pin 됨.

| 데모 | 보여주는 것 | Maven Central 좌표 |
| --- | --- | --- |
| [`easy-paging-demo`](easy-paging-demo/) | `@AutoPaginate` 어노테이션 기반 offset 페이지네이션 (Spring Boot 3 + MyBatis + H2) | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-keyset-demo`](easy-paging-keyset-demo/) | `@KeysetPaginate` 커서 페이지네이션 | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-postgres-demo`](easy-paging-postgres-demo/) | 실제 PostgreSQL 사용 | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-reactive-demo`](easy-paging-reactive-demo/) | Reactive 스택 — WebFlux + R2DBC | [`kr.devslab:easy-paging-spring-boot-starter-reactive:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter-reactive) |

### ssrf-guard

| 데모 | 보여주는 것 | Maven Central 좌표 |
| --- | --- | --- |
| [`ssrf-guard-demo`](ssrf-guard-demo/) | SSRF(Server-Side Request Forgery) 방어를 3종 Spring HTTP 클라이언트(RestClient, RestTemplate, WebClient)에 동시 적용 — 모두 같은 `UrlPolicy`. 15가지 공격 매트릭스 엔드포인트, Micrometer 메트릭 포함 | [`kr.devslab:ssrf-guard:3.0.1`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard) |
| [`ssrf-guard-springai-demo`](ssrf-guard-springai-demo/) | ⭐ **LLM 에이전트 SSRF 방어.** 모든 Spring AI `ToolCallback`을 자동으로 wrap해서 LLM이 `fetch_url`을 호출하기 전에 URL 인자를 검증. 가짜 LLM 드라이버로 API 키 없이 오프라인 실행 가능 | [`kr.devslab:ssrf-guard-springai:3.0.1`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-springai) |
| [`ssrf-guard-feign-demo`](ssrf-guard-feign-demo/) | Spring Cloud OpenFeign `RequestInterceptor` — `@FeignClient` 호출에 동일 `UrlPolicy` 적용. 화이트리스트 / 비화이트리스트 `@FeignClient` 2개로 차단 경로 시연 | [`kr.devslab:ssrf-guard-feign:3.0.1`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-feign) |
| [`ssrf-guard-jdkhttp-demo`](ssrf-guard-jdkhttp-demo/) | `java.net.http.HttpClient`(Java 11+) 래퍼 — 라이브러리 자체엔 Spring 의존성 없음. `main()`에서 3줄 wiring | [`kr.devslab:ssrf-guard-jdkhttp:3.0.1`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-jdkhttp) |
| [`ssrf-guard-okhttp-demo`](ssrf-guard-okhttp-demo/) | OkHttp `Interceptor` + `Dns` — Spring 필요 없음. `OkHttpClient.Builder`에 3줄 wiring | [`kr.devslab:ssrf-guard-okhttp:3.0.1`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-okhttp) |

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
