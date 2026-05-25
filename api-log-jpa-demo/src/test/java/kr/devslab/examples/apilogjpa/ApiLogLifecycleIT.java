package kr.devslab.examples.apilogjpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import kr.devslab.apilog.jpa.model.ApiLogEntity;
import kr.devslab.apilog.jpa.repository.ApiLogRepository;
import kr.devslab.examples.apilogjpa.widget.Widget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end test for the api-log JPA backend. Boots the full Spring app on a
 * random port (so the self-loopback {@code ClientController} -> {@code UpstreamController}
 * call actually goes over HTTP and triggers the api-log event publisher), then
 * verifies that {@code api_log} rows show up with the expected lifecycle.
 *
 * <p>The api-log listener is {@code @Async}, so each assertion uses Awaitility
 * to poll for the expected row rather than {@code Thread.sleep}.
 *
 * <p>{@code @ServiceConnection} auto-rewires {@code spring.datasource.url} to
 * the Testcontainers Postgres — no {@code application-test.yml} or
 * {@code @DynamicPropertySource} needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiLogLifecycleIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int port;

    @Autowired
    ApiLogRepository repo;

    private RestClient http;

    @BeforeEach
    void setUp() {
        // Fresh client per test to keep base-url scoped to the random port the
        // server picked for this run. Truncate the audit table between tests so
        // each test's assertions only see rows it generated itself.
        http = RestClient.create("http://localhost:" + port);
        repo.deleteAll();
    }

    @Test
    void happyGetPath_writesInitiatedThenSuccessRows() {
        Widget body = http.get().uri("/client/widgets/123").retrieve().body(Widget.class);

        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(123L);
        assertThat(body.name()).isEqualTo("Widget-123");

        // The async writer hasn't necessarily flushed by the time the HTTP
        // response returns, so poll until both rows show up.
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            List<ApiLogEntity> rows = repo.findAll();
            assertThat(rows).hasSizeGreaterThanOrEqualTo(2);

            // Both rows for the same call share the same requestId.
            String requestId = rows.stream()
                    .filter(r -> "INITIATED".equals(r.getEventType()))
                    .map(ApiLogEntity::getRequestId)
                    .findFirst()
                    .orElseThrow();
            List<ApiLogEntity> sameCall = repo.findByRequestId(requestId);
            assertThat(sameCall).extracting(ApiLogEntity::getEventType)
                    .containsExactlyInAnyOrder("INITIATED", "SUCCESS");
            ApiLogEntity success = sameCall.stream()
                    .filter(r -> "SUCCESS".equals(r.getEventType()))
                    .findFirst()
                    .orElseThrow();
            assertThat(success.getStatusCode()).isEqualTo(200);
            assertThat(success.getResponse()).isNotNull();
        });
    }

    @Test
    void errorPath_writesInitiatedThenErrorRow() {
        // GET /upstream/widgets/999 throws ResponseStatusException(INTERNAL_SERVER_ERROR);
        // RestClient (Spring) propagates that as HttpServerErrorException.
        try {
            http.get().uri("/client/widgets/999").retrieve().body(Widget.class);
        } catch (HttpServerErrorException expected) {
            // expected — the upstream returns 500, the client wrapper propagates it
        }

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            List<ApiLogEntity> rows = repo.findAll();
            assertThat(rows).extracting(ApiLogEntity::getEventType)
                    .contains("INITIATED", "ERROR");
        });
    }

    @Test
    void postBodyIsPreservedInPayloadColumn() {
        Widget input = new Widget(null, "Gizmo", "SKU-Gizmo", new BigDecimal("19.99"));
        Widget echoed = http.post().uri("/client/widgets").body(input).retrieve().body(Widget.class);

        assertThat(echoed).isNotNull();
        assertThat(echoed.name()).isEqualTo("Gizmo");

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            List<ApiLogEntity> postRows = repo.findByEventType("INITIATED").stream()
                    .filter(r -> r.getEndpoint().endsWith("/upstream/widgets"))
                    .toList();
            assertThat(postRows).isNotEmpty();
            ApiLogEntity row = postRows.get(0);
            assertThat(row.getPayload()).isNotNull();
            // payload is a JsonNode — assert the fields the controller serialised
            assertThat(row.getPayload().toString()).contains("Gizmo").contains("SKU-Gizmo");
        });
    }

    @Test
    void explicitRequestId_correlatesAllRowsForThatCall() {
        ResponseEntity<String> response = http.post()
                .uri("/client/widgets/with-request-id/42")
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            List<ApiLogEntity> rows = repo.findByRequestId("demo-fixed-rid");
            // Should at minimum have an INITIATED + SUCCESS, both keyed off the fixed id.
            assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
            assertThat(rows).extracting(ApiLogEntity::getRequestId)
                    .allMatch("demo-fixed-rid"::equals);
        });
    }

    @Test
    void schemaInitializedOnBoot_repositoryIsWiredAndTableExists() {
        // The starter's BUILTIN schema initializer should have created api_log
        // before this test runs; count() succeeding without an SQL exception
        // proves the table is there and the JPA wiring resolved correctly.
        assertThat(repo.count()).isGreaterThanOrEqualTo(0);
    }
}
