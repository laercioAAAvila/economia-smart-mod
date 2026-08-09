ALTER TABLE economy_accounts
    ADD COLUMN IF NOT EXISTS minecraft_player_name VARCHAR(64) NULL;

CREATE INDEX IF NOT EXISTS economy_accounts_minecraft_player_name_idx
    ON economy_accounts(minecraft_player_name)
    WHERE account_type = 'PLAYER' AND minecraft_player_name IS NOT NULL;
