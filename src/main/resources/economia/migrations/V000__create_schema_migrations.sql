CREATE TABLE IF NOT EXISTS economy_schema_migrations (
    version INTEGER PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    applied_at TIMESTAMP NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    CONSTRAINT economy_schema_migrations_execution_time_non_negative CHECK (execution_time_ms >= 0)
);

