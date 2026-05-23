package kr.devslab.examples.apilogjpa.widget;

import java.util.List;
import kr.devslab.apilog.jpa.model.ApiLogEntity;
import kr.devslab.apilog.jpa.repository.ApiLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view over the {@code api_log} table. The starter doesn't ship its
 * own reporting API — that's intentional, since every consumer wants something
 * different — so this controller is just enough to let curl examples in the
 * README show how the writes correlate to the calls that produced them.
 *
 * <p>Three lookups, each showcasing a different built-in repository method:
 * <ul>
 *   <li>{@code /api-log/by-request/{requestId}} — full lifecycle for one call
 *       (INITIATED + SUCCESS, or INITIATED + ERROR, etc.)</li>
 *   <li>{@code /api-log/recent} — latest 20 rows across all calls, newest first</li>
 *   <li>{@code /api-log/by-event/{eventType}} — filter by event class</li>
 * </ul>
 */
@RestController
@RequestMapping("/api-log")
public class ApiLogController {

    private final ApiLogRepository repo;

    public ApiLogController(ApiLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/by-request/{requestId}")
    public List<ApiLogEntity> byRequestId(@PathVariable String requestId) {
        return repo.findByRequestId(requestId);
    }

    @GetMapping("/recent")
    public List<ApiLogEntity> recent() {
        return repo.findAll(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent();
    }

    @GetMapping("/by-event/{eventType}")
    public List<ApiLogEntity> byEventType(@PathVariable String eventType) {
        return repo.findByEventType(eventType);
    }
}
