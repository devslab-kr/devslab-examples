package kr.devslab.examples.easypaging.report;

import kr.devslab.easypaging.annotation.AutoPaginate;
import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The whole point of the demo is right here: one annotation, one mapper call,
 * one wrapped response. Page-size clamping, 0-based pagination, safe sort,
 * total counts, and the Spring Data-shaped JSON envelope all come from
 * {@link AutoPaginate}.
 *
 * <p>Try it (once the app is running):
 * <pre>
 *   curl 'http://localhost:8080/reports?page=0&amp;size=5'
 *   curl 'http://localhost:8080/reports?page=2&amp;size=5&amp;sort=createdAt,desc'
 *   curl 'http://localhost:8080/reports?size=9999'       # clamped to maxSize=50
 *   curl 'http://localhost:8080/reports?sort=id;DROP'    # rejected (HTTP 400)
 * </pre>
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    @AutoPaginate(maxSize = 50)
    public PageResponse<Report> list(Pageable pageable) {
        return PageResponse.from(reports.findAll(), pageable);
    }
}
