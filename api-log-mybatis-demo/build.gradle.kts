plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "kr.devslab.examples"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1")

    // The library this demo showcases - MyBatis backend.
    implementation("kr.devslab:api-log-core:3.0.0")
    implementation("kr.devslab:api-log-mybatis:3.0.0")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    // Spring Boot 4 split TestRestTemplate into its own module.
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Awaitility - api-log writes happen on a separate event-listener thread,
    // so the test polls /api-log/recent until the expected rows show up rather
    // than sleeping with a fixed timeout.
    testImplementation("org.awaitility:awaitility:4.2.2")

    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
