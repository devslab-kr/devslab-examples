# ssrf-guard-feign-demo

[English](README.md) · **한국어**

[`ssrf-guard-feign`](https://github.com/devslab-kr/ssrf-guard) — Spring Cloud OpenFeign 클라이언트용 SSRF 방어 예제.

선언적 `@FeignClient` 인터페이스 2개가 하나의 `UrlPolicy`를 공유:
- `HttpBinClient` — `https://httpbin.org` 가리킴 (화이트리스트) — 호출 성공
- `EvilClient` — `https://evil.com` 가리킴 (화이트리스트 밖) — Feign `RequestInterceptor`에서 차단됨, HTTP 트래픽이 JVM을 떠나지 않음

## 이 데모만 받기

각 데모는 독립 Gradle 프로젝트라, `devslab-examples` 저장소 전체를 clone하지 않고
이 폴더만 받을 수 있습니다.

**git 사용 (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set ssrf-guard-feign-demo
cd ssrf-guard-feign-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/ssrf-guard-feign-demo
cd ssrf-guard-feign-demo
```

## 실행

```bash
cd ssrf-guard-feign-demo
./gradlew bootRun
```

## 시험해보기

```bash
# 화이트리스트 호스트 — 실제 httpbin.org 호출
curl http://localhost:8080/feign/legit | jq

# 화이트리스트 밖 — SSRF 가드 인터셉터에서 차단
curl http://localhost:8080/feign/evil | jq
# → { "status": "blocked", "reason": "blocked_host", ... }
```

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | `kr.devslab:ssrf-guard-feign:3.1.0` + `spring-cloud-starter-openfeign` |
| `HttpBinClient.java` / `EvilClient.java` | 평범한 `@FeignClient` 인터페이스 2개 — 가드 코드 없음 |
| `FeignDemoController.java` | `SsrfGuardException` catch (Feign이 한 단계 wrap — 컨트롤러가 unwrap) |
| `application.yml` | `ssrf.guard.exact-hosts: [httpbin.org]` — 그 한 줄이 화이트리스트 |

Feign 인터셉터는 자동 등록됨 — `ssrf-guard-feign-3.1.0`이 Spring 자동설정으로 `feign.RequestInterceptor` 빈을 publish하고, Spring Cloud OpenFeign이 모든 `@FeignClient`에 적용.

## 빌드 검증

```bash
./gradlew build
```
