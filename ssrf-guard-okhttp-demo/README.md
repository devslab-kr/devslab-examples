# ssrf-guard-okhttp-demo

Runnable example for [`ssrf-guard-okhttp`](https://github.com/devslab-kr/ssrf-guard) — SSRF protection for OkHttp clients.

**No Spring needed for the library itself.** The demo wraps Spring Boot around the wiring to give a curl-friendly endpoint, but the actual integration is three lines on `OkHttpClient.Builder`.

## Run

```bash
cd ssrf-guard-okhttp-demo
./gradlew bootRun
```

## Try it

```bash
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq
curl 'http://localhost:8080/fetch?url=http://2130706433/' | jq
curl 'http://localhost:8080/fetch?url=https://evil.com/' | jq
```

## What to read

| File | Why |
| --- | --- |
| `build.gradle.kts` | `kr.devslab:ssrf-guard-okhttp:3.0.1` + `com.squareup.okhttp3:okhttp:4.12.0` |
| `SsrfGuardOkHttpDemoApplication.java` | Three lines on the OkHttp builder — `.addInterceptor(...)`, `.dns(...)`, `.followRedirects(...)` |
| `OkHttpDemoController.java` | Standard OkHttp `newCall().execute()` — the wrap is invisible at the call site |

## Using outside Spring

The wiring is just stock OkHttp builder calls — no Spring required:

```java
HostPolicy hostPolicy = new HostPolicy(
    List.of("api.partner.com"),
    List.of()
);
UrlPolicy urlPolicy = new UrlPolicy(
    Set.of("https"),
    Set.of(-1, 443),
    hostPolicy,
    true,   // rejectIpLiteralHosts
    true,   // rejectUserInfo
    NoOpSsrfGuardMetrics.INSTANCE
);

OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(new SsrfGuardOkHttpInterceptor(urlPolicy))
    .dns(new SsrfGuardOkHttpDns(hostPolicy, true))    // blockPrivate=true
    .build();
```

Useful for Android apps (OkHttp is the de facto Android HTTP client), Retrofit-backed services, or any non-Spring JVM consumer of OkHttp.

## Verify the build

```bash
./gradlew build
```
