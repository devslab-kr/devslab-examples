plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    // GraalVM Native Image — provides `nativeCompile` + `nativeRun` tasks,
    // and the `processAot` task that exercises every RuntimeHintsRegistrar
    // contributed by libraries on the classpath. ssrf-guard 3.1.0 ships
    // hints for UrlPolicy / HostPolicy / SsrfBlockPayload / BlockReason
    // + the LLM JSON-walking path + Micrometer reflective bean; this demo
    // is the end-to-end proof those hints actually let a native image
    // build cleanly.
    id("org.graalvm.buildtools.native") version "0.10.5"
}

group = "kr.devslab.examples"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    // The meta artifact — pulls -core + -httpclient5 + -restclient + AOT
    // hints transitively. Everything ssrf-guard contributes to the native
    // image (reflection / proxies / resources) flows through this single
    // dep.
    implementation("kr.devslab:ssrf-guard:3.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            // Verbose during the build itself — the build log is the part
            // that catches missing reflection hints (they show up as
            // "missing-class-error" warnings the AOT processor emits).
            verbose.set(true)
            buildArgs.add("--no-fallback")
            // Enables Spring's "init at run time" defaults for classes the
            // Boot AOT scanner already knows are unsafe to init at build.
        }
    }
}
