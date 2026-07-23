CREATE INDEX IF NOT EXISTS economy_operations_state_updated_idx
    ON economy_operations(state, updated_at);
