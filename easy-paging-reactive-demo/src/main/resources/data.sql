-- 500 articles distributed evenly across 5 authors (100 each). Timestamps step
-- back 1 hour per row so sort=publishedAt,desc produces a realistic stream.
-- view_count is deterministic so tests can assert against it without
-- flakiness (random() would be more realistic but unstable).

INSERT INTO articles (title, author, published_at, view_count)
SELECT
    'Article #' || i,
    (ARRAY['alice', 'bob', 'charlie', 'dana', 'eve'])[1 + ((i - 1) % 5)],
    NOW() - (i || ' hours')::interval,
    ((i * 13) % 1000)::bigint
FROM generate_series(1, 500) AS i;
