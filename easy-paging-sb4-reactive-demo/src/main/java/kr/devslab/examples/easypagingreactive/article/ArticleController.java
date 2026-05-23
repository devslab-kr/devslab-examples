package kr.devslab.examples.easypagingreactive.article;

import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Reactive (WebFlux) controller. The wire-level contract — JSON envelope shape,
 * page-size handling, sort syntax — is identical to the {@code easy-paging-demo}
 * and {@code easy-paging-postgres-demo} controllers. The only difference here
 * is the return type ({@code Mono<PageResponse<T>>}) and the fact that no
 * thread is blocked while the DB round-trips run.
 *
 * <p>Notice: no {@code @AutoPaginate} annotation. On the reactive path,
 * pagination is wired in the service via
 * {@link kr.devslab.easypaging.r2dbc.R2dbcOffsetPagingSupport} rather than by
 * an AOP aspect. The aspect machinery is MyBatis-specific; R2DBC has its own
 * Query/Criteria API and the helper integrates with that directly.
 *
 * <p>Sample requests (once the DB is up and the app is running):
 * <pre>
 *   curl 'http://localhost:8080/articles?page=0&amp;size=10'
 *   curl 'http://localhost:8080/articles?page=0&amp;size=10&amp;sort=publishedAt,desc'
 *   curl 'http://localhost:8080/articles?author=alice&amp;page=0&amp;size=20'
 * </pre>
 */
@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articles;

    public ArticleController(ArticleService articles) {
        this.articles = articles;
    }

    @GetMapping
    public Mono<PageResponse<Article>> list(
            Pageable pageable,
            @RequestParam(required = false) String author) {
        return articles.list(author, pageable);
    }
}
