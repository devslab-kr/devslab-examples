package kr.devslab.examples.ssrfguardnativeimage;

import kr.devslab.ssrfguard.core.SsrfGuardException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints:
 * <ul>
 *   <li>{@code /fetch?url=...} — guarded fetch through {@link RestClient}.</li>
 *   <li>{@code /attacks} — the same 12-pattern attack catalog the main
 *       ssrf-guard-demo exposes, scoped to what's interesting here.</li>
 * </ul>
 */
@RestController
public class NativeImageDemoController {

    private final RestClient restClient;

    public NativeImageDemoController(RestClient restClient) {
        this.restClient = restClient;
    }

    @GetMapping("/fetch")
    public Map<String, Object> fetch(@RequestParam String url) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("runtime", System.getProperty("org.graalvm.nativeimage.imagecode") != null
                ? "graalvm-native"
                : "jvm");
        response.put("url", url);
        try {
            String body = restClient.get().uri(url).retrieve().body(String.class);
            response.put("status", "allowed");
            response.put("bodyPreview",
                    body == null || body.length() <= 200 ? body : body.substring(0, 200) + "...");
            return response;
        } catch (SsrfGuardException e) {
            // The block reason serializes through SsrfBlockPayload, which
            // has explicit GraalVM reflection hints (it's a record + Jackson
            // serialization in the wire path).
            response.put("status", "blocked");
            response.put("reason", e.reason().label());
            response.put("message", e.getMessage());
            return response;
        }
    }

    /**
     * Same 12 patterns as the main demo. Each one round-trips through the
     * SSRF interceptor, which uses the {@code UrlPolicy}, {@code HostPolicy},
     * and {@code SsrfBlockPayload} types — every reflective surface
     * registered by ssrf-guard's RuntimeHintsRegistrar.
     */
    @GetMapping("/attacks")
    public Map<String, Object> attacks() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("description",
                "12 attack URLs that should all block. Run each via /fetch?url=... "
                + "and confirm the response says status: blocked. If anything "
                + "comes back status: error with a MissingReflectionRegistrationError "
                + "or NullPointerException in the native binary, that's a hint gap.");
        root.put("scenarios", List.of(
                Map.of("name", "AWS metadata (IP literal)",       "url", "http://169.254.169.254/latest/meta-data/iam/security-credentials/"),
                Map.of("name", "GCP metadata (internal host)",    "url", "http://metadata.google.internal/computeMetadata/v1/instance/"),
                Map.of("name", "Decimal-encoded loopback",        "url", "http://2130706433/"),
                Map.of("name", "Hex-encoded loopback",            "url", "http://0x7f000001/"),
                Map.of("name", "Octal-style loopback",            "url", "http://0177.0.0.1/"),
                Map.of("name", "Partial-form loopback",           "url", "http://127.1/"),
                Map.of("name", "IPv4-mapped IPv6 loopback",       "url", "http://[::ffff:127.0.0.1]/"),
                Map.of("name", "IPv4-mapped IPv6 private",        "url", "http://[::ffff:10.0.0.5]/admin"),
                Map.of("name", "Private RFC1918 host",            "url", "http://10.0.0.5/internal-api/users"),
                Map.of("name", "Userinfo with non-whitelisted host", "url", "https://user:pass@evil.com/leak"),
                Map.of("name", "Non-whitelisted external host",   "url", "https://evil.com/exfiltrate"),
                Map.of("name", "httpbin redirect → AWS metadata", "url", "https://httpbin.org/redirect-to?url=http://169.254.169.254/")
        ));
        return root;
    }
}
