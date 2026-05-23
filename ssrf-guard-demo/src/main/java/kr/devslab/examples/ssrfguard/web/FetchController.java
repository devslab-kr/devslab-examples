package kr.devslab.examples.ssrfguard.web;

import kr.devslab.ssrfguard.core.SsrfGuardException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * The "happy-path" demo controller — fetches an arbitrary URL and returns
 * either the response body or a structured error.
 *
 * <p>The interesting part is that you don't see any guard wiring here.
 * Spring Boot auto-built the {@link RestClient.Builder} and ssrf-guard's
 * auto-configuration silently pinned its {@code UrlPolicy} +
 * {@code HttpComponentsClientHttpRequestFactory} onto it. Every call below
 * goes through the four-layer SSRF filter:
 *
 * <ol>
 *   <li>URL-time policy (scheme, host whitelist, port, IP literal, userinfo)</li>
 *   <li>DNS-time host re-check + private-IP filter</li>
 *   <li>Socket connect uses the validated IP — no second DNS lookup</li>
 *   <li>Every redirect hop is re-validated</li>
 * </ol>
 *
 * <p>Try:
 * <pre>
 *   # Allowed (httpbin.org is in the whitelist)
 *   curl 'http://localhost:8080/fetch?url=https://httpbin.org/get'
 *
 *   # Blocked — see {@link AttackDemoController} for the full attack matrix.
 *   curl 'http://localhost:8080/fetch?url=http://169.254.169.254/latest/meta-data/'
 *   curl 'http://localhost:8080/fetch?url=http://2130706433/'
 *   curl 'http://localhost:8080/fetch?url=https://evil.com/'
 * </pre>
 */
@RestController
@RequestMapping("/fetch")
public class FetchController {

    private final RestClient restClient;

    // RestClient.Builder is auto-configured by Spring Boot AND auto-customised
    // by ssrf-guard. We do NOT add the SSRF interceptor manually — it's already
    // there.
    public FetchController(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @GetMapping
    public Map<String, Object> fetch(@RequestParam String url) {
        try {
            String body = restClient.get().uri(url).retrieve().body(String.class);
            return Map.of(
                    "status", "allowed",
                    "client", "RestClient",
                    "url", url,
                    "bodyPreview", preview(body)
            );
        } catch (SsrfGuardException e) {
            // ssrf-guard rejected the URL before any network IO. The exception
            // carries a `BlockReason` enum so we can render a structured response.
            return Map.of(
                    "status", "blocked",
                    "client", "RestClient",
                    "url", url,
                    "reason", e.reason().label(),
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            // Any other failure (e.g. real httpbin returned 5xx) is surfaced
            // separately so the demo doesn't conflate them with SSRF blocks.
            return Map.of(
                    "status", "error",
                    "client", "RestClient",
                    "url", url,
                    "error", e.getClass().getSimpleName(),
                    "message", String.valueOf(e.getMessage())
            );
        }
    }

    private static String preview(String body) {
        if (body == null) return null;
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
