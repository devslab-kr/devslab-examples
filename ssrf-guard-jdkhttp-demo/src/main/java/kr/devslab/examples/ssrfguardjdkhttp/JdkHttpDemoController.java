package kr.devslab.examples.ssrfguardjdkhttp;

import kr.devslab.ssrfguard.core.SsrfGuardException;
import kr.devslab.ssrfguard.jdkhttp.SsrfGuardedHttpClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Drives the {@link SsrfGuardedHttpClient} from a REST endpoint so the demo
 * is curl-friendly.
 *
 * <pre>
 *   curl 'http://localhost:8080/fetch?url=https://httpbin.org/get'
 *   curl 'http://localhost:8080/fetch?url=http://169.254.169.254/'
 *   curl 'http://localhost:8080/fetch?url=http://2130706433/'
 * </pre>
 */
@RestController
public class JdkHttpDemoController {

    private final SsrfGuardedHttpClient client;

    public JdkHttpDemoController(SsrfGuardedHttpClient client) {
        this.client = client;
    }

    @GetMapping("/fetch")
    public Map<String, Object> fetch(@RequestParam String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            return Map.of(
                    "status", "allowed",
                    "client", "java.net.http.HttpClient",
                    "url", url,
                    "httpStatus", resp.statusCode(),
                    "bodyPreview", body == null || body.length() <= 200 ? body : body.substring(0, 200) + "..."
            );
        } catch (SsrfGuardException e) {
            return Map.of(
                    "status", "blocked",
                    "client", "java.net.http.HttpClient",
                    "url", url,
                    "reason", e.reason().label(),
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "client", "java.net.http.HttpClient",
                    "url", url,
                    "error", e.getClass().getSimpleName(),
                    "message", String.valueOf(e.getMessage())
            );
        }
    }
}
