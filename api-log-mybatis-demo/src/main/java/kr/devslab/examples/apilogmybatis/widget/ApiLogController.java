package kr.devslab.examples.apilogmybatis.widget;

import kr.devslab.apilog.mybatis.mapper.ApiLogMapper;
import kr.devslab.apilog.mybatis.model.ApiLogRow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reader endpoints over {@code api_log}. Lets the integration test (and a
 * human poking around) inspect what the writer produced - the starter only
 * exposes the writer side, so visibility is the consumer's job.
 *
 * <p>Two mappers used:
 * <ul>
 *   <li>{@link ApiLogMapper} - the starter's built-in, used here for the
 *       {@code findByRequestId} correlation lookup.</li>
 *   <li>{@link ApiLogQueryMapper} - this demo's custom mapper, for "recent N"
 *       and "by event type" reads the starter doesn't ship.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api-log")
public class ApiLogController {

    private final ApiLogMapper apiLogMapper;
    private final ApiLogQueryMapper queryMapper;

    public ApiLogController(ApiLogMapper apiLogMapper, ApiLogQueryMapper queryMapper) {
        this.apiLogMapper = apiLogMapper;
        this.queryMapper = queryMapper;
    }

    @GetMapping("/by-request/{requestId}")
    public List<ApiLogRow> byRequest(@PathVariable String requestId) {
        return apiLogMapper.findByRequestId(requestId);
    }

    @GetMapping("/recent")
    public List<ApiLogRow> recent() {
        return queryMapper.findRecent(20);
    }

    @GetMapping("/by-event/{eventType}")
    public List<ApiLogRow> byEvent(@PathVariable String eventType) {
        return queryMapper.findByEvent(eventType);
    }
}
