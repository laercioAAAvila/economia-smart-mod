ALTER TABLE economy_commercial_blocks
    ADD COLUMN IF NOT EXISTS owner_name VARCHAR(64) NULL;

ALTER TABLE economy_commercial_blocks
    ADD COLUMN IF NOT EXISTS owner_account_number VARCHAR(16) NULL;

ALTER TABLE economy_commercial_blocks
    DROP CONSTRAINT IF EXISTS economy_commercial_blocks_type_valid;

ALTER TABLE economy_commercial_blocks
    ADD CONSTRAINT economy_commercial_blocks_type_valid
        CHECK (block_type IN ('ATM', 'SELL_SHOP', 'BUY_SHOP', 'BANK_COUNTER', 'MAIL'));

ALTER TABLE economy_inventory_slots
    DROP CONSTRAINT IF EXISTS economy_inventory_type_valid;

ALTER TABLE economy_inventory_slots
    ADD CONSTRAINT economy_inventory_type_valid
        CHECK (inventory_type IN ('PRODUCT_STOCK', 'CASH_RESERVE', 'PURCHASED_ITEMS', 'BANK_STOCK', 'GOLD_RESERVE', 'MAIL_RECEIVED'));

CREATE TABLE IF NOT EXISTS economy_mail_recipients (
    id UUID PRIMARY KEY,
    origin_block_id UUID NOT NULL REFERENCES economy_commercial_blocks(id),
    destination_block_id UUID NOT NULL REFERENCES economy_commercial_blocks(id),
    added_by_player_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_mail_recipient_unique UNIQUE (origin_block_id, destination_block_id)
);

CREATE INDEX IF NOT EXISTS economy_mail_recipients_origin_idx
    ON economy_mail_recipients(origin_block_id);

CREATE INDEX IF NOT EXISTS economy_mail_recipients_destination_idx
    ON economy_mail_recipients(destination_block_id);
