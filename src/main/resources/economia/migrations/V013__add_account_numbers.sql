ALTER TABLE economy_accounts
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(6) NULL;

CREATE SEQUENCE IF NOT EXISTS economy_account_number_seq
    AS INTEGER
    MINVALUE 1
    MAXVALUE 999999
    START WITH 1
    NO CYCLE;

UPDATE economy_accounts
   SET account_number = LPAD(nextval('economy_account_number_seq')::TEXT, 6, '0')
 WHERE account_type = 'PLAYER'
   AND account_number IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS economy_accounts_account_number_unique
    ON economy_accounts(account_number)
    WHERE account_type = 'PLAYER' AND account_number IS NOT NULL;
