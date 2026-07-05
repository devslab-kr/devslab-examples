# ssrf-guard-httpclient5-demo

[English](README.md) · **한국어**

[`ssrf-guard-httpclient5`](https://github.com/devslab-kr/ssrf-guard) — Apache HttpClient 5에 대한 SSRF 방어 실행 가능 예제.

**다른 ssrf-guard 데모와는 다른 모양.** Apache HttpClient 5는 SSRF 정책을 URL parse 시점이 아니라 **DNS 해석 시점**에 hook합니다. "URL 검사했음"과 "소켓 열림" 사이의 TOCTOU 윈도우를 닫는 정확한 게이트.

## 모듈이 하는 일

`HttpClients.custom()`의 두 확장 지점:

| 플러그인 지점 | 역할 |
| --- | --- |
| [`DnsResolver`](https://hc.apache.org/httpcomponents-client-5.4.x/current/httpclient5/apidocs/org/apache/hc/client5/http/DnsResolver.html) | `SafeDnsResolver` — 화이트리스트 외 호스트 거부, 해석된 IP에서 private/loopback/link-local/cloud-metadata 필터, 남는 게 없으면 `UnknownHostException` throw — `Socket.connect()` 자체가 안 일어남. |
| [`RedirectStrategy`](https://hc.apache.org/httpcomponents-client-5.4.x/current/httpclient5/apidocs/org/apache/hc/client5/http/protocol/RedirectStrategy.html) | `SafeRedirectStrategy` — 매 redirect 홉에서 scheme 검사 + 동일한 DNS 게이트 재실행. "`example.com` 화이트리스트, 그 다음 302 `http://169.254.169.254/`" 공격 차단. |

`SafeDnsResolver`가 반환하는 `InetAddress[]`는 HttpClient가 `Socket.connect()`에 그대로 전달하는 배열 — 검증과 연결 사이에 두 번째 DNS 조회 없음. TOCTOU 윈도우가 거기서 닫힙니다.

## 이 데모만 받기

각 데모는 독립 Gradle 프로젝트라, `devslab-examples` 저장소 전체를 clone하지 않고
이 폴더만 받을 수 있습니다.

**git 사용 (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set ssrf-guard-httpclient5-demo
cd ssrf-guard-httpclient5-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/ssrf-guard-httpclient5-demo
cd ssrf-guard-httpclient5-demo
```

## 실행

```bash
cd ssrf-guard-httpclient5-demo
./gradlew bootRun
```

## 시험해보기

```bash
# 허용 — 화이트리스트 호스트
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq

# 차단 — AWS 메타데이터 (link-local IP가 DNS 게이트에서 필터됨)
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq

# 차단 — 10진수 인코딩된 127.0.0.1
curl 'http://localhost:8080/fetch?url=http://2130706433/' | jq

# 차단 — 화이트리스트 외 호스트 (`SafeDnsResolver`가 사전 거부)
curl 'http://localhost:8080/fetch?url=https://evil.com/' | jq
```

차단된 응답 예:

```json
{
  "client": "Apache HttpClient 5",
  "url": "http://169.254.169.254/",
  "status": "blocked",
  "reason": "blocked_dns",
  "message": "No allowed IP after filtering: 169.254.169.254"
}
```

`reason: "blocked_dns"`는 두 케이스를 커버:
- *"Host not in whitelist: <host>"* — `SafeDnsResolver`가 `InetAddress.getAllByName` 호출하기도 전에 거부.
- *"No allowed IP after filtering: <host>"* — DNS는 IP를 반환했지만 모두 차단 범위였음.

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | 의존성 둘: `kr.devslab:ssrf-guard-httpclient5:3.1.1` + `org.apache.httpcomponents.client5:httpclient5:5.4.1` |
| `SsrfGuardHttpClient5DemoApplication.java` | `@SpringBootApplication`만. 모듈의 자동 설정이 가드된 `CloseableHttpClient` 빈을 wire — **wiring 코드 0줄**. |
| `HttpClient5DemoController.java` | 표준 `client.execute(get, handler)` 호출 — 가드 참조 0 |
| `application.yml` | `ssrf.guard.*` 키: 화이트리스트, `block-private-networks`, `follow-redirects`, `allowed-schemes` |

## Spring 없이

모듈 자동 설정은 5줄 wrapping에 불과:

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

Spring 없는 모든 JVM 앱 (Quarkus, Helidon, Lambda, 순수 `main`)에 drop-in.

## 다른 ssrf-guard 데모와의 비교

| 데모 | URL 검사 시점 | 라이브러리 |
| --- | --- | --- |
| `ssrf-guard-demo` (RestClient / RestTemplate / WebClient) | URL parse 시점 (`ClientHttpRequestInterceptor`) + WebClient는 DNS 시점도 (reactor-netty AddressResolverGroup) | Spring HTTP 스택 |
| `ssrf-guard-feign-demo` | URL parse 시점 (`RequestInterceptor`) | Spring Cloud OpenFeign |
| `ssrf-guard-okhttp-demo` | URL parse 시점 (`Interceptor`) + DNS 시점 (`Dns` SPI) | OkHttp |
| `ssrf-guard-jdkhttp-demo` | URL parse 시점 (래퍼) | `java.net.http.HttpClient` |
| **`ssrf-guard-httpclient5-demo` (이것)** | **DNS 시점만** (`DnsResolver` + `RedirectStrategy`) | Apache HttpClient 5 |
| `ssrf-guard-springai-demo` / `-langchain4j-demo` | LLM 툴 인자 JSON 검증 | Spring AI / LangChain4j |

DNS 시점만으로 충분한 이유:
- IP 리터럴 (`http://169.254.169.254/`, decimal/hex/octal 인코딩 loopback) 모두 같은 `InetAddress[]`로 디코딩됨 — private-IP 필터가 잡음.
- 화이트리스트 외 호스트는 resolver의 `getAllByName` 호출 자체에 도달 안 함.
- DNS rebinding과 late-binding A-record도 같은 게이트에서 잡힘 — 공격자가 race할 두 번째 lookup이 없음.

URL-parse 시점 게이트와 비교해 *못 잡는* 것: scheme 제한과 `https://user:pass@host/` userinfo 거부. 다른 모양이 필요해서 현재 `-httpclient5` 모듈에는 없습니다.

## 빌드 검증

```bash
./gradlew build
```

`SsrfGuardHttpClient5DemoApplicationTests` 스모크 테스트가 AWS 메타데이터, 10진수 loopback, 비화이트리스트 호스트 모두 DNS 게이트에서 차단됨을 검증 — 어떤 소켓도 열리기 전에.
