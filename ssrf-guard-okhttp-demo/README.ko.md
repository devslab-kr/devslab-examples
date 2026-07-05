# ssrf-guard-okhttp-demo

[English](README.md) · **한국어**

[`ssrf-guard-okhttp`](https://github.com/devslab-kr/ssrf-guard) — OkHttp 클라이언트용 SSRF 방어 예제.

**라이브러리 자체엔 Spring 필요 없음.** 데모가 Spring Boot로 감싸서 curl 친화적 엔드포인트를 제공하지만, 실제 통합은 `OkHttpClient.Builder` 3줄.

## 이 데모만 받기

각 데모는 독립 Gradle 프로젝트라, `devslab-examples` 저장소 전체를 clone하지 않고
이 폴더만 받을 수 있습니다.

**git 사용 (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set ssrf-guard-okhttp-demo
cd ssrf-guard-okhttp-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/ssrf-guard-okhttp-demo
cd ssrf-guard-okhttp-demo
```

## 실행

```bash
cd ssrf-guard-okhttp-demo
./gradlew bootRun
```

## 시험해보기

```bash
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq
curl 'http://localhost:8080/fetch?url=http://2130706433/' | jq
curl 'http://localhost:8080/fetch?url=https://evil.com/' | jq
```

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | `kr.devslab:ssrf-guard-okhttp:3.1.1` + `com.squareup.okhttp3:okhttp:4.12.0` |
| `SsrfGuardOkHttpDemoApplication.java` | OkHttp 빌더에 3줄 — `.addInterceptor(...)`, `.dns(...)`, `.followRedirects(...)` |
| `OkHttpDemoController.java` | 표준 OkHttp `newCall().execute()` — 호출부에서 wrap은 보이지 않음 |

## Spring 없이 사용

wiring은 평범한 OkHttp 빌더 호출입니다 — Spring 불필요:

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

Android 앱 (OkHttp가 사실상 표준 HTTP 클라이언트), Retrofit 기반 서비스, OkHttp 쓰는 모든 비-Spring JVM 환경에 유용.

## 빌드 검증

```bash
./gradlew build
```
