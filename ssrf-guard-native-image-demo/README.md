# ssrf-guard-native-image-demo

**English** · [한국어](README.ko.md)

End-to-end proof that [`ssrf-guard 3.1.1`](https://github.com/devslab-kr/ssrf-guard)'s **GraalVM native-image hints** work — drop the library into a Spring Boot app, run `./gradlew nativeCompile`, and the resulting native binary blocks SSRF attempts at the same gates the JVM build does.

## What this demo proves

ssrf-guard 3.1.1 ships `RuntimeHintsRegistrar` entries through `META-INF/spring/aot.factories` in each module. The hints cover:

| Type | Why it needs a hint |
| --- | --- |
| `UrlPolicy`, `HostPolicy` | Spring Boot AOT reflectively instantiates `@ConfigurationProperties` binding |
| `SsrfBlockPayload` (record) | Jackson serializes it on the wire when the LLM-tool wrap rejects a URL |
| `BlockReason` (enum) | Same — appears in the JSON error payload |
| `JsonToolInputGuard` | Tree-walks tool-input JSON via Jackson reflection |
| `MicrometerSsrfGuardMetrics` | Conditional bean — `MeterRegistry` reflective lookup |

Without those hints, a native binary would fail at runtime with `MissingReflectionRegistrationError` the first time it tried to bind `ssrf.guard.*` or serialize an `SsrfBlockPayload`. With the hints, it Just Works.

## Prerequisites

| Tool | Version | Why |
| --- | --- | --- |
| **GraalVM** | 21+ with `native-image` installed | `./gradlew nativeCompile` won't run without it |
| **Docker** | (optional) any | If you'd rather use `./gradlew bootBuildImage` to produce an OCI image |
| **Memory** | ~8 GB free | Native image builds are memory-hungry |
| **Disk** | ~3 GB | Build cache + the output binary (~80 MB) |

Set `GRAALVM_HOME` (or just `JAVA_HOME` if your JDK is a GraalVM distribution). Verify with `native-image --version`.

## Get just this demo

Each demo is a standalone Gradle project, so you can grab this one folder without
cloning the whole `devslab-examples` repo.

**With git (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set ssrf-guard-native-image-demo
cd ssrf-guard-native-image-demo
```

**Without git (folder only):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/ssrf-guard-native-image-demo
cd ssrf-guard-native-image-demo
```

## Run it on the JVM first

```bash
cd ssrf-guard-native-image-demo
./gradlew bootRun
```

Then in another terminal:

```bash
# Allowed (host in whitelist):
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq

# Blocked (AWS metadata IP literal):
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/' | jq

# See the 12-pattern attack catalog:
curl http://localhost:8080/attacks | jq
```

Response shows `"runtime": "jvm"` so you know which build you're hitting.

## Build the native image

```bash
./gradlew nativeCompile
```

Takes 3–8 minutes depending on hardware. Output lands at `build/native/nativeCompile/ssrf-guard-native-image-demo` (Linux/macOS) or `…\ssrf-guard-native-image-demo.exe` (Windows).

You can also do `./gradlew bootBuildImage` to produce a container image instead.

## Run the native binary

```bash
./build/native/nativeCompile/ssrf-guard-native-image-demo
```

Same endpoints, same behaviour — but now `"runtime": "graalvm-native"` and startup is ~50 ms instead of ~1.5 seconds.

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

If you instead see `MissingReflectionRegistrationError` or `status: error`, that's a hint gap in ssrf-guard — please file an issue with the stack trace.

## Faster verification (no full native build)

`./gradlew nativeCompile` takes minutes. To verify the hints register correctly without doing the full link:

```bash
./gradlew processAot
```

Runs in ~10 seconds. Produces the AOT-generated source under `build/generated/aotSources`. Inspect `META-INF/native-image/...` to see the reflection / proxy / resource hints ssrf-guard contributed:

```bash
find build/generated -name 'reflect-config.json' -exec head -30 {} \;
```

You should see entries for `UrlPolicy`, `HostPolicy`, `SsrfBlockPayload`, `BlockReason`, etc.

## What to read

| File | Why |
| --- | --- |
| `build.gradle.kts` | The two-line setup: `kr.devslab:ssrf-guard:3.1.1` + `org.graalvm.buildtools.native` plugin |
| `SsrfGuardNativeImageDemoApplication.java` | Just `@SpringBootApplication` + a `RestClient` bean — autoconfig handles the rest |
| `NativeImageDemoController.java` | `/fetch` + `/attacks` — surfaces `runtime` (jvm vs graalvm-native) for visual confirmation |
| `application.yml` | `ssrf.guard.*` keys — bound via `@ConfigurationProperties`, which is the path that needs the AOT hints |

## CI strategy

This demo's CI runs **JVM mode only** — `./gradlew build`. The native-image build is too slow / resource-heavy for every PR. We verify the hints register correctly via the JVM test suite (which uses the same `processAot` codegen path indirectly).

Native-image verification is left to whoever wants to confirm locally — the steps above take under 10 minutes start to finish.

## Why this matters

GraalVM native images are the answer to JVM cold-start cost in:
- AWS Lambda / Cloud Run / Azure Container Apps
- Kubernetes pods that scale to zero
- CLI tools that need to start in <100 ms

Without library-side hints, every dependency the consumer pulls in is a potential native-image landmine. ssrf-guard 3.1.1 ships the hints so consumers can `nativeCompile` without writing `reflect-config.json` for our types.
