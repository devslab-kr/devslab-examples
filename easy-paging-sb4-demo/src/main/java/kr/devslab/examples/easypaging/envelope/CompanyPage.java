package kr.devslab.examples.easypaging.envelope;

import java.util.List;
import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Example of a company-defined paginated response envelope, used by the two
 * "custom response" controllers in this demo.
 *
 * <p>The shape is <em>distinct</em> from the starter's default
 * {@link PageResponse} on purpose so a client hitting
 * {@code /reports/company} or {@code /reports/auto-envelope} sees a JSON body
 * that's visibly different from the one returned by {@code /reports}:
 *
 * <pre>{@code
 * {
 *   "ok": true,
 *   "data": [ ... ],
 *   "meta": { "page": 0, "size": 5, "total": 137, "pages": 28 }
 * }
 * }</pre>
 *
 * <p>{@link CompanyPage#from(List, Pageable)} mirrors {@code PageResponse.from}
 * — it borrows the starter's metadata extraction (the "right" PageHelper
 * unwrapping, the 0-vs-1 indexing dance) and just remaps the fields into the
 * company shape. That keeps the controllers using this type as thin as the
 * default ones.
 */
public record CompanyPage<T>(
        boolean ok,
        List<T> data,
        PageMeta meta) {

    public record PageMeta(int page, int size, long total, int pages) {}

    /** Build from a mapper result + the request {@link Pageable}. */
    public static <T> CompanyPage<T> from(List<T> list, Pageable pageable) {
        PageResponse<T> p = PageResponse.from(list, pageable);
        return new CompanyPage<>(
                true,
                p.content(),
                new PageMeta(p.page(), p.size(), p.totalElements(), p.totalPages()));
    }
}
