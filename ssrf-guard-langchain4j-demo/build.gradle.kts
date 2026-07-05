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
    // - ssrf-guard core: needed so the demo can wire a real UrlPolicy bean
    //   from `ssrf.guard.*` properties.
    // - ssrf-guard-langchain4j: registers a BeanPostProcessor that wraps every
    //   ToolExecutor bean automatically — the "secure-by-default" pitch
    //   mirrored from ssrf-guard-springai, just for the LangChain4j community.
    implementation("kr.devslab:ssrf-guard:3.1.1")
    implementation("kr.devslab:ssrf-guard-langchain4j:3.1.1")

    // LangChain4j 1.x. We don't actually call an LLM in this demo — the
    // FakeLlmService stands in for one — but we pull the API in so the
    // ToolExecutor / ToolExecutionRequest types compile.
    implementation("dev.langchain4j:langchain4j:1.15.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
