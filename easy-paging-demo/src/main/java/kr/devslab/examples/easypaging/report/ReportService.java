package kr.devslab.examples.easypaging.report;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Plain service layer. In a real application, this is where authorization,
 * tenant filtering, and domain rules would live. The pagination concern is
 * intentionally absent here — it's handled by the aspect at the controller
 * boundary.
 */
@Service
public class ReportService {

    private final ReportMapper mapper;

    public ReportService(ReportMapper mapper) {
        this.mapper = mapper;
    }

    public List<Report> findAll() {
        return mapper.findAll();
    }
}
