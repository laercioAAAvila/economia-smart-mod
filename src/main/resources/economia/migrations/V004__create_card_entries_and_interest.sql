CREATE TABLE IF NOT EXISTS economy_card_entries (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL REFERENCES economy_cards(id),
    transaction_id UUID NOT NULL REFERENCES economy_transactions(id),
    entry_type VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    remaining_amount BIGINT NOT NULL,
    description VARCHAR(255) NULL,
    merchant_name VARCHAR(128) NULL,
    interest_eligible_date DATE NULL,
    business_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    CONSTRAINT economy_card_entries_type_valid CHECK (entry_type IN ('PURCHASE', 'DAILY_INTEREST', 'PAYMENT', 'REVERSAL', 'ADJUSTMENT')),
    CONSTRAINT economy_card_entries_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT economy_card_entries_remaining_non_negative CHECK (remaining_amount >= 0)
);

CREATE TABLE IF NOT EXISTS economy_interest_accruals (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL REFERENCES economy_cards(id),
    account_id UUID NOT NULL REFERENCES economy_accounts(id),
    accrual_date DATE NOT NULL,
    interest_mode VARCHAR(32) NOT NULL,
    rate_bps INTEGER NOT NULL,
    calculation_base BIGINT NOT NULL,
    remainder_before BIGINT NOT NULL,
    interest_amount BIGINT NOT NULL,
    remainder_after BIGINT NOT NULL,
    transaction_id UUID NULL REFERENCES economy_transactions(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_interest_card_date_unique UNIQUE (card_id, accrual_date),
    CONSTRAINT economy_interest_mode_valid CHECK (interest_mode IN ('SIMPLE', 'COMPOUND')),
    CONSTRAINT economy_interest_rate_non_negative CHECK (rate_bps >= 0),
    CONSTRAINT economy_interest_base_non_negative CHECK (calculation_base >= 0),
    CONSTRAINT economy_interest_remainder_before_non_negative CHECK (remainder_before >= 0),
    CONSTRAINT economy_interest_amount_non_negative CHECK (interest_amount >= 0),
    CONSTRAINT economy_interest_remainder_after_non_negative CHECK (remainder_after >= 0)
);

CREATE INDEX IF NOT EXISTS economy_card_entries_card_date_idx
    ON economy_card_entries(card_id, business_date);

CREATE INDEX IF NOT EXISTS economy_interest_account_id_idx
    ON economy_interest_accruals(account_id);

