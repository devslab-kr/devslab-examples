package kr.devslab.examples.easypaging.envelope;

import java.util.List;
import kr.devslab.easypaging.spi.PageResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a {@link PageResponseFactory} so that <em>any</em> controller
 * method annotated with {@code @AutoPaginate} that returns {@code Object} (or
 * a raw {@code List<T>}) gets its result wrapped in a {@link CompanyPage}
 * instead of the default {@code PageResponse}.
 *
 * <p>Crucially, this only affects controller methods whose declared return
 * type is {@code Object} or {@code List}. Methods that explicitly return
 * {@code PageResponse<T>} (like the main {@code /reports} endpoint) or
 * {@code CompanyPage<T>} (like {@code /reports/company}) <strong>pass through
 * untouched</strong> — the aspect only routes through this factory when it's
 * the one constructing the response envelope itself.
 *
 * <p>So registering this bean is safe even in an app that mixes the two
 * patterns: explicit-return endpoints keep their declared shape, and only the
 * "let the aspect build it" endpoints share the company-wide format.
 */
@Configuration
class CompanyEnvelopeConfig {

    @Bean
    PageResponseFactory companyEnvelope() {
        return (content, pageable, totalElements, totalPages) ->
                new CompanyPage<>(
                        true,
                        List.copyOf(content),
                        new CompanyPage.PageMeta(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                totalElements,
                                totalPages));
    }
}
