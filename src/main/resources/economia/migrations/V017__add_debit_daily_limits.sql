ALTER TABLE economy_cards
    ADD COLUMN IF NOT EXISTS debit_daily_limit BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS debit_daily_spent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS debit_daily_spent_on DATE NULL;

ALTER TABLE economy_cards
    ADD CONSTRAINT economy_cards_debit_daily_limit_non_negative CHECK (debit_daily_limit >= 0),
    ADD CONSTRAINT economy_cards_debit_daily_spent_non_negative CHECK (debit_daily_spent >= 0);
