package kr.devslab.examples.easypaging;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Smoke test for the demo. Verifies that:
 *   1. the application context boots (auto-config from easy-paging works),
 *   2. the seeded H2 data is queryable through the @AutoPaginate-annotated endpoint, and
 *   3. the response carries the Spring Data-shaped pagination envelope.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void firstPageReturnsExpectedPaginationMetadata() throws Exception {
        // 137 seeded rows / size=5 = 28 pages (27 full + 1 partial of 2).
        mockMvc.perform(get("/reports").param("page", "0").param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.totalElements").value(137))
            .andExpect(jsonPath("$.totalPages").value(28))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void oversizedPageSizeIsClampedByMaxSize() throws Exception {
        // Controller has @AutoPaginate(maxSize = 50); ?size=9999 must be clamped.
        mockMvc.perform(get("/reports").param("page", "0").param("size", "9999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(50))
            .andExpect(jsonPath("$.content.length()").value(50));
    }
}
