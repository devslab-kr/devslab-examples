package kr.devslab.examples.easypagingpostgres.product;

import kr.devslab.easypaging.annotation.AutoPaginate;
import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standard paginated catalog listing with an optional category filter — the
 * shape most product/search APIs end up in. The {@code @AutoPaginate} aspect
 * handles size clamping, sort validation, and the Spring Data-shaped JSON
 * envelope; the controller method body stays one line.
 *
 * <p>Sample requests (once the DB is up and the app is running):
 * <pre>
 *   curl 'http://localhost:8080/products?page=0&amp;size=10'
 *   curl 'http://localhost:8080/products?page=0&amp;size=10&amp;sort=price,desc'
 *   curl 'http://localhost:8080/products?category=books&amp;page=0&amp;size=20'
 *   curl 'http://localhost:8080/products?sort=name;DROP%20TABLE%20products'  # rejected
 * </pre>
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    @AutoPaginate(maxSize = 100)
    public PageResponse<Product> list(
            Pageable pageable,
            @RequestParam(required = false) String category) {
        return PageResponse.from(products.findAll(category), pageable);
    }
}
