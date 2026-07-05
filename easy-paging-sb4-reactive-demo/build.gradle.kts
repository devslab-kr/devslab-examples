plugins {
    java
    id("org.springframework.boot") version "4.1.0"
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
    // WebFlux instead of MVC — the whole point of this demo over the postgres one.
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // R2DBC instead of JDBC. Spring Boot wires up R2dbcEntityTemplate and the
    // R2DBC ConnectionFactory automatically when this is on the classpath.
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    // The reactive companion artifact — provides R2dbcOffsetPagingSupport,
    // R2dbcKeysetSupport, and ReactiveKeysetRequestArgumentResolver. Pulls in
    // the core starter (kr.devslab:easy-paging-spring-boot-starter) transitively
    // via `api(project(":core"))` so PageResponse/Pageable/etc. are available
    // without an explicit dependency on it.
    implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:4.0.0")

    // Real PostgreSQL R2DBC driver.
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Testcontainers. spring-boot-testcontainers provides @ServiceConnection,
    // which auto-rewires r2dbc:postgresql:... to the container at test time.
    // testcontainers:r2dbc is the bridge that exposes R2dbcConnectionDetails.
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-r2dbc")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
