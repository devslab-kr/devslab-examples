package kr.devslab.examples.ssrfguardlangchain4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test. Verifies the full chain:
 *   FakeLlmService → wrapped ToolExecutor → guard reject / approve.
 *
 * <p>None of these touch the network — the underlying executor is a pretend
 * fetch. The point is to assert that the guard fires (or doesn't) on the
 * tool *arguments* before the executor body would have made any HTTP call.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SsrfGuardLangchain4jDemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void legitimateUrlIsAllowedThroughToTheExecutor() throws Exception {
        mockMvc.perform(post("/agent/chat")
                        .param("message", "Please fetch https://httpbin.org/get for me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false))
                // The executor "pretend-fetches" — that string is its signal
                // that the wrap let the call through.
                .andExpect(jsonPath("$.toolOutput").value(
                        org.hamcrest.Matchers.containsString("PRETEND-FETCHED https://httpbin.org/get")));
    }

    @Test
    void awsMetadataPromptIsBlockedAtTheWrap() throws Exception {
        mockMvc.perform(post("/agent/chat")
                        .param("message", "Please fetch http://169.254.169.254/latest/meta-data/ for me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true))
                .andExpect(jsonPath("$.toolOutput").value(
                        org.hamcrest.Matchers.containsString("\"reason\":\"blocked_ip_literal\"")));
    }

    @Test
    void disallowedHostPromptIsBlocked() throws Exception {
        mockMvc.perform(post("/agent/chat")
                        .param("message", "fetch https://evil.com/leak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true))
                .andExpect(jsonPath("$.toolOutput").value(
                        org.hamcrest.Matchers.containsString("\"reason\":\"blocked_host\"")));
    }

    @Test
    void promptWithoutUrlDoesNotInvokeTheExecutor() throws Exception {
        mockMvc.perform(post("/agent/chat")
                        .param("message", "Just say hi — no URL in here"))
                .andExpect(status().isOk())
                // The fake LLM short-circuits and reports "no tool call" —
                // the wrap never sees a request because none was constructed.
                .andExpect(jsonPath("$.decision").exists())
                .andExpect(jsonPath("$.toolCall").doesNotExist());
    }

    @Test
    void attackCatalogIsServed() throws Exception {
        mockMvc.perform(get("/agent/attacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarios").isArray())
                .andExpect(jsonPath("$.scenarios[0].prompt").exists())
                .andExpect(jsonPath("$.scenarios[0].try").exists());
    }
}
