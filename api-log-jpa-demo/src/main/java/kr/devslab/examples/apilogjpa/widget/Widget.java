package kr.devslab.examples.apilogjpa.widget;

import java.math.BigDecimal;

/**
 * Tiny payload type the demo's client and upstream controllers pass back and
 * forth. Records keep the surface area small — Jackson handles both directions
 * out of the box, so the demo focuses on what api-log captures (the request
 * payload, the response body, the status code) rather than on bean plumbing.
 */
public record Widget(Long id, String name, String sku, BigDecimal price) {
}
