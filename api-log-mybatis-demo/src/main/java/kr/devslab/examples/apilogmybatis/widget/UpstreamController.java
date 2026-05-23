package kr.devslab.examples.apilogmybatis.widget;

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

import java.math.BigDecimal;

/**
 * The "external service" half of the self-loopback. ClientController calls
 * these endpoints via RestApiClientUtil, which emits api-log events around
 * each call.
 *
 * <p>id == 999 deliberately throws so the error-path test can assert that
 * the failure shows up in api_log as an ERROR row.
 */
@RestController
@RequestMapping("/upstream/widgets")
public class UpstreamController {

    @GetMapping("/{id}")
    public Widget getWidget(@PathVariable long id) {
        if (id == 999L) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "simulated upstream failure");
        }
        return new Widget(id, "Widget-" + id, "SKU-" + id, BigDecimal.valueOf(id * 10));
    }

    @PostMapping
    public Widget createWidget(@RequestBody Widget body) {
        Long id = body.id() != null ? body.id() : System.currentTimeMillis();
        return new Widget(id, body.name(), body.sku(), body.price());
    }

    @PutMapping("/{id}")
    public Widget updateWidget(@PathVariable long id, @RequestBody Widget body) {
        return new Widget(id, body.name(), body.sku(), body.price());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWidget(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }
}
