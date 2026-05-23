-- Seed 137 rows so the pagination examples in the README produce realistic
-- multi-page output (137 rows / size=20 = 7 pages, matching the README sample).
DELETE FROM reports;

INSERT INTO reports (id, title, created_at)
SELECT
    X                                                   AS id,
    'Report #' || X                                     AS title,
    DATEADD('DAY', -1 * (138 - X), CURRENT_TIMESTAMP)   AS created_at
FROM SYSTEM_RANGE(1, 137);
