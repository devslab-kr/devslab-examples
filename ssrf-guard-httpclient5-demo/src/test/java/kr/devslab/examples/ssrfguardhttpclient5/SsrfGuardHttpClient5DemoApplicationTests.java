package kr.devslab.examples.ssrfguardhttpclient5;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests for the Apache HttpClient 5 demo. The point isn't to verify
 * Apache HttpClient — it's to verify that ssrf-guard-httpclient5's autoconfig
 * correctly fires {@code SafeDnsResolver} on URLs that ought to be blocked.
 *
 * <p>None of these tests reach the network: the blocked tests bail out at
 * the DNS gate (`SafeDnsResolver` throws `UnknownHostException` before any
 * socket would open), and we don't exercise the allowed path against a real
 * external host.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SsrfGuardHttpClient5DemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void awsMetadataAddressIsBlockedAtDnsGate() throws Exception {
        // 169.254.169.254 is a literal IP — SafeDnsResolver resolves it to
        // itself, then the private-network filter rejects it (link-local).
        mockMvc.perform(get("/fetch").param("url", "http://169.254.169.254/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_dns"));
    }

    @Test
    void decimalEncodedLoopbackIsBlockedAtDnsGate() throws Exception {
        // 2130706433 decodes to 127.0.0.1 — the private-IP filter catches it.
        mockMvc.perform(get("/fetch").param("url", "http://2130706433/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_dns"));
    }

    @Test
    void hostNotInWhitelistIsBlockedAtDnsGate() throws Exception {
        // SafeDnsResolver rejects before even consulting DNS — the message
        // says "Host not in whitelist".
        mockMvc.perform(get("/fetch").param("url", "https://evil.com/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.reason").value("blocked_dns"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Host not in whitelist")));
    }
}
