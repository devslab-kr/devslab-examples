package kr.devslab.examples.apilogjpa.widget;

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

/**
 * The "external service" half of the self-loopback demo. In a real app this
 * controller would live in a different process — here it shares the JVM with
 * the {@link ClientController} so a {@code docker compose up -d db} is the
 * only external dependency needed to exercise the full request lifecycle.
 *
 * <p>Endpoints intentionally cover the four verbs api-log captures
 * (GET/POST/PUT/DELETE) plus one deliberate error trigger ({@code GET /999})
 * so the integration test can assert {@code event_type = ERROR} rows show up
 * in {@code api_log} too.
 */
@RestController
@RequestMapping("/upstream/widgets")
public class UpstreamController {

    @GetMapping("/{id}")
    public Widget getById(@PathVariable Long id) {
        if (id == 999L) {
            // Deliberate 5xx so the demo can show what an ERROR-type api_log row looks like.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "simulated upstream failure");
        }
        return new Widget(id, "Widget-" + id, "SKU-" + id, BigDecimal.valueOf(id * 10));
    }

    @PostMapping
    public Widget create(@RequestBody Widget body) {
        Long assignedId = body.id() != null ? body.id() : System.currentTimeMillis();
        return new Widget(assignedId, body.name(), body.sku(), body.price());
    }

    @PutMapping("/{id}")
    public Widget update(@PathVariable Long id, @RequestBody Widget body) {
        return new Widget(id, body.name(), body.sku(), body.price());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
