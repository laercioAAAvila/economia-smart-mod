CREATE TABLE IF NOT EXISTS economy_operations (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    player_uuid UUID NULL,
    commercial_block_id UUID NULL REFERENCES economy_commercial_blocks(id),
    state VARCHAR(32) NOT NULL,
    payload TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    CONSTRAINT economy_operations_idempotency_unique UNIQUE (idempotency_key),
    CONSTRAINT economy_operations_state_valid CHECK (state IN (
        'CREATED',
        'ITEMS_RESERVED',
        'SQL_COMMITTED',
        'ITEMS_DELIVERED',
        'COMPLETED',
        'ROLLBACK_REQUIRED',
        'ROLLED_BACK'
    ))
);

CREATE INDEX IF NOT EXISTS economy_operations_state_idx
    ON economy_operations(state);

CREATE INDEX IF NOT EXISTS economy_operations_player_idx
    ON economy_operations(player_uuid);

