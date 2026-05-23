package kr.devslab.examples.ssrfguardjdkhttp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SsrfGuardJdkHttpDemoApplicationTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void awsMetadataIsBlocked() throws Exception {
        mockMvc.perform(get("/fetch").param("url", "http://169.254.169.254/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_ip_literal"));
    }

    @Test
    void disallowedHostIsBlocked() throws Exception {
        mockMvc.perform(get("/fetch").param("url", "https://evil.com/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_host"));
    }
}
