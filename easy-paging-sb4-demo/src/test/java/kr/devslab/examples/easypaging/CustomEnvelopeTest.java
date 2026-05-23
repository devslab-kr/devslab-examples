package kr.devslab.examples.easypaging;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Verifies the two "custom response envelope" patterns documented in the
 * demo's README:
 *
 * <ol>
 *   <li>{@code CompanyPage<T>} as the controller's explicit return type
 *       ({@code CompanyPageReportController}, mapped at {@code /reports/company}).</li>
 *   <li>{@code Object} return + a {@code PageResponseFactory} bean
 *       ({@code AutoEnvelopeReportController} + {@code CompanyEnvelopeConfig},
 *       mapped at {@code /reports/auto-envelope}).</li>
 * </ol>
 *
 * <p>The interesting property the second test pair asserts is that
 * <strong>both endpoints produce identical JSON</strong> — the
 * {@code PageResponseFactory} bean and the static {@code CompanyPage.from}
 * factory must agree on shape, since the whole reason to register the bean
 * is to avoid every controller repeating the {@code .from(...)} call by hand.
 *
 * <p>The third test asserts the factory bean does <em>not</em> bleed into
 * controllers that already return an explicit {@code PageResponse<T>} — the
 * main {@code /reports} endpoint keeps its default envelope shape regardless
 * of the bean being registered.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomEnvelopeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void companyPageEndpointReturnsCompanyShape() throws Exception {
        mockMvc.perform(get("/reports/company").param("page", "0").param("size", "5"))
            .andExpect(status().isOk())
            // CompanyPage shape: { ok, data: [...], meta: { page, size, total, pages } }
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.meta.page").value(0))
            .andExpect(jsonPath("$.meta.size").value(5))
            .andExpect(jsonPath("$.meta.total").value(137))
            .andExpect(jsonPath("$.meta.pages").value(28))
            // None of the default PageResponse keys should be present
            .andExpect(jsonPath("$.content").doesNotExist())
            .andExpect(jsonPath("$.totalElements").doesNotExist());
    }

    @Test
    void autoEnvelopeEndpointReturnsSameShapeAsCompanyEndpoint() throws Exception {
        // Same query, two different code paths — must produce byte-equal JSON.
        MvcResult viaStaticFactory = mockMvc.perform(
                get("/reports/company").param("page", "1").param("size", "10"))
            .andExpect(status().isOk()).andReturn();

        MvcResult viaFactoryBean = mockMvc.perform(
                get("/reports/auto-envelope").param("page", "1").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(content().json(viaStaticFactory.getResponse().getContentAsString(), true))
            .andReturn();

        // Sanity-check the body itself has the company shape (not just that the two strings match).
        // If both endpoints regressed to producing the same wrong thing, the equality above
        // alone wouldn't catch it.
        mockMvc.perform(get("/reports/auto-envelope").param("page", "0").param("size", "3"))
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.meta.total").value(137));
    }

    @Test
    void defaultEndpointKeepsPageResponseShapeDespiteFactoryBean() throws Exception {
        // /reports returns PageResponse<Report> explicitly. Even with the
        // PageResponseFactory bean registered, it should pass through unchanged.
        mockMvc.perform(get("/reports").param("page", "0").param("size", "5"))
            .andExpect(status().isOk())
            // Default PageResponse keys — must be present
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.totalElements").value(137))
            .andExpect(jsonPath("$.totalPages").value(28))
            // CompanyPage keys — must NOT be present here
            .andExpect(jsonPath("$.ok").doesNotExist())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.meta").doesNotExist());
    }
}
