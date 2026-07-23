CREATE INDEX IF NOT EXISTS economy_gold_exchange_operation_created_idx
    ON economy_gold_exchange_entries(operation_type, created_at DESC)
    INCLUDE (gold_nugget_units);

CREATE INDEX IF NOT EXISTS economy_card_entries_open_payment_order_idx
    ON economy_card_entries(
        card_id,
        (CASE WHEN entry_type = 'DAILY_INTEREST' THEN 0 ELSE 1 END),
        created_at,
        id
    )
    WHERE remaining_amount > 0;

CREATE INDEX IF NOT EXISTS economy_card_entries_interest_eligible_idx
    ON economy_card_entries(card_id, interest_eligible_date)
    WHERE entry_type = 'PURCHASE'
      AND remaining_amount > 0;

CREATE INDEX IF NOT EXISTS economy_cards_credit_debt_candidates_idx
    ON economy_cards(account_id, id)
    WHERE card_type IN ('CREDIT', 'DEBIT_CREDIT')
      AND status IN ('ACTIVE', 'DISABLED')
      AND (credit_principal_outstanding + credit_interest_outstanding) > 0;
