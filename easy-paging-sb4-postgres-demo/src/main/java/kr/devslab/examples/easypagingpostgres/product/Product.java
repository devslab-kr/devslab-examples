package kr.devslab.examples.easypagingpostgres.product;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Catalog product — the classic "give me paginated listings, optionally
 * filtered by category" use case that paginated REST APIs are usually written
 * for. The schema and seed data live in
 * {@code resources/{schema,data}.sql}; both are PostgreSQL-flavoured
 * (BIGSERIAL, NUMERIC, generate_series).
 */
public class Product {

    private Long id;
    private String name;
    private BigDecimal price;
    private String category;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
