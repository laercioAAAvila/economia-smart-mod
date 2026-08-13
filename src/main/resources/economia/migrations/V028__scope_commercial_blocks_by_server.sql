ALTER TABLE economy_commercial_blocks
    ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;

UPDATE economy_commercial_blocks block
   SET server_uuid = account.server_uuid
  FROM economy_accounts account
 WHERE block.server_uuid IS NULL
   AND block.linked_account_id = account.id
   AND account.server_uuid IS NOT NULL;

DROP INDEX IF EXISTS economy_commercial_blocks_active_position_unique;

CREATE UNIQUE INDEX IF NOT EXISTS economy_commercial_blocks_server_position_unique
    ON economy_commercial_blocks(server_uuid, dimension, block_x, block_y, block_z)
    WHERE status = 'ACTIVE' AND server_uuid IS NOT NULL;

CREATE INDEX IF NOT EXISTS economy_commercial_blocks_server_type_name_idx
    ON economy_commercial_blocks(server_uuid, block_type, dimension, LOWER(custom_name))
    WHERE status = 'ACTIVE' AND server_uuid IS NOT NULL;
