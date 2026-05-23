package kr.devslab.examples.ssrfguardokhttp;

import kr.devslab.ssrfguard.core.SsrfGuardException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * <pre>
 *   curl 'http://localhost:8080/fetch?url=https://httpbin.org/get'
 *   curl 'http://localhost:8080/fetch?url=http://169.254.169.254/'
 *   curl 'http://localhost:8080/fetch?url=http://2130706433/'
 * </pre>
 */
@RestController
public class OkHttpDemoController {

    private final OkHttpClient client;

    public OkHttpDemoController(OkHttpClient client) {
        this.client = client;
    }

    @GetMapping("/fetch")
    public Map<String, Object> fetch(@RequestParam String url) {
        Request req = new Request.Builder().url(url).build();
        try (Response resp = client.newCall(req).execute()) {
            String body = resp.body() == null ? null : resp.body().string();
            return Map.of(
                    "status", "allowed",
                    "client", "OkHttp",
                    "url", url,
                    "httpStatus", resp.code(),
                    "bodyPreview", body == null || body.length() <= 200 ? body : body.substring(0, 200) + "..."
            );
        } catch (SsrfGuardException e) {
            return Map.of(
                    "status", "blocked",
                    "client", "OkHttp",
                    "url", url,
                    "reason", e.reason().label(),
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            // OkHttp wraps the SsrfGuardException in IOException when it
            // bubbles up through the dispatcher — walk the chain to find
            // it.
            Throwable root = e;
            while (root != null) {
                if (root instanceof SsrfGuardException sg) {
                    return Map.of(
                            "status", "blocked",
                            "client", "OkHttp",
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
                    "client", "OkHttp",
                    "url", url,
                    "error", e.getClass().getSimpleName(),
                    "message", String.valueOf(e.getMessage())
            );
        }
    }
}
