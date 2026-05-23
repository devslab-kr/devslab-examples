package kr.devslab.examples.easypagingreactive.article;

import kr.devslab.easypaging.core.PageResponse;
import kr.devslab.easypaging.r2dbc.R2dbcOffsetPagingSupport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * The reactive equivalent of the postgres demo's {@code ProductService}.
 * Instead of MyBatis + PageHelper, this uses Spring Data R2DBC's
 * {@code R2dbcEntityTemplate} and the reactive starter's
 * {@link R2dbcOffsetPagingSupport} helper to produce the same
 * {@link PageResponse} envelope inside a {@link Mono}.
 *
 * <p>{@code R2dbcOffsetPagingSupport.paginate(...)} runs the rows query and
 * the count query in parallel via {@code Mono.zip}, so the paginated endpoint
 * pays one round-trip of latency for both.
 */
@Service
public class ArticleService {

    private final R2dbcEntityTemplate template;

    public ArticleService(R2dbcEntityTemplate template) {
        this.template = template;
    }

    public Mono<PageResponse<Article>> list(String author, Pageable pageable) {
        Criteria criteria = (author == null)
                ? Criteria.empty()
                : Criteria.where("author").is(author);

        return R2dbcOffsetPagingSupport.paginate(template, Article.class, criteria, pageable);
    }
}
