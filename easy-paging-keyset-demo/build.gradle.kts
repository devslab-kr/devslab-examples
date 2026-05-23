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
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.4.0")

    // H2 in-memory database — keeps the demo self-contained (no external DB).
    // Keyset pagination doesn't need a "real" DB to demonstrate; the SQL pattern
    // is portable to PostgreSQL/MySQL/etc. unchanged.
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
