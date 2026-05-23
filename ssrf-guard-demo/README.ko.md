# ssrf-guard-demo

[English](README.md) · **한국어**

[`ssrf-guard`](https://github.com/devslab-kr/ssrf-guard) — JVM용 SSRF(Server-Side Request Forgery) 방어 라이브러리의 실행 가능한 예제.

하나의 Spring Boot 앱에 **3종 Spring HTTP 클라이언트**가 모두 동일 `UrlPolicy`를 통해 wiring됨:

- `RestClient` (Spring 6.1+) — 메타 아티팩트 `kr.devslab:ssrf-guard:3.1.0`
- `RestTemplate` — `kr.devslab:ssrf-guard-resttemplate:3.1.0`
- `WebClient` (WebFlux) — `kr.devslab:ssrf-guard-webclient:3.1.0`

추가로 `/attacks` 엔드포인트는 가드가 차단하는 모든 SSRF 우회 패턴 목록을 각 모듈별 curl 예제와 함께 제공합니다.

## 전제조건

- JDK 21+
- 인터넷 접근 가능한 화이트리스트 호스트 (`httpbin.org` 기본 — 허용 경로가 실제로 네트워크에 닿는 것을 보여주기 위함)

## 실행

```bash
cd ssrf-guard-demo
./gradlew bootRun
```

앱은 `http://localhost:8080`에 뜸.

## 시험해보기

### 허용 — RestClient가 실제 httpbin.org에 도달

```bash
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq
```

```json
{
  "status": "allowed",
  "client": "RestClient",
  "url": "https://httpbin.org/get",
  "bodyPreview": "{\n  \"args\": {}, \n  \"headers\": { ... }, \n  ..."
}
```

### 차단 — AWS 메타데이터 탈취 시도 (canonical SSRF→cloud-takeover)

```bash
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/latest/meta-data/iam/security-credentials/' | jq
```

```json
{
  "status": "blocked",
  "client": "RestClient",
  "url": "http://169.254.169.254/latest/meta-data/iam/security-credentials/",
  "reason": "blocked_ip_literal",
  "message": "IP-literal host blocked (rejectIpLiteralHosts=true): 169.254.169.254"
}
```

### 차단 — 화이트리스트 밖의 호스트

```bash
curl 'http://localhost:8080/fetch?url=https://evil.com/' | jq
# reason: "blocked_host"
```

### 차단 — 십진수로 인코딩된 loopback (`2130706433` == `127.0.0.1`)

```bash
curl 'http://localhost:8080/fetch?url=http://2130706433/' | jq
# reason: "blocked_ip_literal"
```

### 차단 — userinfo (`user:pass@host`)

```bash
curl 'http://localhost:8080/fetch?url=https://user:pass@httpbin.org/get' | jq
# reason: "blocked_userinfo"
```

### 차단 — AWS 메타데이터로의 redirect (4번째 방어 레이어)

```bash
curl 'http://localhost:8080/fetch?url=https://httpbin.org/redirect-to?url=http://169.254.169.254/' | jq
# 2번째 hop에서 잡힘 — 초기 호스트는 화이트리스트지만 redirect 전략이 매 URL 변경을 재검증함
```

### 같은 공격, 다른 HTTP 클라이언트, 같은 결과

```bash
curl 'http://localhost:8080/fetch-resttemplate?url=http://169.254.169.254/'
curl 'http://localhost:8080/fetch-webclient?url=http://169.254.169.254/'
```

동일한 `reason` 필드 — 세 HTTP 클라이언트 모두 같은 `UrlPolicy` 빈으로 래핑됨.

### 전체 공격 매트릭스 (15개)

```bash
curl http://localhost:8080/attacks | jq
```

각 공격 패턴마다 `expectedReason`과 미리 만들어진 `tryRestClient` / `tryRestTemplate` / `tryWebClient` curl 문자열을 반환. 하나를 골라 `bash`로 파이프:

```bash
curl -s http://localhost:8080/attacks \
  | jq -r '.attacks[] | select(.name == "aws-metadata-credentials") | .tryRestClient' \
  | bash | jq
```

### 관찰성 — Micrometer 메트릭

```bash
# 위 curl 몇 개 실행 후:
curl -s http://localhost:8080/actuator/metrics/ssrf_guard_blocked_total | jq
curl -s http://localhost:8080/actuator/prometheus | grep ssrf_guard
```

`reason` 태그별 카운터 (`blocked_host`, `blocked_ip_literal`, `blocked_private_ip`, `blocked_userinfo`, `blocked_redirect`, ...) + 허용된 요청에 대한 별도 `ssrf_guard_allowed_total`을 볼 수 있음.

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | 표준 스타터 외 의존성은 `kr.devslab:ssrf-guard:3.1.0`, `:ssrf-guard-resttemplate:3.0.1`, `:ssrf-guard-webclient:3.0.1` 셋뿐 — 별도 configuration 클래스 불필요 |
| `application.yml` | 모든 `ssrf.guard.*` 옵션이 한 곳에 주석과 함께 |
| `web/FetchController.java` | RestClient 전체 — 3줄 setup, 가드는 보이지 않게 실행 |
| `web/FetchResttemplateController.java` | RestTemplate 동일 — 레거시 코드 마이그레이션 불필요 |
| `web/FetchWebClientController.java` | Reactive 버전; `SsrfGuardException`이 `Mono.onErrorResume`을 통해 흐르는 방식 시연 |
| `web/AttackDemoController.java` | 각 공격 패턴 + 예상 `BlockReason` 카탈로그 |

## 화이트리스트 풀기 (sanity check)

가드가 실제로 뭐 하는지 확인하고 싶다면? `application.yml` 수정:

```yaml
ssrf:
  guard:
    enabled: false
```

재시작 후 AWS 메타데이터 curl 다시 — 이제 실제로 `169.254.169.254`에 시도함 (대부분 네트워크에서 타임아웃 나지만, 가드가 더 이상 개입 안 함).

## 빌드 검증

```bash
./gradlew build
```

스모크 테스트 `SsrfGuardDemoApplicationTests`가:

1. 화이트리스트 URL이 통과 (`status=allowed`)
2. 공격 URL이 올바른 `reason` 태그로 차단됨
3. 차단 후 actuator 메트릭 엔드포인트가 `ssrf_guard_blocked_total`을 노출

## 더 읽기

- ssrf-guard 도큐사이트: <https://ssrf-guard.devslab.kr/>
- ssrf-guard repo: <https://github.com/devslab-kr/ssrf-guard>
- Java SSRF 학습 (이 데모의 공격 패턴 출처): <https://github.com/JoyChou93/java-sec-code>
- OWASP SSRF Top 10: <https://owasp.org/Top10/A10_2021-Server-Side_Request_Forgery_%28SSRF%29/>
