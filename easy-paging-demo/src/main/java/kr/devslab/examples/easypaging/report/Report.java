package kr.devslab.examples.easypaging.report;

import java.time.Instant;

/**
 * Simple report row stored in the in-memory H2 database.
 *
 * <p>MyBatis hydrates instances of this class from the {@code reports} table; the
 * fields here line up with the column names in {@code schema.sql} via MyBatis's
 * {@code map-underscore-to-camel-case} setting (so {@code created_at} → {@link #createdAt}).
 */
public class Report {

    private Long id;
    private String title;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
