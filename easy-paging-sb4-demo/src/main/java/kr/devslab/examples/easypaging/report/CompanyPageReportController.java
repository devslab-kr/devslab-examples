package kr.devslab.examples.easypaging.report;

import kr.devslab.easypaging.annotation.AutoPaginate;
import kr.devslab.examples.easypaging.envelope.CompanyPage;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pattern 1 of two for the custom-envelope advanced section.
 *
 * <p>The controller's declared return type is the company's own
 * {@link CompanyPage} record, and the method body explicitly calls
 * {@link CompanyPage#from} to produce it. The {@code @AutoPaginate} aspect
 * still handles the PageHelper lifecycle and the sort/size validation; it
 * just doesn't construct the envelope itself.
 *
 * <p>Trade-offs vs. the factory-bean approach
 * ({@link AutoEnvelopeReportController}):
 *
 * <ul>
 *   <li>Full type safety — return type is {@code CompanyPage<Report>}, not {@code Object}</li>
 *   <li>Each controller method opts in explicitly, so endpoints can mix and match envelope shapes</li>
 *   <li>Trivially testable — {@code CompanyPage.from} is a pure static method</li>
 * </ul>
 *
 * <p>Try it:
 * <pre>
 *   curl 'http://localhost:8080/reports/company?page=0&amp;size=5'
 * </pre>
 */
@RestController
@RequestMapping("/reports/company")
public class CompanyPageReportController {

    private final ReportService reports;

    public CompanyPageReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    @AutoPaginate(maxSize = 50)
    public CompanyPage<Report> list(Pageable pageable) {
        return CompanyPage.from(reports.findAll(), pageable);
    }
}
