CREATE TABLE IF NOT EXISTS economy_accounts (
    id UUID PRIMARY KEY,
    player_uuid UUID NULL,
    username VARCHAR(64) NULL,
    username_normalized VARCHAR(64) NULL,
    password_hash VARCHAR(255) NULL,
    password_salt VARCHAR(255) NULL,
    password_algorithm VARCHAR(64) NULL,
    account_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    balance BIGINT NOT NULL,
    configured_credit_limit BIGINT NOT NULL,
    credit_principal_outstanding BIGINT NOT NULL,
    credit_interest_outstanding BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP NULL,
    version BIGINT NOT NULL,
    CONSTRAINT economy_accounts_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT economy_accounts_credit_limit_non_negative CHECK (configured_credit_limit >= 0),
    CONSTRAINT economy_accounts_principal_non_negative CHECK (credit_principal_outstanding >= 0),
    CONSTRAINT economy_accounts_interest_non_negative CHECK (credit_interest_outstanding >= 0),
    CONSTRAINT economy_accounts_type_valid CHECK (account_type IN ('PLAYER', 'SYSTEM_TREASURY', 'SYSTEM_CASH', 'SYSTEM_CURRENCY_ISSUANCE')),
    CONSTRAINT economy_accounts_status_valid CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS economy_accounts_player_uuid_unique
    ON economy_accounts(player_uuid)
    WHERE account_type = 'PLAYER' AND player_uuid IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS economy_accounts_username_normalized_unique
    ON economy_accounts(username_normalized)
    WHERE account_type = 'PLAYER' AND username_normalized IS NOT NULL;

