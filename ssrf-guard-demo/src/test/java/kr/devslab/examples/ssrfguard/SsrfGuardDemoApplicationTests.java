package kr.devslab.examples.ssrfguard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test for the demo. Verifies that:
 *   1. The application context boots (ssrf-guard auto-config wires correctly
 *      for all three HTTP clients).
 *   2. The attack matrix endpoint returns the expected catalog shape.
 *   3. An obvious SSRF attempt (AWS metadata) gets the right BlockReason —
 *      no network is touched because the URL-time gate rejects it first.
 *
 * <p>We don't make outbound calls in tests (no internet dependency, no
 * httpbin.org), so the "allowed" path is exercised in the README curls,
 * not here.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SsrfGuardDemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void attackCatalogIsServed() throws Exception {
        mockMvc.perform(get("/attacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attacks").isArray())
                .andExpect(jsonPath("$.attacks[0].name").exists())
                .andExpect(jsonPath("$.attacks[0].expectedReason").exists())
                .andExpect(jsonPath("$.attacks[0].tryRestClient").exists());
    }

    @Test
    void awsMetadataIsBlockedViaRestClient() throws Exception {
        mockMvc.perform(get("/fetch")
                        .param("url", "http://169.254.169.254/latest/meta-data/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                // 169.254.169.254 is an IP literal → caught by the URL-time check
                // (blocked_ip_literal). If rejectIpLiteralHosts were off, the
                // DNS-time filter would catch it as blocked_private_ip.
                .andExpect(jsonPath("$.reason").value("blocked_ip_literal"));
    }

    @Test
    void disallowedHostIsBlockedViaRestTemplate() throws Exception {
        mockMvc.perform(get("/fetch-resttemplate")
                        .param("url", "https://evil.com/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_host"));
    }

    @Test
    void userinfoIsBlockedViaRestClient() throws Exception {
        mockMvc.perform(get("/fetch")
                        .param("url", "https://user:pass@httpbin.org/get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_userinfo"));
    }
}
