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
    // Spring Boot is here only to give us a REST endpoint we can curl.
    // The library this demo showcases — ssrf-guard-jdkhttp — has NO Spring
    // dependency itself; the Spring Boot framing is just the demo's UX.
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("kr.devslab:ssrf-guard-jdkhttp:3.1.1")
    // ssrf-guard-core's @ConfigurationProperties pulls in spring-boot
    // (transitively from -jdkhttp's API), so we get the SsrfGuardProperties
    // binding for free.

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
