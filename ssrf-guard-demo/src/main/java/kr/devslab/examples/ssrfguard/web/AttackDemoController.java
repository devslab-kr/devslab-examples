package kr.devslab.examples.ssrfguard.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalog of the SSRF attack patterns the demo guard blocks. Each entry has
 * the URL the attacker would supply, the rule that catches it, and a
 * {@code try} field with a pre-built curl. Useful for live walkthroughs and
 * for verifying the guard against a known matrix without remembering each
 * URL by hand.
 *
 * <p>The attack URLs themselves are <i>real</i> in the sense that they're
 * exactly what would appear in a CTF / bug-bounty SSRF — references for
 * each are at <a href="https://github.com/JoyChou93/java-sec-code">java-sec-code</a>
 * (Java vulnerability training).
 *
 * <p>Try:
 * <pre>
 *   curl 'http://localhost:8080/attacks' | jq
 *   # Then pick any attack and run its `try` curl to see the block in action.
 * </pre>
 */
@RestController
@RequestMapping("/attacks")
public class AttackDemoController {

    @GetMapping
    public Map<String, Object> attacks() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("description",
                "URLs an attacker might supply to a vulnerable Spring Boot service. " +
                "Each one is blocked by ssrf-guard at one of the four defense layers — " +
                "URL-time check, DNS-time whitelist re-check, IP filter, redirect re-validation.");
        root.put("attacks", List.of(
                attack("aws-metadata-credentials",
                        "http://169.254.169.254/latest/meta-data/iam/security-credentials/",
                        "blocked_private_ip",
                        "AWS IMDSv1 credential theft — the canonical SSRF→cloud-takeover chain. " +
                                "Caught by the DNS-time private-IP filter (169.254.0.0/16 is link-local)."),

                attack("gcp-metadata",
                        "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/",
                        "blocked_host",
                        "GCP metadata server. metadata.google.internal isn't in the host whitelist — " +
                                "the URL-time check rejects it before DNS is even tried."),

                attack("decimal-ip-literal",
                        "http://2130706433/",
                        "blocked_ip_literal",
                        "127.0.0.1 written as a single decimal integer. Java's InetAddress parsed " +
                                "this on older JDKs; ssrf-guard's URL-time IP-literal detector rejects " +
                                "anything that looks like an IP regardless of the JDK behavior."),

                attack("hex-ip-literal",
                        "http://0x7f000001/",
                        "blocked_ip_literal",
                        "127.0.0.1 in hex. Same defense as the decimal form."),

                attack("octal-ip-literal",
                        "http://0177.0.0.1/",
                        "blocked_ip_literal",
                        "127.0.0.1 with octal leading zeros. Each labelled octal."),

                attack("short-form-ip-literal",
                        "http://127.1/",
                        "blocked_ip_literal",
                        "127.0.0.1 in dotted shorthand."),

                attack("ipv4-mapped-ipv6",
                        "http://[::ffff:127.0.0.1]/",
                        "blocked_ip_literal",
                        "Loopback via IPv4-mapped IPv6. ssrf-guard unmaps the v4 inside and " +
                                "classifies it correctly as loopback."),

                attack("ipv4-mapped-private-network",
                        "http://[::ffff:10.0.0.5]/",
                        "blocked_ip_literal",
                        "Internal RFC-1918 host via IPv4-mapped IPv6. Java's isLoopbackAddress() " +
                                "doesn't catch this — ssrf-guard unmaps and re-checks."),

                attack("ipv6-loopback",
                        "http://[::1]/",
                        "blocked_ip_literal",
                        "Plain IPv6 loopback."),

                attack("private-network-direct",
                        "http://10.0.0.5/",
                        "blocked_ip_literal",
                        "Direct RFC-1918 IP. IP-literal check fires first; even if it didn't, " +
                                "the DNS-time private-IP filter would catch the resolved address."),

                attack("userinfo-bypass",
                        "https://user:pass@evil.com/",
                        "blocked_userinfo",
                        "URLs with embedded credentials are blocked entirely — known SSRF bypass " +
                                "vector (the resolved host depends on parser quirks) AND a credential " +
                                "leak risk on its own."),

                attack("disallowed-host",
                        "https://evil.com/api/internal-data",
                        "blocked_host",
                        "Host not in the whitelist (`exact-hosts` or `suffixes`). The first " +
                                "defense — domain-name allowlist enforced at URL-time."),

                attack("disallowed-scheme",
                        "file:///etc/passwd",
                        "blocked_scheme",
                        "Non-HTTP scheme. Useful when the underlying client library would " +
                                "otherwise accept it (HttpClient 5 won't, but Apache HttpComponents 4 will)."),

                attack("disallowed-port",
                        "https://httpbin.org:8080/get",
                        "blocked_port",
                        "Whitelisted host but a non-whitelisted port. Catches reverse-proxy / " +
                                "admin-port pivots."),

                attack("redirect-to-private",
                        "https://httpbin.org/redirect-to?url=http://169.254.169.254/",
                        "blocked_redirect",
                        "Whitelisted host returns a 302 to AWS metadata. ssrf-guard re-validates " +
                                "every redirect hop through the same policy — the metadata IP is " +
                                "caught at hop 2, not silently followed.")
        ));
        return root;
    }

    private static Map<String, Object> attack(String name, String url, String reason, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("url", url);
        m.put("expectedReason", reason);
        m.put("description", description);
        // Pre-built curl, ready to copy-paste. URL-encoded so the &-laden ones
        // (like httpbin redirect) survive shell expansion intact.
        m.put("tryRestClient", "curl 'http://localhost:8080/fetch?url=" + urlEncode(url) + "'");
        m.put("tryRestTemplate", "curl 'http://localhost:8080/fetch-resttemplate?url=" + urlEncode(url) + "'");
        m.put("tryWebClient", "curl 'http://localhost:8080/fetch-webclient?url=" + urlEncode(url) + "'");
        return m;
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
