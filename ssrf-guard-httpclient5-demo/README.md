# ssrf-guard-httpclient5-demo

**English** · [한국어](README.ko.md)

Runnable example for [`ssrf-guard-httpclient5`](https://github.com/devslab-kr/ssrf-guard) — SSRF protection for Apache HttpClient 5.

**Different shape from the other ssrf-guard demos.** Apache HttpClient 5 hooks the SSRF policy at **DNS-resolution time**, not URL-parse time. That's the right gate for HttpClient because it closes the TOCTOU window between "you checked the URL" and "the socket opens".

## What the module does

Two extension points on `HttpClients.custom()`:

| Plug-in point | What it does |
| --- | --- |
| [`DnsResolver`](https://hc.apache.org/httpcomponents-client-5.4.x/current/httpclient5/apidocs/org/apache/hc/client5/http/DnsResolver.html) | `SafeDnsResolver` — refuses to resolve hosts outside the whitelist; filters private / loopback / link-local / cloud-metadata IPs out of the resolved set; if nothing is left, throws `UnknownHostException` so `Socket.connect()` never happens. |
| [`RedirectStrategy`](https://hc.apache.org/httpcomponents-client-5.4.x/current/httpclient5/apidocs/org/apache/hc/client5/http/protocol/RedirectStrategy.html) | `SafeRedirectStrategy` — runs scheme check + the same DNS gate on every redirect hop. Closes the "whitelist `example.com`, then it 302s to `http://169.254.169.254/`" attack. |

The `InetAddress[]` `SafeDnsResolver` returns is the exact same array HttpClient passes to `Socket.connect()` — so there's no second DNS lookup between validation and connection. That's the TOCTOU window closed.

## Run

```bash
cd ssrf-guard-httpclient5-demo
./gradlew bootRun
```

## Try it

```bash
# Allowed — host in whitelist
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq

# Blocked — AWS metadata (link-local IP filtered out at DNS gate)
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq

# Blocked — decimal-encoded 127.0.0.1
curl 'http://localhost:8080/fetch?url=http://2130706433/' | jq

# Blocked — host not in whitelist (SafeDnsResolver refuses upfront)
curl 'http://localhost:8080/fetch?url=https://evil.com/' | jq
```

Blocked responses look like:

```json
{
  "client": "Apache HttpClient 5",
  "url": "http://169.254.169.254/",
  "status": "blocked",
  "reason": "blocked_dns",
  "message": "No allowed IP after filtering: 169.254.169.254"
}
```

`reason: "blocked_dns"` covers both:
- *"Host not in whitelist: <host>"* — `SafeDnsResolver` refused before even calling `InetAddress.getAllByName`.
- *"No allowed IP after filtering: <host>"* — DNS returned IPs but every one was in a blocked range.

## What to read

| File | Why |
| --- | --- |
| `build.gradle.kts` | Two deps: `kr.devslab:ssrf-guard-httpclient5:3.1.0` + `org.apache.httpcomponents.client5:httpclient5:5.4.1` |
| `SsrfGuardHttpClient5DemoApplication.java` | Just `@SpringBootApplication`. The module's autoconfig wires the guarded `CloseableHttpClient` bean — **zero lines of wiring code**. |
| `HttpClient5DemoController.java` | Standard `client.execute(get, handler)` call — no reference to the guard at all |
| `application.yml` | `ssrf.guard.*` keys: whitelist, `block-private-networks`, `follow-redirects`, `allowed-schemes` |

## Without Spring

The module's autoconfig is a thin wrapper around five lines:

```java
HostPolicy hostPolicy = new HostPolicy(
    List.of("api.partner.com"),
    List.of());
SafeDnsResolver dns = new SafeDnsResolver(hostPolicy, /* blockPrivate */ true,
                                          NoOpSsrfGuardMetrics.INSTANCE);

CloseableHttpClient client = HttpClients.custom()
    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
        .setDnsResolver(dns)
        .build())
    .setRedirectStrategy(new SafeRedirectStrategy(
        dns, List.of("http", "https"), NoOpSsrfGuardMetrics.INSTANCE))
    .build();
```

Drop into any non-Spring JVM app (Quarkus, Helidon, Lambda, plain `main`).

## How this compares to other ssrf-guard demos

| Demo | Where the URL is checked | Library |
| --- | --- | --- |
| `ssrf-guard-demo` (RestClient / RestTemplate / WebClient) | URL-parse time (`ClientHttpRequestInterceptor`) + DNS-time for WebClient (reactor-netty AddressResolverGroup) | Spring HTTP stack |
| `ssrf-guard-feign-demo` | URL-parse time (`RequestInterceptor`) | Spring Cloud OpenFeign |
| `ssrf-guard-okhttp-demo` | URL-parse time (`Interceptor`) + DNS-time (`Dns` SPI) | OkHttp |
| `ssrf-guard-jdkhttp-demo` | URL-parse time (wrapper) | `java.net.http.HttpClient` |
| **`ssrf-guard-httpclient5-demo` (this)** | **DNS-time only** (`DnsResolver` + `RedirectStrategy`) | Apache HttpClient 5 |
| `ssrf-guard-springai-demo` / `-langchain4j-demo` | LLM tool argument JSON validation | Spring AI / LangChain4j |

The DNS-time-only approach is enough because:
- IP literals (`http://169.254.169.254/`, decimal/hex/octal-encoded loopbacks) all decode to the same `InetAddress[]` the DNS resolver returns — the private-IP filter catches them.
- Hosts not in the whitelist never reach the resolver's `getAllByName` call.
- DNS rebinding and late-binding A-records are caught at the same gate — there's no second lookup the attacker can race against.

What it *doesn't* catch (compared to URL-parse-time gates): scheme restrictions and `https://user:pass@host/` userinfo rejection. Those need a different shape and aren't in the `-httpclient5` module today.

## Verify the build

```bash
./gradlew build
```

Runs the smoke tests in `SsrfGuardHttpClient5DemoApplicationTests` — checks that AWS-metadata, decimal-encoded loopback, and non-whitelisted hosts all block at the DNS gate before any socket opens.
