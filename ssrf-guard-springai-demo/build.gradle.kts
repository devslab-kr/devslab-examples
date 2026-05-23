plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
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

    // The libraries this demo showcases.
    // - ssrf-guard core/restclient: needed by the FakeLlmService since it calls
    //   the RestClient against the wrapped tool's "remote" target.
    // - ssrf-guard-springai: wraps every ToolCallback bean automatically via
    //   a BeanPostProcessor — that's the whole "secure-by-default" pitch.
    implementation("kr.devslab:ssrf-guard:3.0.1")
    implementation("kr.devslab:ssrf-guard-springai:3.0.1")

    // Spring AI 1.0 GA. We don't actually call an LLM in this demo — the
    // FakeLlmService stands in for one — but we pull the API in so the
    // ToolCallback / ToolDefinition / ToolMetadata types compile.
    implementation("org.springframework.ai:spring-ai-model:1.0.7")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
