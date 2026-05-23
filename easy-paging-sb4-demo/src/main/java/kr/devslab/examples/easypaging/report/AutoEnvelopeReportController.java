package kr.devslab.examples.easypaging.report;

import kr.devslab.easypaging.annotation.AutoPaginate;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pattern 2 of two for the custom-envelope advanced section.
 *
 * <p>The controller method declares its return type as {@code Object} and
 * hands the aspect a raw {@code List<Report>} from the service. Because there
 * is a {@code PageResponseFactory} bean in the application context
 * (see {@code envelope/CompanyEnvelopeConfig}), the aspect routes the result
 * through it — and the wire output is the same {@code CompanyPage} shape that
 * {@code /reports/company} produces.
 *
 * <p>This is the pattern to reach for when <strong>every</strong> paginated
 * endpoint in an app should use the same envelope and you don't want each
 * controller to repeat the {@code Envelope.from(...)} call.
 *
 * <p>Trade-offs vs. the explicit-return approach
 * ({@link CompanyPageReportController}):
 *
 * <ul>
 *   <li>No type safety on the return: it's {@code Object}, so the IDE can't help</li>
 *   <li>One factory bean, many controllers — DRY for company-wide standardization</li>
 *   <li>Testing requires the factory bean to be in the context (so {@code @SpringBootTest}, not pure unit tests of the controller)</li>
 * </ul>
 *
 * <p>Try it:
 * <pre>
 *   curl 'http://localhost:8080/reports/auto-envelope?page=0&amp;size=5'
 * </pre>
 *
 * <p>The JSON returned by this endpoint is byte-for-byte the same shape as
 * {@code /reports/company} — that's the point of having both: the two
 * patterns produce the same wire output via different code paths.
 */
@RestController
@RequestMapping("/reports/auto-envelope")
public class AutoEnvelopeReportController {

    private final ReportService reports;

    public AutoEnvelopeReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    @AutoPaginate(maxSize = 50)
    public Object list(
            Pageable pageable,
            // Demonstrates that ordinary query params still work in the Object-return
            // pattern — the aspect only cares about the pagination contract.
            @RequestParam(required = false) String ignoredFilter) {
        return reports.findAll();
    }
}
