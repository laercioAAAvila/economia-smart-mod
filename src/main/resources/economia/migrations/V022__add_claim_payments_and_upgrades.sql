CREATE TABLE IF NOT EXISTS economy_claim_direct_payments (
    id UUID PRIMARY KEY,
    anchor_id UUID NOT NULL UNIQUE REFERENCES economy_claim_anchors(id) ON DELETE CASCADE,
    payer_player_uuid UUID NOT NULL,
    amount BIGINT NOT NULL,
    payment_method VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    CONSTRAINT economy_claim_direct_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT economy_claim_direct_payments_method_valid CHECK (payment_method IN ('CASH', 'DEBIT', 'CREDIT')),
    CONSTRAINT economy_claim_direct_payments_status_valid CHECK (status IN ('PENDING', 'PAID', 'COMPLETED'))
);

CREATE INDEX IF NOT EXISTS economy_claim_direct_payments_status_idx
    ON economy_claim_direct_payments(status, created_at);

CREATE TABLE IF NOT EXISTS economy_claim_limit_upgrades (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES economy_groups(id) ON DELETE CASCADE,
    buyer_player_uuid UUID NOT NULL,
    from_limit INTEGER NOT NULL,
    to_limit INTEGER NOT NULL,
    percentage_basis_points INTEGER NOT NULL,
    amount BIGINT NOT NULL,
    payment_method VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    CONSTRAINT economy_claim_limit_upgrades_limits_valid CHECK (from_limit > 0 AND to_limit = from_limit + 1),
    CONSTRAINT economy_claim_limit_upgrades_percentage_valid CHECK (percentage_basis_points >= 0),
    CONSTRAINT economy_claim_limit_upgrades_amount_positive CHECK (amount > 0),
    CONSTRAINT economy_claim_limit_upgrades_method_valid CHECK (payment_method IN ('CASH', 'DEBIT', 'CREDIT')),
    CONSTRAINT economy_claim_limit_upgrades_status_valid CHECK (status IN ('PENDING', 'COMPLETED')),
    CONSTRAINT economy_claim_limit_upgrades_level_unique UNIQUE (group_id, to_limit)
);

CREATE INDEX IF NOT EXISTS economy_claim_limit_upgrades_status_idx
    ON economy_claim_limit_upgrades(group_id, status, created_at);
