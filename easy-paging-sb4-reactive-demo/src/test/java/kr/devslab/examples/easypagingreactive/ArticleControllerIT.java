package kr.devslab.examples.easypagingreactive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test against a real PostgreSQL R2DBC container. Mirrors the
 * postgres demo's {@code ProductControllerIT} but uses {@link WebTestClient}
 * (WebFlux) instead of MockMvc (servlet), and runs against R2DBC instead
 * of JDBC.
 *
 * <p>{@link ServiceConnection} on a {@link PostgreSQLContainer} produces an
 * {@code R2dbcConnectionDetails} automatically when both
 * {@code spring-boot-starter-data-r2dbc} and {@code testcontainers:r2dbc}
 * are on the classpath — no JDBC URL rewriting, no manual
 * {@code @DynamicPropertySource}, no JDBC fallback.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class ArticleControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void firstPageReturnsExpectedPaginationMetadata() {
        // 500 seeded rows / size=10 = 50 pages
        webTestClient.get().uri("/articles?page=0&size=10")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
                .jsonPath("$.content.length()").isEqualTo(10)
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(10)
                .jsonPath("$.totalElements").isEqualTo(500)
                .jsonPath("$.totalPages").isEqualTo(50)
                .jsonPath("$.first").isEqualTo(true)
                .jsonPath("$.last").isEqualTo(false);
    }

    @Test
    void authorFilterReducesTotalElements() {
        // data.sql distributes 500 rows across 5 authors → 100 each.
        webTestClient.get().uri("/articles?author=alice&page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
                .jsonPath("$.totalElements").isEqualTo(100)
                .jsonPath("$.totalPages").isEqualTo(5)
                .jsonPath("$.content.length()").isEqualTo(20);
    }

    @Test
    void sortPushesNewestFirstAndContentMatchesId() {
        // With size=1 and sort=id,asc the very first article is id=1.
        webTestClient.get().uri("/articles?page=0&size=1&sort=id,asc")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
                .jsonPath("$.content[0].id").isEqualTo(1)
                .jsonPath("$.content[0].title").isEqualTo("Article #1");
    }

    @Test
    void lastPageReportsLastFlag() {
        // 500 rows, size=100 → pages 0..4. page=4 is the last.
        webTestClient.get().uri("/articles?page=4&size=100")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
                .jsonPath("$.content.length()").isEqualTo(100)
                .jsonPath("$.first").isEqualTo(false)
                .jsonPath("$.last").isEqualTo(true);
    }
}
