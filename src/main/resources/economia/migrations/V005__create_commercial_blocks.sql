CREATE TABLE IF NOT EXISTS economy_commercial_blocks (
    id UUID PRIMARY KEY,
    block_type VARCHAR(32) NOT NULL,
    owner_player_uuid UUID NULL,
    linked_account_id UUID NULL REFERENCES economy_accounts(id),
    funding_card_id UUID NULL REFERENCES economy_cards(id),
    placed_by_player_uuid UUID NOT NULL,
    dimension VARCHAR(255) NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    custom_name VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    removed_at TIMESTAMP NULL,
    CONSTRAINT economy_commercial_blocks_type_valid CHECK (block_type IN ('ATM', 'SELL_SHOP', 'BUY_SHOP', 'BANK_COUNTER')),
    CONSTRAINT economy_commercial_blocks_status_valid CHECK (status IN ('ACTIVE', 'REMOVED', 'BLOCKED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS economy_commercial_blocks_active_position_unique
    ON economy_commercial_blocks(dimension, block_x, block_y, block_z)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS economy_commercial_blocks_owner_idx
    ON economy_commercial_blocks(owner_player_uuid);

