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
    // Spring Boot is here only for the REST endpoint that wraps the demo.
    // The ssrf-guard-httpclient5 module ships its own Spring autoconfig —
    // that's what wires the SafeDnsResolver + SafeRedirectStrategy onto a
    // CloseableHttpClient bean. With this dependency on the classpath plus
    // Spring Boot's autoconfig scanner, no wiring code is required in the
    // demo's main(). Drop autoconfig (e.g. add @ImportAutoConfiguration
    // exclusions) and the wiring still works through SafeDnsResolver
    // constructed by hand — see the README's "Without Spring" section.
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("kr.devslab:ssrf-guard-httpclient5:3.1.0")
    // The Apache HttpClient 5 runtime. Versions 5.3+ ship the
    // DnsResolver / RedirectStrategy interfaces the module plugs into.
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
