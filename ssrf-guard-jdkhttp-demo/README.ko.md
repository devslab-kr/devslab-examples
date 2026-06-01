# ssrf-guard-jdkhttp-demo

[English](README.md) · **한국어**

[`ssrf-guard-jdkhttp`](https://github.com/devslab-kr/ssrf-guard) — JDK 표준 `java.net.http.HttpClient` (Java 11+)용 SSRF 방어 예제.

**라이브러리 자체엔 Spring 필요 없음.** 이 데모는 REST 엔드포인트 노출을 위해서만 Spring Boot를 사용합니다. 실제 SSRF Guard wiring은 `SsrfGuardJdkHttpDemoApplication.java`의 3줄입니다.

## 이 데모만 받기

각 데모는 독립 Gradle 프로젝트라, `devslab-examples` 저장소 전체를 clone하지 않고
이 폴더만 받을 수 있습니다.

**git 사용 (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set ssrf-guard-jdkhttp-demo
cd ssrf-guard-jdkhttp-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/ssrf-guard-jdkhttp-demo
cd ssrf-guard-jdkhttp-demo
```

## 실행

```bash
cd ssrf-guard-jdkhttp-demo
./gradlew bootRun
```

## 시험해보기

```bash
# 허용
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq

# AWS 메타데이터 — URL 단계 IP-리터럴 체크에서 차단
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq

# 십진수 인코딩 loopback
curl 'http://localhost:8080/fetch?url=http://2130706433/' | jq

# 화이트리스트 밖 호스트
curl 'http://localhost:8080/fetch?url=https://evil.com/' | jq
```

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | 의존성 하나: `kr.devslab:ssrf-guard-jdkhttp:3.1.0` |
| `SsrfGuardJdkHttpDemoApplication.java` | 전체 스토리: `HostPolicy` → `UrlPolicy` → `HttpClient` wrap |
| `JdkHttpDemoController.java` | 평범한 `client.send(req, ...)` — 호출부에서 wrap은 보이지 않음 |

## Spring 없이 사용

위 wiring은 Spring 의존적이지 않습니다 — 정책 클래스 (`HostPolicy`, `UrlPolicy`, `SsrfGuardedHttpClient`)는 POJO:

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

// java.net.http.HttpClient처럼 그대로 사용
HttpResponse<String> resp = safe.send(
    HttpRequest.newBuilder(URI.create("https://api.partner.com/")).build(),
    HttpResponse.BodyHandlers.ofString());
```

Lambda, AWS SDK 소비자, Quarkus 앱, CLI 도구 — `java.net.http`를 Spring 없이 쓰는 모든 환경에 유용.

## 빌드 검증

```bash
./gradlew build
```
