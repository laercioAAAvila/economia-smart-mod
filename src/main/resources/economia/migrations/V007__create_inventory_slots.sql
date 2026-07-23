CREATE TABLE IF NOT EXISTS economy_inventory_slots (
    id UUID PRIMARY KEY,
    commercial_block_id UUID NOT NULL REFERENCES economy_commercial_blocks(id),
    inventory_type VARCHAR(32) NOT NULL,
    slot_index INTEGER NOT NULL,
    item_id VARCHAR(255) NULL,
    item_count INTEGER NOT NULL,
    item_components TEXT NULL,
    item_data_version INTEGER NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT economy_inventory_slot_unique UNIQUE (commercial_block_id, inventory_type, slot_index),
    CONSTRAINT economy_inventory_type_valid CHECK (inventory_type IN ('PRODUCT_STOCK', 'CASH_RESERVE', 'PURCHASED_ITEMS', 'BANK_STOCK', 'GOLD_RESERVE')),
    CONSTRAINT economy_inventory_slot_non_negative CHECK (slot_index >= 0),
    CONSTRAINT economy_inventory_count_non_negative CHECK (item_count >= 0),
    CONSTRAINT economy_inventory_count_stack_limit CHECK (item_count <= 64),
    CONSTRAINT economy_inventory_version_positive CHECK (version > 0)
);

CREATE INDEX IF NOT EXISTS economy_inventory_block_type_idx
    ON economy_inventory_slots(commercial_block_id, inventory_type);

