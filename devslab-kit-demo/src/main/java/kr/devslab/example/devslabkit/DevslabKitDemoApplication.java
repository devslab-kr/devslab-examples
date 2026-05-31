package kr.devslab.example.devslabkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A minimal consumer of {@code devslab-kit-spring-boot-starter}.
 *
 * <p>There is no platform code here — authentication, authorization,
 * multi-tenancy, dynamic menus, audit logging, the first-admin bootstrap, and
 * the admin REST API all come from the starter's auto-configuration. This class
 * exists only to boot the application.
 */
@SpringBootApplication
public class DevslabKitDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevslabKitDemoApplication.class, args);
    }
}
