package kr.devslab.examples.apilogmybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import kr.devslab.apilog.mybatis.model.ApiLogRow;
import kr.devslab.examples.apilogmybatis.widget.ApiLogQueryMapper;
import kr.devslab.examples.apilogmybatis.widget.Widget;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end lifecycle test against a real PostgreSQL container.
 *
 * <p>The whole point is to prove the event pipeline lands rows in api_log
 * the way the README claims:
 * <ul>
 *   <li>RestApiClientUtil publishes events around every call</li>
 *   <li>The starter's listener consumes them on a separate thread</li>
 *   <li>MybatisApiLogWriter persists them via the auto-wired ApiLogMapper</li>
 *   <li>BUILTIN schema management creates the api_log table at startup</li>
 * </ul>
 *
 * <p>Because the listener runs on a different thread than the HTTP call,
 * tests use Awaitility instead of fixed sleeps to poll until the expected
 * rows appear.
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
    TestRestTemplate rest;

    @Autowired
    ApiLogQueryMapper queryMapper;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void happyGetProducesInitiatedAndSuccessRows() {
        ResponseEntity<Widget> response = rest.getForEntity(url("/client/widgets/123"), Widget.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(123L);

        // The listener is async - wait for both lifecycle rows to land.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ApiLogRow> recent = recent();

            // Find a request id that has both INITIATED and SUCCESS rows.
            boolean foundPair = recent.stream()
                    .map(ApiLogRow::getRequestId)
                    .distinct()
                    .anyMatch(rid -> {
                        List<ApiLogRow> rowsForRid = recent.stream()
                                .filter(r -> rid.equals(r.getRequestId()))
                                .toList();
                        boolean hasInitiated = rowsForRid.stream()
                                .anyMatch(r -> "INITIATED".equals(r.getEventType()));
                        boolean hasSuccess = rowsForRid.stream()
                                .anyMatch(r -> "SUCCESS".equals(r.getEventType()));
                        return hasInitiated && hasSuccess;
                    });
            assertThat(foundPair).isTrue();
        });
    }

    @Test
    void errorPathProducesErrorRow() {
        ResponseEntity<String> response = rest.getForEntity(url("/client/widgets/999"), String.class);
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ApiLogRow> errors = byEvent("ERROR");
            assertThat(errors).isNotEmpty();
            assertThat(errors).anySatisfy(row ->
                    assertThat(row.getEndpoint()).contains("/upstream/widgets/999"));
        });
    }

    @Test
    void postBodyIsPreservedInPayloadColumn() {
        Widget body = new Widget(null, "Sprocket-7", "SKU-7", new BigDecimal("19.99"));
        ResponseEntity<Widget> response = rest.postForEntity(url("/client/widgets"), body, Widget.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ApiLogRow> successes = byEvent("SUCCESS");
            assertThat(successes).anySatisfy(row -> {
                assertThat(row.getPayload()).isNotNull();
                assertThat(row.getPayload()).contains("Sprocket-7");
            });
        });
    }

    @Test
    void explicitRequestIdCorrelatesEntries() {
        ResponseEntity<String> response = rest.postForEntity(
                url("/client/widgets/with-request-id/123"), null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<List<ApiLogRow>> entries = rest.exchange(
                    url("/api-log/by-request/demo-fixed-rid"),
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ApiLogRow>>() {});
            assertThat(entries.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entries.getBody()).isNotNull();
            assertThat(entries.getBody().size()).isGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void schemaInitializerCreatedTheTable() {
        // If BUILTIN schema management didn't run, this query would throw with a
        // "relation api_log does not exist" SQLException - the assertion is just
        // "the query completes and returns a list".
        List<ApiLogRow> rows = queryMapper.findRecent(1);
        assertThat(rows).isNotNull();
    }

    // ----- helpers wrapping the demo's own /api-log endpoints -----

    private List<ApiLogRow> recent() {
        ResponseEntity<List<ApiLogRow>> response = rest.exchange(
                url("/api-log/recent"),
                org.springframework.http.HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ApiLogRow>>() {});
        return response.getBody();
    }

    private List<ApiLogRow> byEvent(String eventType) {
        ResponseEntity<List<ApiLogRow>> response = rest.exchange(
                url("/api-log/by-event/" + eventType),
                org.springframework.http.HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ApiLogRow>>() {});
        return response.getBody();
    }
}
