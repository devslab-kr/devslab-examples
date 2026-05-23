# devslab-examples

Runnable examples for [devslab-kr](https://github.com/devslab-kr) Spring Boot starters and libraries.

Each subdirectory is an **independent** Spring Boot application with its own Gradle build. Pick one, `cd` into it, and run `./gradlew bootRun`.

## Examples

| Demo | Showcases | Maven Central coordinates |
| --- | --- | --- |
| _(none yet — first demo coming in a follow-up PR)_ | — | — |

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
