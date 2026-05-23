-- DROP-then-CREATE so every restart leaves us in a known state. Real apps
-- should use Flyway (it supports R2DBC migrations via flyway-r2dbc) or
-- Liquibase instead — see the README.
DROP TABLE IF EXISTS articles;

CREATE TABLE articles (
    id            BIGSERIAL    PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    author        VARCHAR(100) NOT NULL,
    published_at  TIMESTAMP    NOT NULL,
    view_count    BIGINT       NOT NULL
);

-- Indexes that match the most common filter/sort combos.
CREATE INDEX idx_articles_published_at ON articles(published_at DESC);
CREATE INDEX idx_articles_author       ON articles(author);
