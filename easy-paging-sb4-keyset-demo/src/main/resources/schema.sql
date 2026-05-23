CREATE TABLE IF NOT EXISTS locations (
    id         BIGINT           PRIMARY KEY,
    worker_id  UUID             NOT NULL,
    time       TIMESTAMP        NOT NULL,
    lat        DOUBLE PRECISION NOT NULL,
    lng        DOUBLE PRECISION NOT NULL
);

-- Covering index for the keyset query: WHERE worker_id = ? ORDER BY time DESC, id DESC.
-- A real PostgreSQL/MySQL deployment wants this for cursor pagination to stay O(log N)
-- per page instead of degrading like OFFSET would.
CREATE INDEX IF NOT EXISTS idx_locations_worker_time_id
    ON locations(worker_id, time DESC, id DESC);
