SELECT setval(
    'economy_account_number_seq',
    GREATEST(
        1,
        COALESCE((
            SELECT MAX(account_number::INTEGER)
              FROM economy_accounts
             WHERE account_type = 'PLAYER'
               AND account_number IS NOT NULL
               AND account_number ~ '^[0-9]{6}$'
        ), 1)
    ),
    true
);
