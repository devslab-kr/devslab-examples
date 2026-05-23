package kr.devslab.examples.apilogr2dbc.widget;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * Read endpoints over the {@code api_log} table — useful for live inspection in
 * the demo and for the integration test to poll until expected rows arrive.
 */
@RestController
@RequestMapping("/api-log")
public class ApiLogController {

    private final ApiLogReader reader;

    public ApiLogController(ApiLogReader reader) {
        this.reader = reader;
    }

    @GetMapping("/by-request/{requestId}")
    public Flux<ApiLogView> byRequest(@PathVariable String requestId) {
        return reader.findByRequestId(requestId);
    }

    @GetMapping("/recent")
    public Flux<ApiLogView> recent() {
        return reader.findRecent(20);
    }

    @GetMapping("/by-event/{eventType}")
    public Flux<ApiLogView> byEvent(@PathVariable String eventType) {
        return reader.findByEvent(eventType);
    }
}
