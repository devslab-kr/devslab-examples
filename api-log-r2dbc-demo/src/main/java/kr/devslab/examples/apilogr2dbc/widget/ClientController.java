package kr.devslab.examples.apilogr2dbc.widget;

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

import kr.devslab.apilog.dto.ApiRequest;
import kr.devslab.apilog.dto.ApiResponse;
import kr.devslab.apilog.util.ReactiveApiClientUtil;
import reactor.core.publisher.Mono;

/**
 * The "client" side of the self-loopback. Every method here makes an outbound
 * HTTP call via {@link ReactiveApiClientUtil}; the starter auto-publishes an
 * INITIATED + (SUCCESS|ERROR) {@code ApiEvent} pair for each call, which the
 * R2DBC backend writes into the {@code api_log} table.
 *
 * <p>All methods return {@link Mono} — the whole path stays non-blocking from
 * the inbound WebFlux request through {@code ReactiveApiClientUtil}'s WebClient
 * call to the upstream and back.
 *
 * <p>The {@code /with-request-id} variant shows the explicit-correlation path:
 * when you build an {@link ApiRequest} with a {@code requestId}, the same id is
 * stamped on both the INITIATED and SUCCESS/ERROR rows so you can join the
 * INITIATED + terminal rows yourself.
 */
@RestController
@RequestMapping("/client/widgets")
public class ClientController {

    private final ReactiveApiClientUtil api;
    private final String upstreamBaseUrl;

    public ClientController(ReactiveApiClientUtil api,
                            @Value("${api-log-demo.upstream-base-url}") String upstreamBaseUrl) {
        this.api = api;
        this.upstreamBaseUrl = upstreamBaseUrl;
    }

    @GetMapping("/{id}")
    public Mono<Widget> get(@PathVariable Long id) {
        return api.getTyped(upstreamBaseUrl + "/upstream/widgets/" + id, Widget.class);
    }

    @PostMapping
    public Mono<Widget> create(@RequestBody Widget body) {
        return api.postTyped(upstreamBaseUrl + "/upstream/widgets", body, Widget.class);
    }

    @PutMapping("/{id}")
    public Mono<Widget> update(@PathVariable Long id, @RequestBody Widget body) {
        return api.putTyped(upstreamBaseUrl + "/upstream/widgets/" + id, body, Widget.class);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return api.delete(upstreamBaseUrl + "/upstream/widgets/" + id)
            .then(Mono.just(ResponseEntity.noContent().build()));
    }

    /**
     * Demonstrates explicit-requestId correlation. Builds an {@link ApiRequest}
     * with a known {@code requestId} so callers can group all rows belonging to
     * one logical operation (including any retries) by querying
     * {@code /api-log/by-request/{requestId}}.
     */
    @PostMapping("/with-request-id/{id}")
    public Mono<ApiResponse> withFixedRequestId(@PathVariable Long id) {
        ApiRequest request = ApiRequest.builder()
            .endpoint(upstreamBaseUrl + "/upstream/widgets/" + id)
            .requestId("demo-fixed-rid")
            .build();
        return api.send(HttpMethod.GET, request);
    }
}
