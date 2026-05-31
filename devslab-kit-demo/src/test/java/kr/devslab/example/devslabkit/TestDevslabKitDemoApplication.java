package kr.devslab.example.devslabkit;

import org.springframework.boot.SpringApplication;

/**
 * Run the demo locally with Testcontainers-provided Postgres + Redis instead of
 * the compose stack: {@code ./gradlew bootTestRun}.
 */
public class TestDevslabKitDemoApplication {

    public static void main(String[] args) {
        SpringApplication.from(DevslabKitDemoApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
