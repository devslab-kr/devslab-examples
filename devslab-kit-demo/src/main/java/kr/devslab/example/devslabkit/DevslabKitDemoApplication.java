package kr.devslab.example.devslabkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A minimal consumer of {@code devslab-kit-spring-boot-starter}.
 *
 * <p>This is the whole point of the demo: a <strong>plain</strong>
 * {@code @SpringBootApplication} in the app's own package
 * ({@code kr.devslab.example.devslabkit}, not under {@code kr.devslab.kit}) — no
 * {@code scanBasePackages}, no {@code @EntityScan}, no {@code @EnableJpaRepositories}.
 * The starter's auto-configuration registers the platform's services, JPA entities
 * and repositories, the admin REST API, and the first-admin bootstrap on its own,
 * broadening scanning rather than requiring the consumer to widen it.
 */
@SpringBootApplication
public class DevslabKitDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevslabKitDemoApplication.class, args);
    }
}
