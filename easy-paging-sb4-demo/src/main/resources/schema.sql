CREATE TABLE IF NOT EXISTS reports (
    id         BIGINT       PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    created_at TIMESTAMP    NOT NULL
);
