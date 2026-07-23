CREATE TABLE IF NOT EXISTS economy_audit_logs (
    id UUID PRIMARY KEY,
    actor_player_uuid UUID NULL,
    actor_type VARCHAR(32) NOT NULL,
    action VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id UUID NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    dimension VARCHAR(255) NULL,
    block_x INTEGER NULL,
    block_y INTEGER NULL,
    block_z INTEGER NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_audit_actor_type_valid CHECK (actor_type IN ('PLAYER', 'ADMIN', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS economy_audit_actor_idx
    ON economy_audit_logs(actor_player_uuid);

CREATE INDEX IF NOT EXISTS economy_audit_target_idx
    ON economy_audit_logs(target_type, target_id);

