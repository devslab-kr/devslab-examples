package kr.devslab.examples.easypagingpostgres;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test against a real PostgreSQL container — not H2. The whole
 * point of this demo is to prove the starter works end-to-end on the database
 * a production deployment would actually use, so the test does too.
 *
 * <p>{@link ServiceConnection} (Spring Boot 3.1+) auto-rewires the
 * application's datasource to the container's random port, so no
 * {@code application-test.yml} or {@code DynamicPropertySource} boilerplate is
 * needed — the {@code spring.datasource.url} in {@code application.yml} is
 * silently overridden at test bootstrap.
 *
 * <p>CI runs this exactly the same way as a developer laptop would: the
 * GitHub-hosted Ubuntu runner already has Docker, and Testcontainers pulls
 * {@code postgres:16-alpine} on demand. No \"is Postgres installed?\"
 * branching anywhere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void firstPageReturnsExpectedPaginationMetadata() throws Exception {
        // 500 seeded rows / size=10 = 50 pages
        mockMvc.perform(get("/products").param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(10))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(500))
            .andExpect(jsonPath("$.totalPages").value(50))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void categoryFilterReducesTotalElements() throws Exception {
        // data.sql distributes 500 rows evenly across 5 categories → 100 each.
        mockMvc.perform(get("/products")
                .param("category", "books")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(100))
            .andExpect(jsonPath("$.totalPages").value(5))
            .andExpect(jsonPath("$.content.length()").value(20));
    }

    @Test
    void sortIsValidatedAgainstSqlInjection() throws Exception {
        // The aspect rejects sort properties that don't match
        // [A-Za-z_][A-Za-z0-9_.]* before they ever reach the DB.
        mockMvc.perform(get("/products").param("sort", "name;DROP TABLE products"))
            .andExpect(status().isBadRequest());

        // And the table is, of course, still there.
        mockMvc.perform(get("/products").param("page", "0").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(500));
    }

    @Test
    void oversizedPageSizeIsClampedByMaxSize() throws Exception {
        // @AutoPaginate(maxSize = 100) on the controller
        mockMvc.perform(get("/products").param("page", "0").param("size", "9999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(100))
            .andExpect(jsonPath("$.content.length()").value(100));
    }
}
