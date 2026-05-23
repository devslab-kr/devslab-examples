package kr.devslab.examples.ssrfguard.web;

import kr.devslab.ssrfguard.core.SsrfGuardException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Same thing as {@link FetchController}, but using Spring's older
 * {@link RestTemplate}. The point is to show the policy is HTTP-client-
 * neutral — the {@code ssrf-guard-resttemplate} module hooks the same
 * {@code UrlPolicy} onto {@link RestTemplateBuilder}, so the
 * {@code ssrf.guard.*} configuration is applied identically.
 *
 * <p>Most enterprise Spring Boot codebases still use RestTemplate; this
 * controller is the "no migration needed" story.
 *
 * <p>Try:
 * <pre>
 *   curl 'http://localhost:8080/fetch-resttemplate?url=https://httpbin.org/get'
 *   curl 'http://localhost:8080/fetch-resttemplate?url=http://169.254.169.254/'
 * </pre>
 */
@RestController
@RequestMapping("/fetch-resttemplate")
public class FetchResttemplateController {

    private final RestTemplate restTemplate;

    public FetchResttemplateController(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    @GetMapping
    public Map<String, Object> fetch(@RequestParam String url) {
        try {
            String body = restTemplate.getForObject(url, String.class);
            return Map.of(
                    "status", "allowed",
                    "client", "RestTemplate",
                    "url", url,
                    "bodyPreview", preview(body)
            );
        } catch (SsrfGuardException e) {
            return Map.of(
                    "status", "blocked",
                    "client", "RestTemplate",
                    "url", url,
                    "reason", e.reason().label(),
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            // RestTemplate sometimes wraps the SsrfGuardException in
            // ResourceAccessException; unwrap once to surface the real reason.
            Throwable root = e;
            while (root.getCause() != null && root != root.getCause()) {
                if (root.getCause() instanceof SsrfGuardException sg) {
                    return Map.of(
                            "status", "blocked",
                            "client", "RestTemplate",
                            "url", url,
                            "reason", sg.reason().label(),
                            "message", sg.getMessage()
                    );
                }
                root = root.getCause();
            }
            return Map.of(
                    "status", "error",
                    "client", "RestTemplate",
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
