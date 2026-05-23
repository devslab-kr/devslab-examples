-- Seed 300 location pings for one fixed worker UUID so the demo's curl examples
-- always have something to scroll through. Timestamps step back 1 minute per row,
-- so id=1 is the oldest and id=300 is the most recent (ORDER BY time DESC starts
-- with id=300).
--
-- A real worker stream would be millions of rows — 300 is enough to walk the
-- cursor flow a few times and see hasNext flip from true to false on the last page.

DELETE FROM locations;

INSERT INTO locations (id, worker_id, time, lat, lng)
SELECT
    X                                              AS id,
    '00000000-0000-0000-0000-000000000001'         AS worker_id,
    DATEADD('MINUTE', -1 * (301 - X), CURRENT_TIMESTAMP) AS time,
    -- A drifting walk roughly around Seoul (37.5665, 126.9780), nothing
    -- meaningful — just so the lat/lng aren't all identical.
    37.5665 + (X * 0.0001)                         AS lat,
    126.9780 + (X * 0.0001)                        AS lng
FROM SYSTEM_RANGE(1, 300);
