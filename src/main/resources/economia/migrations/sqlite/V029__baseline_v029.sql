
CREATE TABLE IF NOT EXISTS economy_accounts (
    id TEXT PRIMARY KEY,
    player_uuid TEXT NULL,
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
    account_number VARCHAR(6) NULL,
    minecraft_player_name VARCHAR(64) NULL,
    server_uuid TEXT NULL,
    opening_fee BIGINT NOT NULL DEFAULT 0,
    opening_request_id TEXT NULL,
    CONSTRAINT economy_accounts_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT economy_accounts_credit_limit_non_negative CHECK (configured_credit_limit >= 0),
    CONSTRAINT economy_accounts_principal_non_negative CHECK (credit_principal_outstanding >= 0),
    CONSTRAINT economy_accounts_interest_non_negative CHECK (credit_interest_outstanding >= 0),
    CONSTRAINT economy_accounts_type_valid CHECK (account_type IN ('PLAYER','SYSTEM_TREASURY','SYSTEM_CASH','SYSTEM_CURRENCY_ISSUANCE','CLAN_TREASURY','CLAN_SUPPORT','PRIVATE_PROPERTY')),
    CONSTRAINT economy_accounts_status_valid CHECK (status IN ('PENDING','ACTIVE','BLOCKED','CLOSED')),
    CONSTRAINT economy_accounts_opening_fee_non_negative CHECK (opening_fee >= 0)
);

CREATE TABLE IF NOT EXISTS economy_cards (
    id TEXT PRIMARY KEY,
    account_id TEXT NOT NULL REFERENCES economy_accounts(id),
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
    debit_daily_limit BIGINT NOT NULL DEFAULT 0,
    debit_daily_spent BIGINT NOT NULL DEFAULT 0,
    debit_daily_spent_on DATE NULL,
    card_creation_number INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT economy_cards_type_valid CHECK (card_type IN ('DEBIT', 'CREDIT', 'DEBIT_CREDIT')),
    CONSTRAINT economy_cards_status_valid CHECK (status IN ('ACTIVE', 'DISABLED', 'BLOCKED', 'EXPIRED')),
    CONSTRAINT economy_cards_limit_non_negative CHECK (individual_credit_limit >= 0),
    CONSTRAINT economy_cards_principal_non_negative CHECK (credit_principal_outstanding >= 0),
    CONSTRAINT economy_cards_interest_non_negative CHECK (credit_interest_outstanding >= 0),
    CONSTRAINT economy_cards_remainder_non_negative CHECK (interest_rounding_remainder >= 0),
    CONSTRAINT economy_cards_security_version_positive CHECK (security_version > 0),
    CONSTRAINT economy_cards_debit_daily_limit_non_negative CHECK (debit_daily_limit >= 0),
    CONSTRAINT economy_cards_debit_daily_spent_non_negative CHECK (debit_daily_spent >= 0),
    CONSTRAINT economy_cards_creation_number_non_negative CHECK (card_creation_number >= 0)
);

CREATE TABLE IF NOT EXISTS economy_transactions (
    id TEXT PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    initiator_player_uuid TEXT NULL,
    source_account_id TEXT NULL REFERENCES economy_accounts(id),
    destination_account_id TEXT NULL REFERENCES economy_accounts(id),
    card_id TEXT NULL REFERENCES economy_cards(id),
    commercial_block_id TEXT NULL,
    dimension VARCHAR(255) NULL,
    block_x INTEGER NULL,
    block_y INTEGER NULL,
    block_z INTEGER NULL,
    failure_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    request_fingerprint VARCHAR(64) NULL,
    origin VARCHAR(16) NOT NULL DEFAULT 'MINECRAFT',
    CONSTRAINT economy_transactions_idempotency_unique UNIQUE (idempotency_key),
    CONSTRAINT economy_transactions_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT economy_transactions_status_valid CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REVERSED')),
    CONSTRAINT economy_transactions_origin_valid CHECK (origin IN ('MINECRAFT','WEB','ADMIN','SYSTEM'))
);

CREATE TABLE IF NOT EXISTS economy_ledger_entries (
    id TEXT PRIMARY KEY,
    transaction_id TEXT NOT NULL REFERENCES economy_transactions(id),
    account_id TEXT NOT NULL REFERENCES economy_accounts(id),
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

CREATE TABLE IF NOT EXISTS economy_card_entries (
    id TEXT PRIMARY KEY,
    card_id TEXT NOT NULL REFERENCES economy_cards(id),
    transaction_id TEXT NOT NULL REFERENCES economy_transactions(id),
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
    id TEXT PRIMARY KEY,
    card_id TEXT NOT NULL REFERENCES economy_cards(id),
    account_id TEXT NOT NULL REFERENCES economy_accounts(id),
    accrual_date DATE NOT NULL,
    interest_mode VARCHAR(32) NOT NULL,
    rate_bps INTEGER NOT NULL,
    calculation_base BIGINT NOT NULL,
    remainder_before BIGINT NOT NULL,
    interest_amount BIGINT NOT NULL,
    remainder_after BIGINT NOT NULL,
    transaction_id TEXT NULL REFERENCES economy_transactions(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_interest_card_date_unique UNIQUE (card_id, accrual_date),
    CONSTRAINT economy_interest_mode_valid CHECK (interest_mode IN ('SIMPLE', 'COMPOUND')),
    CONSTRAINT economy_interest_rate_non_negative CHECK (rate_bps >= 0),
    CONSTRAINT economy_interest_base_non_negative CHECK (calculation_base >= 0),
    CONSTRAINT economy_interest_remainder_before_non_negative CHECK (remainder_before >= 0),
    CONSTRAINT economy_interest_amount_non_negative CHECK (interest_amount >= 0),
    CONSTRAINT economy_interest_remainder_after_non_negative CHECK (remainder_after >= 0)
);

CREATE TABLE IF NOT EXISTS economy_commercial_blocks (
    id TEXT PRIMARY KEY,
    block_type VARCHAR(32) NOT NULL,
    owner_player_uuid TEXT NULL,
    linked_account_id TEXT NULL REFERENCES economy_accounts(id),
    funding_card_id TEXT NULL REFERENCES economy_cards(id),
    placed_by_player_uuid TEXT NOT NULL,
    dimension VARCHAR(255) NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    custom_name VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    removed_at TIMESTAMP NULL,
    owner_name VARCHAR(64) NULL,
    owner_account_number VARCHAR(16) NULL,
    server_uuid TEXT NULL,
    CONSTRAINT economy_commercial_blocks_type_valid CHECK (block_type IN ('ATM','SELL_SHOP','BUY_SHOP','BANK_COUNTER','MAIL')),
    CONSTRAINT economy_commercial_blocks_status_valid CHECK (status IN ('ACTIVE', 'REMOVED', 'BLOCKED'))
);

CREATE TABLE IF NOT EXISTS economy_shop_offers (
    id TEXT PRIMARY KEY,
    commercial_block_id TEXT NOT NULL REFERENCES economy_commercial_blocks(id),
    slot_index INTEGER NOT NULL,
    item_id VARCHAR(255) NOT NULL,
    item_components TEXT NULL,
    item_data_version INTEGER NULL,
    quantity_per_operation INTEGER NOT NULL,
    base_buy_price BIGINT NULL,
    base_sell_price BIGINT NULL,
    minimum_buy_price BIGINT NULL,
    maximum_sell_price BIGINT NULL,
    target_quantity BIGINT NULL,
    purchased_quantity BIGINT NOT NULL,
    comparison_mode VARCHAR(32) NOT NULL,
    pricing_mode VARCHAR(32) NOT NULL,
    demand_level INTEGER NOT NULL,
    supply_level INTEGER NOT NULL,
    quantity_per_price_level BIGINT NULL,
    demand_increase_bps INTEGER NULL,
    supply_decrease_bps INTEGER NULL,
    recovery_levels_per_idle_day INTEGER NOT NULL,
    maximum_demand_level INTEGER NOT NULL,
    maximum_supply_level INTEGER NOT NULL,
    last_player_purchase_date DATE NULL,
    last_player_sale_date DATE NULL,
    is_buy_enabled BOOLEAN NOT NULL,
    is_sell_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT economy_shop_offers_slot_unique UNIQUE (commercial_block_id, slot_index),
    CONSTRAINT economy_shop_offers_slot_valid CHECK (slot_index >= 0 AND slot_index < 16),
    CONSTRAINT economy_shop_offers_quantity_positive CHECK (quantity_per_operation > 0),
    CONSTRAINT economy_shop_offers_purchased_non_negative CHECK (purchased_quantity >= 0),
    CONSTRAINT economy_shop_offers_demand_non_negative CHECK (demand_level >= 0),
    CONSTRAINT economy_shop_offers_supply_non_negative CHECK (supply_level >= 0),
    CONSTRAINT economy_shop_offers_comparison_valid CHECK (comparison_mode IN ('FULL_COMPONENTS', 'ITEM_ID_ONLY')),
    CONSTRAINT economy_shop_offers_pricing_valid CHECK (pricing_mode IN ('FIXED', 'DYNAMIC', 'MONETARY_GOLD')),
    CONSTRAINT economy_shop_offers_recovery_non_negative CHECK (recovery_levels_per_idle_day >= 0),
    CONSTRAINT economy_shop_offers_max_demand_non_negative CHECK (maximum_demand_level >= 0),
    CONSTRAINT economy_shop_offers_max_supply_non_negative CHECK (maximum_supply_level >= 0),
    CONSTRAINT economy_shop_offers_version_positive CHECK (version > 0)
);

CREATE TABLE IF NOT EXISTS economy_inventory_slots (
    id TEXT PRIMARY KEY,
    commercial_block_id TEXT NOT NULL REFERENCES economy_commercial_blocks(id),
    inventory_type VARCHAR(32) NOT NULL,
    slot_index INTEGER NOT NULL,
    item_id VARCHAR(255) NULL,
    item_count INTEGER NOT NULL,
    item_components TEXT NULL,
    item_data_version INTEGER NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT economy_inventory_slot_unique UNIQUE (commercial_block_id, inventory_type, slot_index),
    CONSTRAINT economy_inventory_type_valid CHECK (inventory_type IN ('PRODUCT_STOCK','CASH_RESERVE','PURCHASED_ITEMS','BANK_STOCK','GOLD_RESERVE','MAIL_RECEIVED')),
    CONSTRAINT economy_inventory_slot_non_negative CHECK (slot_index >= 0),
    CONSTRAINT economy_inventory_count_non_negative CHECK (item_count >= 0),
    CONSTRAINT economy_inventory_count_stack_limit CHECK (item_count <= 64),
    CONSTRAINT economy_inventory_version_positive CHECK (version > 0)
);

CREATE TABLE IF NOT EXISTS economy_gold_exchange_entries (
    id TEXT PRIMARY KEY,
    transaction_id TEXT NOT NULL REFERENCES economy_transactions(id),
    player_uuid TEXT NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    gold_item_id VARCHAR(255) NOT NULL,
    gold_item_count BIGINT NOT NULL,
    gold_nugget_units BIGINT NOT NULL,
    unit_value BIGINT NOT NULL,
    money_amount BIGINT NOT NULL,
    commercial_block_id TEXT NULL REFERENCES economy_commercial_blocks(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_gold_exchange_type_valid CHECK (operation_type IN ('MINT', 'REDEMPTION', 'ADMIN_ADJUSTMENT')),
    CONSTRAINT economy_gold_exchange_item_count_positive CHECK (gold_item_count > 0),
    CONSTRAINT economy_gold_exchange_units_positive CHECK (gold_nugget_units > 0),
    CONSTRAINT economy_gold_exchange_unit_value_positive CHECK (unit_value > 0),
    CONSTRAINT economy_gold_exchange_money_non_negative CHECK (money_amount >= 0)
);

CREATE TABLE IF NOT EXISTS economy_gold_reserve_summary (
    id TEXT PRIMARY KEY,
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

CREATE TABLE IF NOT EXISTS economy_offer_daily_stats (
    id TEXT PRIMARY KEY,
    offer_id TEXT NOT NULL REFERENCES economy_shop_offers(id),
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
    id TEXT PRIMARY KEY,
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

CREATE TABLE IF NOT EXISTS economy_audit_logs (
    id TEXT PRIMARY KEY,
    actor_player_uuid TEXT NULL,
    actor_type VARCHAR(32) NOT NULL,
    action VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id TEXT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    dimension VARCHAR(255) NULL,
    block_x INTEGER NULL,
    block_y INTEGER NULL,
    block_z INTEGER NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_audit_actor_type_valid CHECK (actor_type IN ('PLAYER', 'ADMIN', 'SYSTEM'))
);

CREATE TABLE IF NOT EXISTS economy_operations (
    id TEXT PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    player_uuid TEXT NULL,
    commercial_block_id TEXT NULL REFERENCES economy_commercial_blocks(id),
    state VARCHAR(32) NOT NULL,
    payload TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    request_fingerprint VARCHAR(64) NULL,
    result_payload TEXT NULL,
    CONSTRAINT economy_operations_idempotency_unique UNIQUE (idempotency_key),
    CONSTRAINT economy_operations_state_valid CHECK (state IN (
        'CREATED',
        'ITEMS_RESERVED',
        'SQL_COMMITTED',
        'ITEMS_DELIVERED',
        'COMPLETED',
        'ROLLBACK_REQUIRED',
        'RECONCILIATION_REQUIRED',
        'ROLLED_BACK'
    ))
);

CREATE TABLE IF NOT EXISTS economy_mail_recipients (
    id TEXT PRIMARY KEY,
    origin_block_id TEXT NOT NULL REFERENCES economy_commercial_blocks(id),
    destination_block_id TEXT NOT NULL REFERENCES economy_commercial_blocks(id),
    added_by_player_uuid TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_mail_recipient_unique UNIQUE (origin_block_id, destination_block_id)
);

CREATE TABLE IF NOT EXISTS economy_server_clock (
    id SMALLINT NOT NULL,
    active_millis BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    server_uuid TEXT NULL,
    CONSTRAINT economy_server_clock_singleton CHECK (id = 1),
    CONSTRAINT economy_server_clock_non_negative CHECK (active_millis >= 0)
);

CREATE TABLE IF NOT EXISTS economy_groups (
    id TEXT PRIMARY KEY,
    group_type VARCHAR(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    normalized_name VARCHAR(64) NOT NULL,
    leader_player_uuid TEXT NOT NULL,
    vice_leader_player_uuid TEXT NULL,
    account_id TEXT NOT NULL REFERENCES economy_accounts(id),
    support_account_id TEXT NULL REFERENCES economy_accounts(id),
    claim_limit INTEGER NOT NULL,
    visitor_use_buy_shop BOOLEAN NOT NULL DEFAULT FALSE,
    visitor_use_sell_shop BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP NULL,
    server_uuid TEXT NULL,
    CONSTRAINT economy_groups_type_valid CHECK (group_type IN ('CLAN', 'PRIVATE_PROPERTY')),
    CONSTRAINT economy_groups_status_valid CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT economy_groups_claim_limit_positive CHECK (claim_limit > 0),
    CONSTRAINT economy_groups_id_type_unique UNIQUE (id, group_type)
);

CREATE TABLE IF NOT EXISTS economy_group_members (
    group_id TEXT NOT NULL,
    group_type VARCHAR(16) NOT NULL,
    player_uuid TEXT NOT NULL,
    role VARCHAR(24) NOT NULL,
    permission_mask INTEGER NOT NULL,
    last_active_millis BIGINT NOT NULL DEFAULT 0,
    joined_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    server_uuid TEXT NULL,
    PRIMARY KEY (group_id, player_uuid),
    CONSTRAINT economy_group_members_group_fk
        FOREIGN KEY (group_id, group_type) REFERENCES economy_groups(id, group_type) ON DELETE CASCADE,
    CONSTRAINT economy_group_members_type_valid CHECK (group_type IN ('CLAN', 'PRIVATE_PROPERTY')),
    CONSTRAINT economy_group_members_role_valid CHECK (role IN ('OWNER', 'LEADER', 'VICE_LEADER', 'MEMBER')),
    CONSTRAINT economy_group_members_activity_non_negative CHECK (last_active_millis >= 0)
);

CREATE TABLE IF NOT EXISTS economy_group_invites (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL REFERENCES economy_groups(id) ON DELETE CASCADE,
    group_type VARCHAR(16) NOT NULL,
    invited_player_uuid TEXT NOT NULL,
    invited_by_player_uuid TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP NULL,
    server_uuid TEXT NULL,
    CONSTRAINT economy_group_invites_type_valid CHECK (group_type IN ('CLAN', 'PRIVATE_PROPERTY')),
    CONSTRAINT economy_group_invites_status_valid CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS economy_claim_anchors (
    id TEXT PRIMARY KEY,
    group_id TEXT NULL REFERENCES economy_groups(id) ON DELETE CASCADE,
    group_type VARCHAR(16) NOT NULL,
    dimension VARCHAR(255) NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    placed_by_player_uuid TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    removed_at TIMESTAMP NULL,
    territory_id TEXT NULL,
    server_uuid TEXT NULL
);

CREATE TABLE IF NOT EXISTS economy_claims (
    id TEXT PRIMARY KEY,
    group_id TEXT NULL REFERENCES economy_groups(id) ON DELETE CASCADE,
    group_type VARCHAR(16) NOT NULL,
    dimension VARCHAR(255) NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    created_by_player_uuid TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    territory_id TEXT NULL,
    server_uuid TEXT NULL
);

CREATE TABLE IF NOT EXISTS economy_player_locations (
    id TEXT PRIMARY KEY,
    player_uuid TEXT NOT NULL,
    name VARCHAR(64) NOT NULL,
    dimension VARCHAR(255) NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    server_uuid TEXT NULL
);

CREATE TABLE IF NOT EXISTS economy_claim_territories (
    id TEXT PRIMARY KEY,
    anchor_id TEXT NOT NULL UNIQUE REFERENCES economy_claim_anchors(id) ON DELETE CASCADE,
    claim_type VARCHAR(32) NOT NULL,
    group_id TEXT NULL REFERENCES economy_groups(id),
    owner_player_uuid TEXT NULL,
    land_price BIGINT NOT NULL,
    land_debt BIGINT NOT NULL,
    anchor_paid_until_millis BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    server_uuid TEXT NULL,
    CONSTRAINT economy_claim_territories_type_valid CHECK (claim_type IN ('CLAN', 'PRIVATE_PROPERTY')),
    CONSTRAINT economy_claim_territories_price_non_negative CHECK (land_price >= 0 AND land_debt >= 0),
    CONSTRAINT economy_claim_territories_anchor_time_non_negative CHECK (anchor_paid_until_millis >= 0),
    CONSTRAINT economy_claim_territories_owner_valid CHECK (
        (claim_type = 'CLAN' AND group_id IS NOT NULL) OR
        (claim_type = 'PRIVATE_PROPERTY' AND group_id IS NOT NULL AND owner_player_uuid IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS economy_private_property_members (
    territory_id TEXT NOT NULL REFERENCES economy_claim_territories(id) ON DELETE CASCADE,
    player_uuid TEXT NOT NULL,
    invited_by_player_uuid TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    permission_mask INTEGER NOT NULL DEFAULT 1,
    player_name VARCHAR(16) NULL,
    PRIMARY KEY (territory_id, player_uuid),
    CONSTRAINT economy_private_property_members_permission_mask_valid CHECK (permission_mask >= 0 AND permission_mask <= 7)
);

CREATE TABLE IF NOT EXISTS economy_claim_invoices (
    id TEXT PRIMARY KEY,
    territory_id TEXT NOT NULL REFERENCES economy_claim_territories(id) ON DELETE CASCADE,
    invoice_type VARCHAR(24) NOT NULL,
    debtor_player_uuid TEXT NOT NULL,
    issuer_player_uuid TEXT NOT NULL,
    seller_player_uuid TEXT NULL,
    seller_account_id TEXT NULL REFERENCES economy_accounts(id),
    buyer_group_id TEXT NULL REFERENCES economy_groups(id),
    amount BIGINT NOT NULL,
    minecraft_days INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    CONSTRAINT economy_claim_invoices_type_valid CHECK (invoice_type IN ('LAND','ANCHOR','SALE','BUNDLE')),
    CONSTRAINT economy_claim_invoices_status_valid CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT economy_claim_invoices_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT economy_claim_invoices_days_non_negative CHECK (minecraft_days >= 0)
);

CREATE TABLE IF NOT EXISTS economy_claim_direct_payments (
    id TEXT PRIMARY KEY,
    anchor_id TEXT NOT NULL UNIQUE REFERENCES economy_claim_anchors(id) ON DELETE CASCADE,
    payer_player_uuid TEXT NOT NULL,
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

CREATE TABLE IF NOT EXISTS economy_claim_limit_upgrades (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL REFERENCES economy_groups(id) ON DELETE CASCADE,
    buyer_player_uuid TEXT NOT NULL,
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

CREATE TABLE IF NOT EXISTS economy_claim_invoice_bundle_items (
    bundle_invoice_id TEXT NOT NULL REFERENCES economy_claim_invoices(id) ON DELETE CASCADE,
    child_invoice_id TEXT NOT NULL UNIQUE REFERENCES economy_claim_invoices(id),
    PRIMARY KEY (bundle_invoice_id, child_invoice_id)
);


CREATE UNIQUE INDEX IF NOT EXISTS economy_accounts_server_username_unique ON economy_accounts(server_uuid, username_normalized) WHERE account_type = 'PLAYER' AND server_uuid IS NOT NULL AND username_normalized IS NOT NULL AND status <> 'CLOSED';
CREATE INDEX IF NOT EXISTS economy_accounts_server_player_idx ON economy_accounts(server_uuid, player_uuid, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS economy_accounts_opening_request_unique ON economy_accounts(opening_request_id) WHERE opening_request_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS economy_accounts_account_number_unique ON economy_accounts(account_number) WHERE account_type = 'PLAYER' AND account_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_accounts_minecraft_player_name_idx ON economy_accounts(minecraft_player_name) WHERE account_type = 'PLAYER' AND minecraft_player_name IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_cards_account_id_idx ON economy_cards(account_id);
CREATE UNIQUE INDEX IF NOT EXISTS economy_cards_account_creation_number_idx ON economy_cards(account_id, card_creation_number) WHERE card_creation_number > 0;
CREATE INDEX IF NOT EXISTS economy_cards_credit_debt_candidates_idx ON economy_cards(account_id, id) WHERE card_type IN ('CREDIT','DEBIT_CREDIT') AND status IN ('ACTIVE','DISABLED') AND (credit_principal_outstanding + credit_interest_outstanding) > 0;
CREATE INDEX IF NOT EXISTS economy_ledger_transaction_id_idx ON economy_ledger_entries(transaction_id);
CREATE INDEX IF NOT EXISTS economy_ledger_account_id_idx ON economy_ledger_entries(account_id);
CREATE INDEX IF NOT EXISTS economy_transactions_origin_created_idx ON economy_transactions(origin, created_at DESC);
CREATE INDEX IF NOT EXISTS economy_card_entries_card_date_idx ON economy_card_entries(card_id, business_date);
CREATE INDEX IF NOT EXISTS economy_card_entries_open_payment_order_idx
    ON economy_card_entries(card_id, (CASE WHEN entry_type = 'DAILY_INTEREST' THEN 0 ELSE 1 END), created_at, id)
    WHERE remaining_amount > 0;
CREATE INDEX IF NOT EXISTS economy_card_entries_interest_eligible_idx ON economy_card_entries(card_id, interest_eligible_date) WHERE entry_type='PURCHASE' AND remaining_amount>0;
CREATE INDEX IF NOT EXISTS economy_interest_account_id_idx ON economy_interest_accruals(account_id);
CREATE INDEX IF NOT EXISTS economy_commercial_blocks_owner_idx ON economy_commercial_blocks(owner_player_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS economy_commercial_blocks_server_position_unique ON economy_commercial_blocks(server_uuid, dimension, block_x, block_y, block_z) WHERE status='ACTIVE' AND server_uuid IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_commercial_blocks_server_type_name_idx ON economy_commercial_blocks(server_uuid, block_type, dimension, LOWER(custom_name)) WHERE status='ACTIVE' AND server_uuid IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_shop_offers_block_idx ON economy_shop_offers(commercial_block_id);
CREATE INDEX IF NOT EXISTS economy_inventory_block_type_idx ON economy_inventory_slots(commercial_block_id, inventory_type);
CREATE INDEX IF NOT EXISTS economy_gold_exchange_transaction_idx ON economy_gold_exchange_entries(transaction_id);
CREATE INDEX IF NOT EXISTS economy_gold_exchange_player_idx ON economy_gold_exchange_entries(player_uuid);
CREATE INDEX IF NOT EXISTS economy_gold_exchange_operation_created_idx ON economy_gold_exchange_entries(operation_type, created_at DESC);
CREATE INDEX IF NOT EXISTS economy_operations_state_idx ON economy_operations(state);
CREATE INDEX IF NOT EXISTS economy_operations_player_idx ON economy_operations(player_uuid);
CREATE INDEX IF NOT EXISTS economy_operations_state_updated_idx ON economy_operations(state, updated_at);
CREATE INDEX IF NOT EXISTS economy_audit_actor_idx ON economy_audit_logs(actor_player_uuid);
CREATE INDEX IF NOT EXISTS economy_audit_target_idx ON economy_audit_logs(target_type, target_id);
CREATE INDEX IF NOT EXISTS economy_mail_recipients_origin_idx ON economy_mail_recipients(origin_block_id);
CREATE INDEX IF NOT EXISTS economy_mail_recipients_destination_idx ON economy_mail_recipients(destination_block_id);
CREATE UNIQUE INDEX IF NOT EXISTS economy_server_clock_server_unique ON economy_server_clock(server_uuid) WHERE server_uuid IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS economy_groups_server_name_active_unique ON economy_groups(server_uuid, group_type, normalized_name) WHERE status='ACTIVE' AND server_uuid IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_group_members_group_idx ON economy_group_members(group_id, role);
CREATE UNIQUE INDEX IF NOT EXISTS economy_group_members_server_type_unique ON economy_group_members(server_uuid, player_uuid, group_type) WHERE server_uuid IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS economy_group_invites_server_pending_unique ON economy_group_invites(server_uuid, group_id, invited_player_uuid) WHERE status='PENDING' AND server_uuid IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS economy_claim_anchors_server_position_unique ON economy_claim_anchors(server_uuid, dimension, block_x, block_y, block_z) WHERE removed_at IS NULL AND server_uuid IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS economy_claim_anchors_server_chunk_unique ON economy_claim_anchors(server_uuid, dimension, chunk_x, chunk_z) WHERE removed_at IS NULL AND server_uuid IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS economy_claim_anchors_territory_unique ON economy_claim_anchors(territory_id) WHERE territory_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_claim_anchors_server_active_idx ON economy_claim_anchors(server_uuid, active, removed_at);
CREATE UNIQUE INDEX IF NOT EXISTS economy_claims_server_position_unique ON economy_claims(server_uuid, dimension, chunk_x, chunk_z) WHERE server_uuid IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_claims_group_idx ON economy_claims(group_id, dimension);
CREATE INDEX IF NOT EXISTS economy_claims_territory_idx ON economy_claims(territory_id) WHERE territory_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_claims_server_area_idx ON economy_claims(server_uuid, dimension, chunk_x, chunk_z);
CREATE INDEX IF NOT EXISTS economy_claim_territories_owner_idx ON economy_claim_territories(owner_player_uuid, claim_type);
CREATE INDEX IF NOT EXISTS economy_claim_territories_server_owner_idx ON economy_claim_territories(server_uuid, owner_player_uuid, claim_type);
CREATE INDEX IF NOT EXISTS economy_player_locations_player_idx ON economy_player_locations(player_uuid, created_at);
CREATE INDEX IF NOT EXISTS economy_player_locations_server_player_idx ON economy_player_locations(server_uuid, player_uuid, created_at);
CREATE INDEX IF NOT EXISTS economy_claim_invoices_debtor_idx ON economy_claim_invoices(debtor_player_uuid, status);
CREATE INDEX IF NOT EXISTS economy_claim_invoices_territory_idx ON economy_claim_invoices(territory_id, status);
CREATE INDEX IF NOT EXISTS economy_claim_direct_payments_status_idx ON economy_claim_direct_payments(status, created_at);
CREATE INDEX IF NOT EXISTS economy_claim_limit_upgrades_status_idx ON economy_claim_limit_upgrades(group_id, status, created_at);
