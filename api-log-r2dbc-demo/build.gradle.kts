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
    // WebFlux — the demo's HTTP path is fully non-blocking, top to bottom.
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // R2DBC — Spring Boot wires up DatabaseClient + R2dbcEntityTemplate +
    // the reactive ConnectionFactory automatically when this is on the classpath.
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    // The library this demo showcases — R2DBC backend. core publishes the
    // ReactiveApiClientUtil + the ApplicationEvent contract; r2dbc plugs in
    // the reactive ApiLogWriter + the reactive schema initializer.
    // api-log 3.0.0 is built against Spring Boot 3.5.6 (lib major = SB major
    // per VERSIONING.md), so this demo pins the plugin to 3.5.6 to match.
    implementation("kr.devslab:api-log-core:3.0.1")
    implementation("kr.devslab:api-log-r2dbc:3.0.1")

    // R2DBC PostgreSQL driver — for the app's runtime path.
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    // JDBC PostgreSQL driver — Testcontainers' @ServiceConnection wants a JDBC
    // URL for its JdbcDatabaseContainer, even when the app itself uses R2DBC.
    runtimeOnly("org.postgresql:postgresql")

    // spring-boot-starter-test pulls in WebTestClient transitively when
    // spring-boot-starter-webflux is on the classpath — no need for the
    // SB4-only spring-boot-starter-webflux-test split module.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")

    // Awaitility — api-log writes happen on a separate event-listener thread,
    // so the test polls /api-log/recent until the expected rows show up rather
    // than sleeping with a fixed timeout.
    testImplementation("org.awaitility:awaitility:4.2.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Testcontainers. spring-boot-testcontainers provides @ServiceConnection,
    // which auto-rewires r2dbc:postgresql:... to the container at test time.
    // testcontainers:r2dbc is the bridge that exposes R2dbcConnectionDetails.
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:r2dbc")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
