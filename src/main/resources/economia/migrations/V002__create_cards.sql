CREATE TABLE IF NOT EXISTS economy_cards (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES economy_accounts(id),
    card_type VARCHAR(32) NOT NULL,
    custom_name VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL,
    individual_credit_limit BIGINT NOT NULL,
    credit_principal_outstanding BIGINT NOT NULL,
    credit_interest_outstanding BIGINT NOT NULL,
    interest_rounding_remainder BIGINT NOT NULL,
    security_version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    disabled_at TIMESTAMP NULL,
    CONSTRAINT economy_cards_type_valid CHECK (card_type IN ('DEBIT', 'CREDIT', 'DEBIT_CREDIT')),
    CONSTRAINT economy_cards_status_valid CHECK (status IN ('ACTIVE', 'DISABLED', 'BLOCKED', 'EXPIRED')),
    CONSTRAINT economy_cards_limit_non_negative CHECK (individual_credit_limit >= 0),
    CONSTRAINT economy_cards_principal_non_negative CHECK (credit_principal_outstanding >= 0),
    CONSTRAINT economy_cards_interest_non_negative CHECK (credit_interest_outstanding >= 0),
    CONSTRAINT economy_cards_remainder_non_negative CHECK (interest_rounding_remainder >= 0),
    CONSTRAINT economy_cards_security_version_positive CHECK (security_version > 0)
);

CREATE INDEX IF NOT EXISTS economy_cards_account_id_idx
    ON economy_cards(account_id);

