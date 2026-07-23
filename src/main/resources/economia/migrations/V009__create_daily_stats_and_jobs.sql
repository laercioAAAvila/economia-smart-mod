CREATE TABLE IF NOT EXISTS economy_offer_daily_stats (
    id UUID PRIMARY KEY,
    offer_id UUID NOT NULL REFERENCES economy_shop_offers(id),
    business_date DATE NOT NULL,
    quantity_bought_from_bank BIGINT NOT NULL,
    quantity_sold_to_bank BIGINT NOT NULL,
    money_received_by_bank BIGINT NOT NULL,
    money_paid_by_bank BIGINT NOT NULL,
    highest_demand_level INTEGER NOT NULL,
    highest_supply_level INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_offer_daily_stats_unique UNIQUE (offer_id, business_date),
    CONSTRAINT economy_offer_daily_bought_non_negative CHECK (quantity_bought_from_bank >= 0),
    CONSTRAINT economy_offer_daily_sold_non_negative CHECK (quantity_sold_to_bank >= 0),
    CONSTRAINT economy_offer_daily_received_non_negative CHECK (money_received_by_bank >= 0),
    CONSTRAINT economy_offer_daily_paid_non_negative CHECK (money_paid_by_bank >= 0),
    CONSTRAINT economy_offer_daily_demand_non_negative CHECK (highest_demand_level >= 0),
    CONSTRAINT economy_offer_daily_supply_non_negative CHECK (highest_supply_level >= 0)
);

CREATE TABLE IF NOT EXISTS economy_daily_job_runs (
    id UUID PRIMARY KEY,
    job_type VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    failure_reason VARCHAR(255) NULL,
    CONSTRAINT economy_daily_job_runs_unique UNIQUE (job_type, business_date),
    CONSTRAINT economy_daily_job_runs_type_valid CHECK (job_type IN ('CREDIT_INTEREST', 'DYNAMIC_PRICE_RECOVERY', 'DAILY_GOLD_LIMIT_RESET')),
    CONSTRAINT economy_daily_job_runs_status_valid CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

