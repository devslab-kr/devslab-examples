package kr.devslab.examples.ssrfguardfeign;

import kr.devslab.ssrfguard.core.SsrfGuardException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Two endpoints to drive the two Feign clients:
 * <pre>
 *   curl http://localhost:8080/feign/legit   # httpbin.org is whitelisted → succeeds
 *   curl http://localhost:8080/feign/evil    # evil.com is not → blocked at Feign interceptor
 * </pre>
 */
@RestController
public class FeignDemoController {

    private final HttpBinClient httpBin;
    private final EvilClient evil;

    public FeignDemoController(HttpBinClient httpBin, EvilClient evil) {
        this.httpBin = httpBin;
        this.evil = evil;
    }

    @GetMapping("/feign/legit")
    public Map<String, Object> legit() {
        try {
            String body = httpBin.get();
            return Map.of(
                    "status", "allowed",
                    "feignClient", "HttpBinClient",
                    "url", "https://httpbin.org/get",
                    "bodyPreview", body == null ? null : body.substring(0, Math.min(150, body.length())) + "..."
            );
        } catch (Exception e) {
            return errorMap(e, "HttpBinClient", "https://httpbin.org/get");
        }
    }

    @GetMapping("/feign/evil")
    public Map<String, Object> evil() {
        try {
            String body = evil.hit();
            return Map.of(
                    "status", "allowed-but-suspicious",
                    "feignClient", "EvilClient",
                    "url", "https://evil.com/",
                    "bodyPreview", body
            );
        } catch (Exception e) {
            return errorMap(e, "EvilClient", "https://evil.com/");
        }
    }

    private Map<String, Object> errorMap(Exception e, String client, String url) {
        // Feign wraps the SsrfGuardException once (RetryableException /
        // FeignException ancestor). Walk the chain.
        Throwable root = e;
        while (root != null) {
            if (root instanceof SsrfGuardException sg) {
                return Map.of(
                        "status", "blocked",
                        "feignClient", client,
                        "url", url,
                        "reason", sg.reason().label(),
                        "message", sg.getMessage()
                );
            }
            if (root.getCause() == root) break;
            root = root.getCause();
        }
        return Map.of(
                "status", "error",
                "feignClient", client,
                "url", url,
                "error", e.getClass().getSimpleName(),
                "message", String.valueOf(e.getMessage())
        );
    }
}
