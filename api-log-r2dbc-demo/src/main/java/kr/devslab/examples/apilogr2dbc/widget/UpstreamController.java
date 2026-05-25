package kr.devslab.examples.apilogr2dbc.widget;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

/**
 * The "upstream" side of the self-loopback. In a real deployment this would be
 * a separate service — for the demo it lives in the same JVM and the {@link
 * ClientController} calls it via {@code http://localhost:8080/upstream/widgets}.
 *
 * <p>WebFlux returns {@link Mono}/{@code Flux} from every handler; that keeps
 * the entire request path off the platform thread pool and matches what the
 * R2DBC backend on the api-log writer side expects too.
 *
 * <p>The {@code id == 999} branch exists specifically so the integration test
 * has a way to assert that ERROR rows are written when the upstream returns 5xx.
 */
@RestController
@RequestMapping("/upstream/widgets")
public class UpstreamController {

    @GetMapping("/{id}")
    public Mono<Widget> get(@PathVariable Long id) {
        if (id == 999L) {
            return Mono.error(new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "simulated upstream failure"));
        }
        return Mono.just(new Widget(id, "Widget-" + id, "SKU-" + id, BigDecimal.valueOf(id * 10)));
    }

    @PostMapping
    public Mono<Widget> create(@RequestBody Widget body) {
        Long assignedId = body.id() != null ? body.id() : System.currentTimeMillis();
        return Mono.just(new Widget(assignedId, body.name(), body.sku(), body.price()));
    }

    @PutMapping("/{id}")
    public Mono<Widget> update(@PathVariable Long id, @RequestBody Widget body) {
        return Mono.just(new Widget(id, body.name(), body.sku(), body.price()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return Mono.just(ResponseEntity.noContent().build());
    }
}
