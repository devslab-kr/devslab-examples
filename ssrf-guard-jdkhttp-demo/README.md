# ssrf-guard-jdkhttp-demo

**English** · [한국어](README.ko.md)

Runnable example for [`ssrf-guard-jdkhttp`](https://github.com/devslab-kr/ssrf-guard) — SSRF protection for the JDK standard `java.net.http.HttpClient` (Java 11+).

**No Spring needed for the library itself.** This demo uses Spring Boot only to expose a REST endpoint for curl. The actual SSRF Guard wiring is three lines in `SsrfGuardJdkHttpDemoApplication.java`.

## Get just this demo

Each demo is a standalone Gradle project, so you can grab this one folder without
cloning the whole `devslab-examples` repo.

**With git (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set ssrf-guard-jdkhttp-demo
cd ssrf-guard-jdkhttp-demo
```

**Without git (folder only):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/ssrf-guard-jdkhttp-demo
cd ssrf-guard-jdkhttp-demo
```

## Run

```bash
cd ssrf-guard-jdkhttp-demo
./gradlew bootRun
```

## Try it

```bash
# Allowed
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq

# AWS metadata — blocked at the URL-time IP-literal check
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq

# Decimal-encoded loopback
curl 'http://localhost:8080/fetch?url=http://2130706433/' | jq

# Disallowed host
curl 'http://localhost:8080/fetch?url=https://evil.com/' | jq
```

## What to read

| File | Why |
| --- | --- |
| `build.gradle.kts` | One dep: `kr.devslab:ssrf-guard-jdkhttp:3.1.1` |
| `SsrfGuardJdkHttpDemoApplication.java` | The whole story: build `HostPolicy` → `UrlPolicy` → wrap `HttpClient` |
| `JdkHttpDemoController.java` | Calls `client.send(req, ...)` like any other HttpClient — the wrap is invisible at the call site |

## Using outside Spring

The wiring above isn't Spring-specific — the policy classes (`HostPolicy`, `UrlPolicy`, `SsrfGuardedHttpClient`) are POJOs:

```java
HostPolicy hostPolicy = new HostPolicy(
    List.of("api.partner.com"),  // exactHosts
    List.of()                    // suffixes
);
UrlPolicy urlPolicy = new UrlPolicy(
    Set.of("https"),
    Set.of(-1, 443),
    hostPolicy,
    true,   // rejectIpLiteralHosts
    true,   // rejectUserInfo
    NoOpSsrfGuardMetrics.INSTANCE
);
SsrfGuardedHttpClient safe = new SsrfGuardedHttpClient(
    HttpClient.newHttpClient(), urlPolicy, true);

// Use exactly like java.net.http.HttpClient
HttpResponse<String> resp = safe.send(
    HttpRequest.newBuilder(URI.create("https://api.partner.com/")).build(),
    HttpResponse.BodyHandlers.ofString());
```

Useful for Lambda, AWS SDK consumers, Quarkus apps, CLI tools — anywhere `java.net.http` is in use without Spring.

## Verify the build

```bash
./gradlew build
```
