# ssrf-guard-native-image-demo

[English](README.md) · **한국어**

[`ssrf-guard 3.1.1`](https://github.com/devslab-kr/ssrf-guard)의 **GraalVM 네이티브 이미지 힌트**가 동작함을 end-to-end로 증명 — Spring Boot 앱에 라이브러리 drop, `./gradlew nativeCompile`, 나온 네이티브 바이너리가 JVM 빌드와 동일한 게이트에서 SSRF 시도를 차단.

## 이 데모가 증명하는 것

ssrf-guard 3.1.1은 각 모듈에서 `META-INF/spring/aot.factories`로 `RuntimeHintsRegistrar` 엔트리를 발행합니다. 커버 범위:

| 타입 | 힌트가 필요한 이유 |
| --- | --- |
| `UrlPolicy`, `HostPolicy` | Spring Boot AOT가 `@ConfigurationProperties` 바인딩을 리플렉션으로 인스턴스화 |
| `SsrfBlockPayload` (record) | LLM-tool wrap이 URL 거부할 때 Jackson 직렬화 wire에 등장 |
| `BlockReason` (enum) | 동일 — JSON 에러 페이로드에 포함 |
| `JsonToolInputGuard` | Tool-input JSON 트리를 Jackson 리플렉션으로 walk |
| `MicrometerSsrfGuardMetrics` | 조건부 빈 — `MeterRegistry` 리플렉션 lookup |

힌트 없으면 네이티브 바이너리가 `ssrf.guard.*` 바인딩 / `SsrfBlockPayload` 직렬화 첫 시도에서 `MissingReflectionRegistrationError`로 죽음. 힌트 있으면 그냥 동작.

## 사전 요구사항

| 도구 | 버전 | 이유 |
| --- | --- | --- |
| **GraalVM** | 21+, `native-image` 설치된 것 | `./gradlew nativeCompile` 실행에 필수 |
| **Docker** | (옵션) any | OCI 이미지 만들고 싶으면 `./gradlew bootBuildImage` |
| **메모리** | 약 8 GB free | 네이티브 이미지 빌드 메모리 많이 씀 |
| **디스크** | 약 3 GB | 빌드 캐시 + 출력 바이너리 (~80 MB) |

`GRAALVM_HOME` (또는 JDK가 GraalVM 디스트리이면 `JAVA_HOME`) 설정. `native-image --version`로 확인.

## 이 데모만 받기

각 데모는 독립 Gradle 프로젝트라, `devslab-examples` 저장소 전체를 clone하지 않고
이 폴더만 받을 수 있습니다.

**git 사용 (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set ssrf-guard-native-image-demo
cd ssrf-guard-native-image-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/ssrf-guard-native-image-demo
cd ssrf-guard-native-image-demo
```

## 일단 JVM에서 실행

```bash
cd ssrf-guard-native-image-demo
./gradlew bootRun
```

다른 터미널에서:

```bash
# 허용 (화이트리스트 호스트):
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq

# 차단 (AWS 메타데이터 IP 리터럴):
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq

# 12개 공격 패턴 카탈로그:
curl http://localhost:8080/attacks | jq
```

응답에 `"runtime": "jvm"`이 보여서 어느 빌드를 hit 했는지 알 수 있음.

## 네이티브 이미지 빌드

```bash
./gradlew nativeCompile
```

하드웨어에 따라 3–8분. 출력: `build/native/nativeCompile/ssrf-guard-native-image-demo` (Linux/macOS) 또는 `…\ssrf-guard-native-image-demo.exe` (Windows).

OCI 이미지를 대신 만들고 싶으면 `./gradlew bootBuildImage`.

## 네이티브 바이너리 실행

```bash
./build/native/nativeCompile/ssrf-guard-native-image-demo
```

동일한 엔드포인트, 동일한 동작 — 단 `"runtime": "graalvm-native"`이고 startup이 ~1.5초 대신 ~50ms.

```bash
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq
```

```json
{
  "runtime": "graalvm-native",
  "url": "http://169.254.169.254/",
  "status": "blocked",
  "reason": "blocked_ip_literal",
  "message": "IP-literal host blocked..."
}
```

대신 `MissingReflectionRegistrationError`나 `status: error`가 나오면 ssrf-guard 측 힌트 누락 — 스택트레이스와 함께 이슈 부탁드립니다.

## 더 빠른 검증 (전체 네이티브 빌드 없이)

`./gradlew nativeCompile`은 몇 분 걸림. 전체 링크 없이 힌트만 검증하려면:

```bash
./gradlew processAot
```

10초 정도. AOT 생성 소스가 `build/generated/aotSources`에 떨어짐. `META-INF/native-image/...`에서 ssrf-guard가 기여한 reflection / proxy / resource 힌트 확인 가능:

```bash
find build/generated -name 'reflect-config.json' -exec head -30 {} \;
```

`UrlPolicy`, `HostPolicy`, `SsrfBlockPayload`, `BlockReason` 등 엔트리가 보일 것.

## 읽을 만한 파일

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | 2줄 셋업: `kr.devslab:ssrf-guard:3.1.1` + `org.graalvm.buildtools.native` plugin |
| `SsrfGuardNativeImageDemoApplication.java` | `@SpringBootApplication` + `RestClient` bean만 — 나머지는 자동 설정 처리 |
| `NativeImageDemoController.java` | `/fetch` + `/attacks` — `runtime` 필드 (jvm vs graalvm-native)로 시각적 확인 |
| `application.yml` | `ssrf.guard.*` 키 — `@ConfigurationProperties` 바인딩 경로가 AOT 힌트가 필요한 그 경로 |

## CI 전략

이 데모의 CI는 **JVM 모드만** 실행 — `./gradlew build`. 네이티브 이미지 빌드는 매 PR마다 돌리기엔 너무 느리고 리소스 많이 씀. JVM 테스트 스위트가 `processAot` 코드젠 경로를 간접 검증.

네이티브 이미지 검증은 로컬에서 수동 — 위 단계대로 하면 10분 미만.

## 왜 중요한가

GraalVM 네이티브 이미지는 JVM cold-start 비용 문제의 답:
- AWS Lambda / Cloud Run / Azure Container Apps
- Kubernetes pod scale-to-zero
- 100 ms 미만 시작 필요한 CLI 도구

라이브러리 측 힌트 없으면 사용자가 끌어오는 모든 의존성이 네이티브 이미지 지뢰. ssrf-guard 3.1.1은 힌트를 ship해서 사용자가 우리 타입에 대한 `reflect-config.json` 안 쓰고도 `nativeCompile` 가능.
