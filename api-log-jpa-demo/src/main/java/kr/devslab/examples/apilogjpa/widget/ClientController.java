package kr.devslab.examples.apilogjpa.widget;

import kr.devslab.apilog.dto.ApiRequest;
import kr.devslab.apilog.dto.ApiResponse;
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
 * The "outbound caller" half of the self-loopback demo. Every method here
 * funnels through {@link RestApiClientUtil}, which publishes
 * {@code ApiCallInitiatedEvent} / {@code ApiCallSuccessEvent} /
 * {@code ApiCallErrorEvent} around each HTTP call. The api-log JPA listener
 * picks those up and writes them to {@code api_log} asynchronously.
 *
 * <p>The base URL is injected from {@code api-log-demo.upstream-base-url} —
 * in this self-contained demo that loops back to this same app's port, so a
 * single {@code ./gradlew bootRun} exercises both sides of the call.
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
    public Widget get(@PathVariable Long id) {
        return api.getSyncTyped(upstreamBaseUrl + "/upstream/widgets/" + id, Widget.class);
    }

    @GetMapping("/{id}/async")
    public Widget getAsync(@PathVariable Long id) {
        // join() makes this synchronous from the controller's perspective so we
        // can still return a body — the underlying call still goes through the
        // async path and exercises sendAsyncTyped + CompletableFuture wiring.
        return api.getAsyncTyped(upstreamBaseUrl + "/upstream/widgets/" + id, Widget.class).join();
    }

    @PostMapping
    public Widget create(@RequestBody Widget body) {
        return api.postSyncTyped(upstreamBaseUrl + "/upstream/widgets", body, Widget.class);
    }

    @PutMapping("/{id}")
    public Widget update(@PathVariable Long id, @RequestBody Widget body) {
        return api.putSyncTyped(upstreamBaseUrl + "/upstream/widgets/" + id, body, Widget.class);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ApiResponse response = api.deleteSync(upstreamBaseUrl + "/upstream/widgets/" + id);
        return ResponseEntity.status(response.getStatusCode()).build();
    }

    /**
     * Demonstrates the core {@code send(HttpMethod, ApiRequest)} overload — the
     * caller supplies an explicit {@code requestId} so multiple related calls
     * (e.g. a retry sequence) share a single correlation key in {@code api_log}.
     * The {@code requestId} is echoed in the response so callers can immediately
     * look up the lifecycle via {@code GET /api-log/by-request/{requestId}}.
     */
    @PostMapping("/with-request-id/{id}")
    public ApiResponse postWithRequestId(@PathVariable Long id) {
        ApiRequest request = ApiRequest.builder()
                .endpoint(upstreamBaseUrl + "/upstream/widgets/" + id)
                .requestId("demo-fixed-rid")
                .build();
        return api.send(HttpMethod.GET, request);
    }
}
