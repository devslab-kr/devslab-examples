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

// Spring Cloud 2024.0.x targets Spring Boot 3.4.x.
// 2025.0.x targets Spring Boot 3.5.x — match the Boot version above.
extra["springCloudVersion"] = "2025.0.2"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // The library this demo showcases. Pulls in ssrf-guard-core transitively.
    implementation("kr.devslab:ssrf-guard-feign:3.1.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
