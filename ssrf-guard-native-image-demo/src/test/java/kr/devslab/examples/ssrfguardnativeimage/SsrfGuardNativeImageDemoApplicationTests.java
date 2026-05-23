package kr.devslab.examples.ssrfguardnativeimage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JVM-mode smoke test. CI runs this — building a native image is too slow
 * and resource-heavy to do on every PR. The native-image build is verified
 * manually by running {@code ./gradlew nativeCompile && ./gradlew nativeRun}
 * locally (see README).
 *
 * <p>What this test proves: with ssrf-guard 3.1.0 on the classpath, the
 * autoconfig wires correctly, the {@code RestClientCustomizer} applies the
 * SSRF interceptor, and the same block path the other demos exercise works
 * here. The native-image-specific verification is in the README workflow.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SsrfGuardNativeImageDemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void awsMetadataIsBlocked() throws Exception {
        mockMvc.perform(get("/fetch").param("url", "http://169.254.169.254/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_ip_literal"))
                .andExpect(jsonPath("$.runtime").value("jvm"));
    }

    @Test
    void disallowedHostIsBlocked() throws Exception {
        mockMvc.perform(get("/fetch").param("url", "https://evil.com/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_host"));
    }

    @Test
    void attackCatalogIsServed() throws Exception {
        mockMvc.perform(get("/attacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarios").isArray())
                .andExpect(jsonPath("$.scenarios.length()").value(12));
    }
}
