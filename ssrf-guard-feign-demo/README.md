# ssrf-guard-feign-demo

**English** · [한국어](README.ko.md)

Runnable example for [`ssrf-guard-feign`](https://github.com/devslab-kr/ssrf-guard) — SSRF protection for Spring Cloud OpenFeign clients.

Two declarative `@FeignClient` interfaces share one `UrlPolicy`:
- `HttpBinClient` — points at `https://httpbin.org` (whitelisted) — calls succeed.
- `EvilClient` — points at `https://evil.com` (not whitelisted) — calls blocked at the Feign `RequestInterceptor` before any HTTP traffic leaves the JVM.

## Get just this demo

Each demo is a standalone Gradle project, so you can grab this one folder without
cloning the whole `devslab-examples` repo.

**With git (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set ssrf-guard-feign-demo
cd ssrf-guard-feign-demo
```

**Without git (folder only):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/ssrf-guard-feign-demo
cd ssrf-guard-feign-demo
```

## Run

```bash
cd ssrf-guard-feign-demo
./gradlew bootRun
```

## Try it

```bash
# Whitelisted host — hits httpbin.org for real
curl http://localhost:8080/feign/legit | jq

# Not whitelisted — blocked at the SSRF guard interceptor
curl http://localhost:8080/feign/evil | jq
# → { "status": "blocked", "reason": "blocked_host", ... }
```

## What to read

| File | Why |
| --- | --- |
| `build.gradle.kts` | `kr.devslab:ssrf-guard-feign:3.1.0` + `spring-cloud-starter-openfeign` |
| `HttpBinClient.java` / `EvilClient.java` | Two normal `@FeignClient` interfaces — no guard code |
| `FeignDemoController.java` | Catches `SsrfGuardException` (wrapped one level deep by Feign — the controller unwraps) |
| `application.yml` | `ssrf.guard.exact-hosts: [httpbin.org]` — that one line is the whitelist |

The Feign interceptor registers itself automatically — `ssrf-guard-feign-3.1.0` provides a Spring autoconfig that publishes a `feign.RequestInterceptor` bean, which Spring Cloud OpenFeign then applies to every `@FeignClient`.

## Verify the build

```bash
./gradlew build
```
