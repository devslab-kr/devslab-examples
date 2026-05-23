package kr.devslab.examples.apilogmybatis.widget;

import kr.devslab.apilog.dto.ApiRequest;
import kr.devslab.apilog.util.RestApiClientUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The "caller" half of the self-loopback. Every method here goes through
 * {@link RestApiClientUtil}, so each call produces three events (INITIATED +
 * SUCCESS, or INITIATED + ERROR) that the starter's listener persists to
 * {@code api_log} via the MyBatis mapper.
 *
 * <p>The {@code upstream-base-url} property points back at this same app
 * ({@code http://localhost:8080}) so the demo is single-process - no extra
 * service to start.
 */
@RestController
@RequestMapping("/client/widgets")
public class ClientController {

    private final RestApiClientUtil api;
    private final String upstreamBaseUrl;

    public ClientController(RestApiClientUtil api,
                            @Value("${api-log-demo.upstream-base-url}") String upstreamBaseUrl) {
        this.api = api;
        this.upstreamBaseUrl = upstreamBaseUrl;
    }

    @GetMapping("/{id}")
    public Widget getWidget(@PathVariable long id) {
        return api.getSyncTyped(upstreamBaseUrl + "/upstream/widgets/" + id, Widget.class);
    }

    @GetMapping("/{id}/async")
    public Widget getWidgetAsync(@PathVariable long id) {
        return api.getAsyncTyped(upstreamBaseUrl + "/upstream/widgets/" + id, Widget.class).join();
    }

    @PostMapping
    public Widget createWidget(@RequestBody Widget body) {
        return api.postSyncTyped(upstreamBaseUrl + "/upstream/widgets", body, Widget.class);
    }

    @PutMapping("/{id}")
    public Widget updateWidget(@PathVariable long id, @RequestBody Widget body) {
        return api.putSyncTyped(upstreamBaseUrl + "/upstream/widgets/" + id, body, Widget.class);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWidget(@PathVariable long id) {
        api.deleteSync(upstreamBaseUrl + "/upstream/widgets/" + id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Demonstrates explicit-requestId correlation - the typical retry pattern.
     * Every attempt for the same logical operation reuses the same requestId
     * so {@code findByRequestId} returns the full attempt history.
     */
    @PostMapping("/with-request-id/{id}")
    public String getWithFixedRequestId(@PathVariable long id) {
        api.send(HttpMethod.GET, ApiRequest.builder()
                .endpoint(upstreamBaseUrl + "/upstream/widgets/" + id)
                .requestId("demo-fixed-rid")
                .build());
        return "demo-fixed-rid";
    }
}
