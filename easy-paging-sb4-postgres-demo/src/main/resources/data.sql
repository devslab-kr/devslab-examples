-- Seed 500 products distributed evenly across 5 categories (100 each), with
-- timestamps stepping back 1 hour per row so sort=createdAt,desc produces a
-- realistic stream. Prices are deterministic (id-based) so tests can assert
-- against them without flakiness — random() would be more realistic but would
-- make snapshot-style assertions impossible.

INSERT INTO products (name, price, category, created_at)
SELECT
    'Product #' || i,
    -- 10.00 .. 999.99, varies smoothly with id so the listing isn't monotone
    ROUND((10 + ((i * 7) % 990) + 0.99)::numeric, 2),
    (ARRAY['electronics', 'books', 'clothing', 'home', 'sports'])[1 + ((i - 1) % 5)],
    NOW() - (i || ' hours')::interval
FROM generate_series(1, 500) AS i;
