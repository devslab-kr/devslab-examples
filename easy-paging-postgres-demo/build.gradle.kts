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

    // The library this demo showcases. Bumped by Dependabot on new releases.
    implementation("kr.devslab:easy-paging-spring-boot-starter:3.0.0")

    // Real PostgreSQL JDBC driver — the whole point of this demo over the H2 one
    // is "does the starter actually work against a production DB?". (Spoiler: yes,
    // because PageHelper handles the dialect; the demo proves it.)
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Testcontainers — tests spin up a real PostgreSQL container instead of
    // relying on a developer/CI-side install. @ServiceConnection (added in
    // Spring Boot 3.1) auto-rewires the datasource to the container, so no
    // application-test.yml shenanigans needed.
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
