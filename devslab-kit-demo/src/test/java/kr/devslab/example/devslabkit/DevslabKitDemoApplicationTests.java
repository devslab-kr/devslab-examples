package kr.devslab.example.devslabkit;

import static org.assertj.core.api.Assertions.assertThat;

import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.audit.AuditEventPublisher;
import kr.devslab.kit.identity.PasswordHasher;
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.tenant.TenantResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Proves the starter wires the whole platform into a plain consumer app: the
 * context boots against real Postgres + Redis (Testcontainers) and the key
 * platform beans are present and usable.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DevslabKitDemoApplicationTests {

    @Autowired
    private TenantResolver tenantResolver;

    @Autowired
    private PermissionChecker permissionChecker;

    @Autowired
    private MenuProvider menuProvider;

    @Autowired
    private AuditEventPublisher auditEventPublisher;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void platformBeansAreAutoConfigured() {
        assertThat(tenantResolver).isNotNull();
        assertThat(permissionChecker).isNotNull();
        assertThat(menuProvider).isNotNull();
        assertThat(auditEventPublisher).isNotNull();
        assertThat(passwordHasher).isNotNull();
    }

    @Test
    void singleTenantResolvesTheDefaultTenant() {
        assertThat(tenantResolver.resolve().tenantId().value()).isEqualTo("default");
    }

    @Test
    void bcryptPasswordHasherRoundTrips() {
        String hash = passwordHasher.hash("hunter2");
        assertThat(passwordHasher.matches("hunter2", hash)).isTrue();
        assertThat(passwordHasher.matches("nope", hash)).isFalse();
    }
}
