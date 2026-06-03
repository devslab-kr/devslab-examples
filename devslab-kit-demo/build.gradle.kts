plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "kr.devslab.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    // For local testing before 0.1.0 is on Maven Central:
    //   (1) in the devslab-kit repo run `./gradlew publishToMavenLocal`
    //   (2) uncomment the line below
    // mavenLocal()
}

dependencies {
    // The devslab-kit starter pulls in the whole platform's auto-configuration —
    // including Swagger UI (springdoc is bundled from 0.2.1), so /swagger-ui and
    // /v3/api-docs come up with no extra dependency.
    implementation("kr.devslab:devslab-kit-spring-boot-starter:0.4.0")

    // devslab-kit is deliberately unopinionated about which Spring starters you
    // bring — a consumer wires the runtime it actually wants. This is the set the
    // platform needs to come fully alive: web + security (admin REST API), JPA +
    // Flyway (the kit ships its schema migrations on the classpath), and
    // data-redis (only used when devslab.kit.cache.type=redis).
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    // Auto-starts compose.yaml (Postgres + Redis) on `bootRun`.
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Spring Boot 4 manages Testcontainers 2.x, whose artifacts were renamed to
    // the `testcontainers-*` prefix (`testcontainers-junit-jupiter`,
    // `testcontainers-postgresql`); the old 1.x IDs (`junit-jupiter`, `postgresql`)
    // are absent from the SB4 BOM, so leaving them unversioned fails to resolve.
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
