package kr.devslab.examples.apilogr2dbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.devslab.examples.apilogr2dbc.widget.ApiLogReader;
import kr.devslab.examples.apilogr2dbc.widget.ApiLogView;
import kr.devslab.examples.apilogr2dbc.widget.Widget;

/**
 * End-to-end lifecycle test against a real PostgreSQL container.
 *
 * <p>{@link ServiceConnection} on a {@link PostgreSQLContainer} produces both
 * {@code DataSourceConnectionDetails} (JDBC) and — since
 * {@code spring-boot-starter-data-r2dbc} is on the classpath — an
 * {@code R2dbcConnectionDetails} that rewires {@code spring.r2dbc.url} to the
 * Testcontainers instance. No manual {@code @DynamicPropertySource} needed.
 *
 * <p>api-log writes happen on the {@code ApplicationEventListener} thread, so
 * every assertion that inspects {@code api_log} runs inside an
 * {@link org.awaitility.Awaitility} block — the test polls the read endpoint
 * (or the reader directly) until the expected rows show up rather than
 * sleeping with a fixed timeout.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class ApiLogLifecycleIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ApiLogReader reader;

    @Test
    void happyGetWritesInitiatedAndSuccessRows() {
        webTestClient.get().uri("/client/widgets/123")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Widget.class)
            .value(w -> {
                assertThat(w.id()).isEqualTo(123L);
                assertThat(w.name()).isEqualTo("Widget-123");
            });

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ApiLogView> rows = reader.findRecent(20).collectList().block();
            assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
            assertThat(rows).extracting(ApiLogView::eventType)
                .contains("INITIATED", "SUCCESS");
        });
    }

    @Test
    void errorPathWritesErrorRow() {
        webTestClient.get().uri("/client/widgets/999")
            .exchange()
            .expectStatus().is5xxServerError();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ApiLogView> rows = reader.findByEvent("ERROR").collectList().block();
            assertThat(rows).isNotEmpty();
            assertThat(rows).anySatisfy(r ->
                assertThat(r.endpoint()).contains("/upstream/widgets/999"));
        });
    }

    @Test
    void postBodyIsPreservedInPayload() {
        Widget body = new Widget(null, "Hyperbolic Cog", "SKU-COG-42", null);

        webTestClient.post().uri("/client/widgets")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Widget.class);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ApiLogView> rows = reader.findByEvent("SUCCESS").collectList().block();
            assertThat(rows).anySatisfy(r ->
                assertThat(r.payload()).isNotNull().contains("Hyperbolic Cog"));
        });
    }

    @Test
    void explicitRequestIdCorrelatesInitiatedAndTerminalRows() {
        webTestClient.post().uri("/client/widgets/with-request-id/123")
            .exchange()
            .expectStatus().isOk();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ApiLogView> rows = reader.findByRequestId("demo-fixed-rid").collectList().block();
            assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
            assertThat(rows).extracting(ApiLogView::requestId)
                .allMatch("demo-fixed-rid"::equals);
        });
    }

    @Test
    void schemaIsInitializedAndQueryable() {
        // If the R2DBC schema initializer didn't run, this would throw "relation
        // api_log does not exist". A clean empty Flux is the success signal.
        List<ApiLogView> rows = reader.findRecent(1).collectList().block();
        assertThat(rows).isNotNull();
    }
}
