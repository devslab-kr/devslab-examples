# devslab-examples

Runnable examples for [devslab-kr](https://github.com/devslab-kr) Spring Boot starters and libraries.

Each subdirectory is an **independent** Spring Boot application with its own Gradle build. Pick one, `cd` into it, and run `./gradlew bootRun`.

## Examples

| Demo | Showcases | Maven Central coordinates |
| --- | --- | --- |
| [`easy-paging-demo`](easy-paging-demo/) | Annotation-driven offset pagination with `@AutoPaginate` (Spring Boot + MyBatis + H2) | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-keyset-demo`](easy-paging-keyset-demo/) | Cursor (keyset) pagination with `@KeysetPaginate` — composite `(time, id)` key, stable under writes, no `OFFSET`/`COUNT(*)` | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |
| [`easy-paging-postgres-demo`](easy-paging-postgres-demo/) | Same starter against **real PostgreSQL** — Docker Compose for `bootRun`, Testcontainers + `@ServiceConnection` for tests, no local DB install | [`kr.devslab:easy-paging-spring-boot-starter:0.4.0`](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter) |

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
