package kr.devslab.example.devslabkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * A minimal consumer of {@code devslab-kit-spring-boot-starter}.
 *
 * <p>The platform's services, the admin REST API, and the first-admin bootstrap
 * all come from the starter's auto-configuration. The one thing a consumer must
 * do — because this app lives in its own package ({@code kr.devslab.example.*}),
 * not under {@code kr.devslab.kit} — is widen scanning to include the kit's
 * packages so its JPA {@code @Entity} types and Spring Data repositories are
 * registered:
 *
 * <ul>
 *   <li>{@code scanBasePackages} — component scan over both packages,</li>
 *   <li>{@link AutoConfigurationPackage} — entity scanning (Hibernate reads the
 *       auto-configuration packages), and</li>
 *   <li>{@link EnableJpaRepositories} — the kit's Spring Data repositories.</li>
 * </ul>
 *
 * Without these you get {@code Not a managed type: …PlatformUserAccountEntity}
 * on startup. (A future kit release may auto-register these so a plain
 * {@code @SpringBootApplication} suffices.)
 */
@SpringBootApplication(scanBasePackages = {"kr.devslab.example.devslabkit", "kr.devslab.kit"})
@AutoConfigurationPackage(basePackages = {"kr.devslab.example.devslabkit", "kr.devslab.kit"})
@EnableJpaRepositories(basePackages = {"kr.devslab.example.devslabkit", "kr.devslab.kit"})
public class DevslabKitDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevslabKitDemoApplication.class, args);
    }
}
