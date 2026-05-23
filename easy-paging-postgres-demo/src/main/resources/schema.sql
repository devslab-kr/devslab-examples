-- DROP-then-CREATE so spring.sql.init re-applying on every startup leaves us
-- in a known state without needing migration tooling for the demo. A real app
-- would use Flyway or Liquibase here — see the README.
DROP TABLE IF EXISTS products;

CREATE TABLE products (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(200)    NOT NULL,
    price       NUMERIC(10, 2)  NOT NULL,
    category    VARCHAR(50)     NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- A composite index that matches the most common filter+sort combo. Not
-- required for correctness, but lets curious readers EXPLAIN ANALYZE the
-- paginated queries and see the planner pick this index.
CREATE INDEX idx_products_category_created_at
    ON products(category, created_at DESC);
