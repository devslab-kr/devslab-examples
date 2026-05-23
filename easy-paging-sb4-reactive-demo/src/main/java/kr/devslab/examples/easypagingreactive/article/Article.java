package kr.devslab.examples.easypagingreactive.article;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data Relational entity (used by R2DBC under the hood). The
 * {@code @Column("published_at")} mapping is needed because Spring Data
 * Relational's default {@code NamingStrategy} doesn't convert camelCase →
 * snake_case automatically — fields that already match the column name
 * (id, title, author) can stay un-annotated.
 */
@Table("articles")
public class Article {

    @Id
    private Long id;
    private String title;
    private String author;

    @Column("published_at")
    private Instant publishedAt;

    @Column("view_count")
    private Long viewCount;

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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
}
