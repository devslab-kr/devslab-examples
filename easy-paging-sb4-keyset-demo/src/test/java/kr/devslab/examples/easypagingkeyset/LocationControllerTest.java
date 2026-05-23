package kr.devslab.examples.easypagingkeyset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end smoke test for the keyset demo. Verifies the full cursor walk:
 * first page produces a {@code nextCursor}, that cursor advances to a new set
 * of rows, the walk continues until {@code hasNext=false}, and the entire
 * seeded data set is returned exactly once with no overlap between pages.
 *
 * <p>This is the property keyset pagination cares most about — every row
 * served exactly once, no gaps, no duplicates — and it's the property that
 * tends to break first when the {@code WHERE} clause's tiebreaker logic is
 * wrong.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LocationControllerTest {

    private static final String WORKER_ID = "00000000-0000-0000-0000-000000000001";
    private static final int TOTAL_SEEDED_ROWS = 300;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void firstPageReturnsRequestedSizeWithNextCursor() throws Exception {
        mockMvc.perform(get("/locations").param("workerId", WORKER_ID).param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(10))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.nextCursor").isNotEmpty());
    }

    @Test
    void walkingNextCursorCoversEveryRowExactlyOnce() throws Exception {
        // size=80 means the 300-row data set fits in 4 pages (80, 80, 80, 60).
        int pageSize = 80;
        Set<Long> seenIds = new HashSet<>();
        String cursor = null;
        int pagesWalked = 0;

        while (pagesWalked < 10) { // safety bound: way more than the expected 4 pages
            MvcResult result = mockMvc.perform(
                    cursor == null
                        ? get("/locations").param("workerId", WORKER_ID).param("size", String.valueOf(pageSize))
                        : get("/locations").param("workerId", WORKER_ID).param("size", String.valueOf(pageSize)).param("cursor", cursor))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
            JsonNode content = body.get("content");
            for (JsonNode row : content) {
                long id = row.get("id").asLong();
                // The fundamental keyset invariant: each row must appear at most once.
                if (!seenIds.add(id)) {
                    throw new AssertionError("Row id=" + id + " appeared on more than one page");
                }
            }
            pagesWalked++;

            if (!body.get("hasNext").asBoolean()) {
                break;
            }
            cursor = body.get("nextCursor").asText();
        }

        if (seenIds.size() != TOTAL_SEEDED_ROWS) {
            throw new AssertionError(
                "Expected " + TOTAL_SEEDED_ROWS + " distinct rows over the cursor walk, got "
                    + seenIds.size() + " in " + pagesWalked + " pages");
        }
    }

    @Test
    void oversizedSizeIsClampedByAnnotationMaxSize() throws Exception {
        // @KeysetPaginate(maxSize = 200) on the controller — ?size=9999 must clamp to 200.
        mockMvc.perform(get("/locations").param("workerId", WORKER_ID).param("size", "9999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(200))
            .andExpect(jsonPath("$.content.length()").value(200));
    }
}
