CREATE TABLE IF NOT EXISTS economy_gold_exchange_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES economy_transactions(id),
    player_uuid UUID NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    gold_item_id VARCHAR(255) NOT NULL,
    gold_item_count BIGINT NOT NULL,
    gold_nugget_units BIGINT NOT NULL,
    unit_value BIGINT NOT NULL,
    money_amount BIGINT NOT NULL,
    commercial_block_id UUID NOT NULL REFERENCES economy_commercial_blocks(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_gold_exchange_type_valid CHECK (operation_type IN ('MINT', 'REDEMPTION', 'ADMIN_ADJUSTMENT')),
    CONSTRAINT economy_gold_exchange_item_count_positive CHECK (gold_item_count > 0),
    CONSTRAINT economy_gold_exchange_units_positive CHECK (gold_nugget_units > 0),
    CONSTRAINT economy_gold_exchange_unit_value_positive CHECK (unit_value > 0),
    CONSTRAINT economy_gold_exchange_money_non_negative CHECK (money_amount >= 0)
);

CREATE TABLE IF NOT EXISTS economy_gold_reserve_summary (
    id UUID PRIMARY KEY,
    reserve_code VARCHAR(64) NOT NULL UNIQUE,
    gold_nugget_units BIGINT NOT NULL,
    currency_issued BIGINT NOT NULL,
    currency_redeemed BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT economy_gold_reserve_units_non_negative CHECK (gold_nugget_units >= 0),
    CONSTRAINT economy_gold_reserve_issued_non_negative CHECK (currency_issued >= 0),
    CONSTRAINT economy_gold_reserve_redeemed_non_negative CHECK (currency_redeemed >= 0),
    CONSTRAINT economy_gold_reserve_version_positive CHECK (version > 0)
);

CREATE INDEX IF NOT EXISTS economy_gold_exchange_transaction_idx
    ON economy_gold_exchange_entries(transaction_id);

CREATE INDEX IF NOT EXISTS economy_gold_exchange_player_idx
    ON economy_gold_exchange_entries(player_uuid);

