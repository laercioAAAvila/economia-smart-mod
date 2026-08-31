CREATE TABLE IF NOT EXISTS economy_schema_migrations (
    version INTEGER PRIMARY KEY,
    description TEXT NOT NULL,
    checksum TEXT NOT NULL,
    applied_at TIMESTAMP NOT NULL,
    execution_time_ms INTEGER NOT NULL CHECK (execution_time_ms >= 0)
);
