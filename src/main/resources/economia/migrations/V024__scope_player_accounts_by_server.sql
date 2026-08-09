ALTER TABLE economy_accounts
    ADD COLUMN IF NOT EXISTS server_uuid UUID NULL,
    ADD COLUMN IF NOT EXISTS opening_fee BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS opening_request_id UUID NULL;

ALTER TABLE economy_accounts DROP CONSTRAINT IF EXISTS economy_accounts_status_valid;
ALTER TABLE economy_accounts ADD CONSTRAINT economy_accounts_status_valid
    CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED', 'CLOSED'));

ALTER TABLE economy_accounts ADD CONSTRAINT economy_accounts_opening_fee_non_negative
    CHECK (opening_fee >= 0);

DROP INDEX IF EXISTS economy_accounts_player_uuid_unique;
DROP INDEX IF EXISTS economy_accounts_username_normalized_unique;

CREATE UNIQUE INDEX IF NOT EXISTS economy_accounts_server_username_unique
    ON economy_accounts(server_uuid, username_normalized)
    WHERE account_type = 'PLAYER' AND server_uuid IS NOT NULL AND username_normalized IS NOT NULL
      AND status <> 'CLOSED';

CREATE INDEX IF NOT EXISTS economy_accounts_server_player_idx
    ON economy_accounts(server_uuid, player_uuid, created_at)
    WHERE account_type = 'PLAYER' AND server_uuid IS NOT NULL AND player_uuid IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS economy_accounts_opening_request_unique
    ON economy_accounts(opening_request_id)
    WHERE opening_request_id IS NOT NULL;
