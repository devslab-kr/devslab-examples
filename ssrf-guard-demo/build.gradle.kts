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
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // The libraries this demo showcases — one app, three HTTP-client modules.
    // The meta `ssrf-guard` artifact transitively pulls in `-core`, `-httpclient5`,
    // and `-restclient`. The `-resttemplate` and `-webclient` modules are
    // additive and reuse the same UrlPolicy / SsrfGuardMetrics beans.
    implementation("kr.devslab:ssrf-guard:3.0.1")
    implementation("kr.devslab:ssrf-guard-resttemplate:3.0.1")
    implementation("kr.devslab:ssrf-guard-webclient:3.0.1")

    // Micrometer Prometheus registry — turns SSRF Guard's counters into
    // /actuator/prometheus output so you can curl the metrics in the demo.
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
