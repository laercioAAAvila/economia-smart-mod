CREATE TABLE IF NOT EXISTS economy_transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    initiator_player_uuid UUID NULL,
    source_account_id UUID NULL REFERENCES economy_accounts(id),
    destination_account_id UUID NULL REFERENCES economy_accounts(id),
    card_id UUID NULL REFERENCES economy_cards(id),
    commercial_block_id UUID NULL,
    dimension VARCHAR(255) NULL,
    block_x INTEGER NULL,
    block_y INTEGER NULL,
    block_z INTEGER NULL,
    failure_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    CONSTRAINT economy_transactions_idempotency_unique UNIQUE (idempotency_key),
    CONSTRAINT economy_transactions_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT economy_transactions_status_valid CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REVERSED'))
);

CREATE TABLE IF NOT EXISTS economy_ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES economy_transactions(id),
    account_id UUID NOT NULL REFERENCES economy_accounts(id),
    entry_type VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_ledger_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT economy_ledger_balance_before_non_negative CHECK (balance_before >= 0),
    CONSTRAINT economy_ledger_balance_after_non_negative CHECK (balance_after >= 0),
    CONSTRAINT economy_ledger_type_valid CHECK (entry_type IN (
        'DEBIT',
        'CREDIT',
        'CREDIT_PRINCIPAL_INCREASE',
        'CREDIT_INTEREST_INCREASE',
        'CREDIT_DEBT_PAYMENT',
        'CURRENCY_ISSUANCE',
        'CURRENCY_REDEMPTION',
        'ADJUSTMENT'
    ))
);

CREATE INDEX IF NOT EXISTS economy_ledger_transaction_id_idx
    ON economy_ledger_entries(transaction_id);

CREATE INDEX IF NOT EXISTS economy_ledger_account_id_idx
    ON economy_ledger_entries(account_id);

