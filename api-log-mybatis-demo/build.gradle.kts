plugins {
    java
    id("org.springframework.boot") version "3.5.6"
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
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.4")

    // The library this demo showcases - MyBatis backend.
    implementation("kr.devslab:api-log-core:3.0.1")
    implementation("kr.devslab:api-log-mybatis:3.0.1")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Awaitility - api-log writes happen on a separate event-listener thread,
    // so the test polls /api-log/recent until the expected rows show up rather
    // than sleeping with a fixed timeout.
    testImplementation("org.awaitility:awaitility:4.2.2")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
