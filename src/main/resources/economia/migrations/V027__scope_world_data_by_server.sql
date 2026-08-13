ALTER TABLE economy_groups ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;
ALTER TABLE economy_group_members ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;
ALTER TABLE economy_group_invites ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;
ALTER TABLE economy_claim_anchors ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;
ALTER TABLE economy_claims ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;
ALTER TABLE economy_claim_territories ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;
ALTER TABLE economy_player_locations ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;
ALTER TABLE economy_server_clock ADD COLUMN IF NOT EXISTS server_uuid UUID NULL;

ALTER TABLE economy_server_clock DROP CONSTRAINT IF EXISTS economy_server_clock_pkey;
CREATE UNIQUE INDEX IF NOT EXISTS economy_server_clock_server_unique
    ON economy_server_clock(server_uuid)
    WHERE server_uuid IS NOT NULL;

UPDATE economy_groups g
   SET server_uuid = a.server_uuid
  FROM economy_accounts a
 WHERE g.server_uuid IS NULL AND a.id = g.account_id AND a.server_uuid IS NOT NULL;
UPDATE economy_group_members m
   SET server_uuid = g.server_uuid
  FROM economy_groups g
 WHERE m.server_uuid IS NULL AND m.group_id = g.id AND g.server_uuid IS NOT NULL;
UPDATE economy_group_invites i
   SET server_uuid = g.server_uuid
  FROM economy_groups g
 WHERE i.server_uuid IS NULL AND i.group_id = g.id AND g.server_uuid IS NOT NULL;
UPDATE economy_claim_anchors a
   SET server_uuid = g.server_uuid
  FROM economy_groups g
 WHERE a.server_uuid IS NULL AND a.group_id = g.id AND g.server_uuid IS NOT NULL;
UPDATE economy_claim_territories t
   SET server_uuid = a.server_uuid
  FROM economy_claim_anchors a
 WHERE t.server_uuid IS NULL AND t.anchor_id = a.id AND a.server_uuid IS NOT NULL;
UPDATE economy_claims c
   SET server_uuid = t.server_uuid
  FROM economy_claim_territories t
 WHERE c.server_uuid IS NULL AND c.territory_id = t.id
   AND t.server_uuid IS NOT NULL;
UPDATE economy_claims c
   SET server_uuid = g.server_uuid
  FROM economy_groups g
 WHERE c.server_uuid IS NULL AND c.group_id = g.id AND g.server_uuid IS NOT NULL;

DROP INDEX IF EXISTS economy_groups_name_active_unique;
CREATE UNIQUE INDEX IF NOT EXISTS economy_groups_server_name_active_unique
    ON economy_groups(server_uuid, group_type, normalized_name)
    WHERE status = 'ACTIVE' AND server_uuid IS NOT NULL;

ALTER TABLE economy_group_members
    DROP CONSTRAINT IF EXISTS economy_group_members_one_group_per_type;
CREATE UNIQUE INDEX IF NOT EXISTS economy_group_members_server_type_unique
    ON economy_group_members(server_uuid, player_uuid, group_type)
    WHERE server_uuid IS NOT NULL;

DROP INDEX IF EXISTS economy_group_invites_pending_unique;
CREATE UNIQUE INDEX IF NOT EXISTS economy_group_invites_server_pending_unique
    ON economy_group_invites(server_uuid, group_id, invited_player_uuid)
    WHERE status = 'PENDING' AND server_uuid IS NOT NULL;

DROP INDEX IF EXISTS economy_claim_anchors_active_position_unique;
DROP INDEX IF EXISTS economy_claim_anchors_active_chunk_unique;
CREATE UNIQUE INDEX IF NOT EXISTS economy_claim_anchors_server_position_unique
    ON economy_claim_anchors(server_uuid, dimension, block_x, block_y, block_z)
    WHERE removed_at IS NULL AND server_uuid IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS economy_claim_anchors_server_chunk_unique
    ON economy_claim_anchors(server_uuid, dimension, chunk_x, chunk_z)
    WHERE removed_at IS NULL AND server_uuid IS NOT NULL;

ALTER TABLE economy_claims DROP CONSTRAINT IF EXISTS economy_claims_position_unique;
CREATE UNIQUE INDEX IF NOT EXISTS economy_claims_server_position_unique
    ON economy_claims(server_uuid, dimension, chunk_x, chunk_z)
    WHERE server_uuid IS NOT NULL;

CREATE INDEX IF NOT EXISTS economy_claims_server_area_idx
    ON economy_claims(server_uuid, dimension, chunk_x, chunk_z);
CREATE INDEX IF NOT EXISTS economy_claim_anchors_server_active_idx
    ON economy_claim_anchors(server_uuid, active, removed_at);
CREATE INDEX IF NOT EXISTS economy_claim_territories_server_owner_idx
    ON economy_claim_territories(server_uuid, owner_player_uuid, claim_type);
CREATE INDEX IF NOT EXISTS economy_player_locations_server_player_idx
    ON economy_player_locations(server_uuid, player_uuid, created_at);
