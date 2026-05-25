package kr.devslab.examples.apilogmybatis.widget;

import java.math.BigDecimal;

/**
 * The demo domain object - small enough that the api_log payload column shows
 * the whole record (handy when grepping logs in the README walkthrough).
 */
public record Widget(Long id, String name, String sku, BigDecimal price) {
}
