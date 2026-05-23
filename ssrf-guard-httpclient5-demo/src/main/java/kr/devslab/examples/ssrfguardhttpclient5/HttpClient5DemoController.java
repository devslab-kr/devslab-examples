package kr.devslab.examples.ssrfguardhttpclient5;

import kr.devslab.ssrfguard.core.SsrfGuardException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <pre>
 *   curl 'http://localhost:8080/fetch?url=https://httpbin.org/get'
 *   curl 'http://localhost:8080/fetch?url=http://169.254.169.254/'
 *   curl 'http://localhost:8080/fetch?url=http://2130706433/'
 *   curl 'http://localhost:8080/fetch?url=https://evil.com/'
 * </pre>
 *
 * <p>The injected {@link CloseableHttpClient} is the SSRF-guarded one created
 * by {@code SsrfGuardHttpClient5AutoConfiguration}. The controller code below
 * doesn't reference the guard at all — call sites stay clean.
 */
@RestController
public class HttpClient5DemoController {

    private final CloseableHttpClient client;

    public HttpClient5DemoController(CloseableHttpClient client) {
        this.client = client;
    }

    @GetMapping("/fetch")
    public Map<String, Object> fetch(@RequestParam String url) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client", "Apache HttpClient 5");
        response.put("url", url);

        HttpGet get = new HttpGet(url);
        try {
            return client.execute(get, http -> {
                response.put("status", "allowed");
                response.put("httpStatus", http.getCode());
                HttpEntity entity = http.getEntity();
                String body = entity == null ? null : EntityUtils.toString(entity);
                if (body != null) {
                    response.put("bodyPreview",
                            body.length() <= 200 ? body : body.substring(0, 200) + "...");
                }
                return response;
            });
        } catch (SsrfGuardException e) {
            // Rare — SafeRedirectStrategy throws RedirectException, which
            // HttpClient wraps. The interceptor in other demos throws
            // SsrfGuardException directly; here SafeDnsResolver throws
            // UnknownHostException (see below). Kept for symmetry.
            response.put("status", "blocked");
            response.put("reason", e.reason().label());
            response.put("message", e.getMessage());
            return response;
        } catch (UnknownHostException e) {
            // The DNS-time gate's failure shape: when SafeDnsResolver
            // refuses to resolve (host not in whitelist, or all IPs
            // private after filtering), HttpClient surfaces it as
            // UnknownHostException. The message tells you which gate
            // fired ("Host not in whitelist" / "No allowed IP after
            // filtering").
            response.put("status", "blocked");
            response.put("reason", "blocked_dns");
            response.put("message", e.getMessage());
            return response;
        } catch (Exception e) {
            // Walk the cause chain — HttpClient sometimes wraps the
            // RedirectException (from SafeRedirectStrategy) further up.
            Throwable root = e;
            while (root != null) {
                if (root instanceof SsrfGuardException sg) {
                    response.put("status", "blocked");
                    response.put("reason", sg.reason().label());
                    response.put("message", sg.getMessage());
                    return response;
                }
                if (root.getCause() == root) break;
                root = root.getCause();
            }
            response.put("status", "error");
            response.put("error", e.getClass().getSimpleName());
            response.put("message", String.valueOf(e.getMessage()));
            return response;
        }
    }
}
