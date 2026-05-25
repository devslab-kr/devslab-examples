package kr.devslab.examples.apilogmybatis.widget;

import kr.devslab.apilog.dto.ApiRequest;
import kr.devslab.apilog.util.RestApiClientUtil;
import org.springframework.core.env.Environment;
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
 * <p>The upstream URL is built at request time from {@code local.server.port}
 * (a property Spring Boot sets after the embedded server binds). That makes the
 * self-loopback work both in {@code ./gradlew bootRun} (port 8080) and in the
 * integration test ({@code RANDOM_PORT}) without any test-specific override.
 * In a real app this method would point at the external service URL — the demo's
 * only special thing is that "upstream" lives in the same JVM for convenience.
 */
@RestController
@RequestMapping("/client/widgets")
public class ClientController {

    private final RestApiClientUtil api;
    private final Environment env;

    public ClientController(RestApiClientUtil api, Environment env) {
        this.api = api;
        this.env = env;
    }

    private String upstream(String path) {
        return "http://localhost:" + env.getProperty("local.server.port") + path;
    }

    @GetMapping("/{id}")
    public Widget getWidget(@PathVariable long id) {
        return api.getSyncTyped(upstream("/upstream/widgets/" + id), Widget.class);
    }

    @GetMapping("/{id}/async")
    public Widget getWidgetAsync(@PathVariable long id) {
        return api.getAsyncTyped(upstream("/upstream/widgets/" + id), Widget.class).join();
    }

    @PostMapping
    public Widget createWidget(@RequestBody Widget body) {
        return api.postSyncTyped(upstream("/upstream/widgets"), body, Widget.class);
    }

    @PutMapping("/{id}")
    public Widget updateWidget(@PathVariable long id, @RequestBody Widget body) {
        return api.putSyncTyped(upstream("/upstream/widgets/" + id), body, Widget.class);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWidget(@PathVariable long id) {
        api.deleteSync(upstream("/upstream/widgets/" + id));
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
                .endpoint(upstream("/upstream/widgets/" + id))
                .requestId("demo-fixed-rid")
                .build());
        return "demo-fixed-rid";
    }
}
