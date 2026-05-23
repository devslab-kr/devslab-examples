# devslab-examples

**English** · [한국어](README.ko.md)

Runnable examples for [devslab-kr](https://github.com/devslab-kr) Spring Boot starters and libraries.

Each subdirectory is an **independent** Spring Boot application with its own Gradle build. Pick one, `cd` into it, and run `./gradlew bootRun`.

> 💬 Questions, ideas, or sharing your application of the demos? Head to [**Discussions**](https://github.com/devslab-kr/devslab-examples/discussions) — bilingual (English / Korean), maintained by the same folks who write the libraries.

## Examples

### easy-paging — Spring Boot 4 (`0.5.x` line)

Latest active line. Use these if your app is on Spring Boot 4+.

| Demo | Showcases | Maven Central coordinates |
| --- | --- | --- |
| [`easy-paging-sb4-demo`](easy-paging-sb4-demo/) | Annotation-driven offset pagination with `@AutoPaginate` (Spring Boot 4 + MyBatis + H2) | [`kr.devslab:easy-paging-spring-boot-starter:0.5.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-keyset-demo`](easy-paging-sb4-keyset-demo/) | Cursor (keyset) pagination with `@KeysetPaginate` — composite `(time, id)` key, stable under writes, no `OFFSET`/`COUNT(*)` | [`kr.devslab:easy-paging-spring-boot-starter:0.5.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-postgres-demo`](easy-paging-sb4-postgres-demo/) | Same starter against **real PostgreSQL** — Docker Compose for `bootRun`, Testcontainers + `@ServiceConnection` for tests, no local DB install | [`kr.devslab:easy-paging-spring-boot-starter:0.5.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-sb4-reactive-demo`](easy-paging-sb4-reactive-demo/) | Reactive stack — **WebFlux + R2DBC** via `R2dbcOffsetPagingSupport`. Same JSON envelope as the MVC demos, served as `Mono<PageResponse<T>>` | [`kr.devslab:easy-paging-spring-boot-starter-reactive:0.5.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter-reactive) |

### easy-paging — Spring Boot 3 maintenance (`0.4.x` line)

For apps still on Spring Boot 3.3–3.5. The starter's [`0.4.x` branch](https://github.com/devslab-kr/easy-paging-spring-boot-starter/tree/0.4.x) continues to receive SB3 security patches; these demos pin against that line.

| Demo | Showcases | Maven Central coordinates |
| --- | --- | --- |
| [`easy-paging-demo`](easy-paging-demo/) | Annotation-driven offset pagination with `@AutoPaginate` (Spring Boot 3 + MyBatis + H2) | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-keyset-demo`](easy-paging-keyset-demo/) | Cursor (keyset) pagination with `@KeysetPaginate` | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-postgres-demo`](easy-paging-postgres-demo/) | Same starter against real PostgreSQL | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-reactive-demo`](easy-paging-reactive-demo/) | Reactive stack — WebFlux + R2DBC | [`kr.devslab:easy-paging-spring-boot-starter-reactive:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter-reactive) |

### ssrf-guard

| Demo | Showcases | Maven Central coordinates |
| --- | --- | --- |
| [`ssrf-guard-demo`](ssrf-guard-demo/) | SSRF (Server-Side Request Forgery) protection across three Spring HTTP clients (RestClient, RestTemplate, WebClient) — same `UrlPolicy` for all. 15-pattern attack matrix endpoint, Micrometer metrics. | [`kr.devslab:ssrf-guard:3.1.0`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard) |
| [`ssrf-guard-springai-demo`](ssrf-guard-springai-demo/) | ⭐ **LLM agent SSRF defense (Spring AI).** Wraps every Spring AI `ToolCallback` so URL-shaped tool arguments are validated before the LLM-driven `fetch_url` runs. Fake-LLM driver makes the demo runnable offline (no API key). | [`kr.devslab:ssrf-guard-springai:3.1.0`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-springai) |
| [`ssrf-guard-langchain4j-demo`](ssrf-guard-langchain4j-demo/) | ⭐ **LLM agent SSRF defense (LangChain4j).** Same story for the other major Java LLM framework — wraps every `ToolExecutor` bean and validates `ToolExecutionRequest.arguments()` JSON before the executor runs. | [`kr.devslab:ssrf-guard-langchain4j:3.1.0`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-langchain4j) |
| [`ssrf-guard-feign-demo`](ssrf-guard-feign-demo/) | Spring Cloud OpenFeign `RequestInterceptor` — same `UrlPolicy` applied to `@FeignClient` calls. Two `@FeignClient` interfaces (one whitelisted, one not) to show the block path. | [`kr.devslab:ssrf-guard-feign:3.1.0`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-feign) |
| [`ssrf-guard-jdkhttp-demo`](ssrf-guard-jdkhttp-demo/) | `java.net.http.HttpClient` (Java 11+) wrapper — no Spring required by the library. Three-line wiring in `main()`. | [`kr.devslab:ssrf-guard-jdkhttp:3.1.0`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-jdkhttp) |
| [`ssrf-guard-okhttp-demo`](ssrf-guard-okhttp-demo/) | OkHttp `Interceptor` + `Dns` integration — also no Spring needed. Three-line wiring on `OkHttpClient.Builder`. | [`kr.devslab:ssrf-guard-okhttp:3.1.0`](https://central.sonatype.com/artifact/kr.devslab/ssrf-guard-okhttp) |

## Conventions

- Each demo is a **standalone Gradle project** — its own `settings.gradle.kts`, `build.gradle.kts`, and `gradlew`. Demos do not share a root build, so their dependency versions and JDK targets can drift independently.
- Each demo depends on the **latest stable release** of the starter it showcases (pinned by version in `build.gradle.kts`). Dependabot bumps it on new releases.
- This repo is **not versioned or tagged** — demos are not published artifacts. `main` is the source of truth.
- Each demo has its own `README.md` with quickstart, prerequisites, and a tour of what the starter is doing.

## Adding a new demo

1. Create `<starter-shortname>-demo/` at the repo root.
2. Copy the layout of an existing demo (e.g. `easy-paging-demo/`) as a template.
3. Add a row to the table above linking to the demo and to its starter on Maven Central.
4. CI auto-detects the new demo from the presence of `build.gradle.kts` — no workflow changes needed.

## CI

Pull requests build only the demos whose files changed. Pushes to `main` build every demo (catches drift from starter version bumps).
