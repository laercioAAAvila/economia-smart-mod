ALTER TABLE economy_claim_anchors ALTER COLUMN group_id DROP NOT NULL;
ALTER TABLE economy_claims ALTER COLUMN group_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS economy_claim_territories (
    id UUID PRIMARY KEY,
    anchor_id UUID NOT NULL UNIQUE REFERENCES economy_claim_anchors(id) ON DELETE CASCADE,
    claim_type VARCHAR(32) NOT NULL,
    group_id UUID NULL REFERENCES economy_groups(id),
    owner_player_uuid UUID NULL,
    land_price BIGINT NOT NULL,
    land_debt BIGINT NOT NULL,
    anchor_paid_until_millis BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_claim_territories_type_valid CHECK (claim_type IN ('CLAN', 'PRIVATE_PROPERTY')),
    CONSTRAINT economy_claim_territories_price_non_negative CHECK (land_price >= 0 AND land_debt >= 0),
    CONSTRAINT economy_claim_territories_anchor_time_non_negative CHECK (anchor_paid_until_millis >= 0),
    CONSTRAINT economy_claim_territories_owner_valid CHECK (
        (claim_type = 'CLAN' AND group_id IS NOT NULL) OR
        (claim_type = 'PRIVATE_PROPERTY' AND group_id IS NOT NULL AND owner_player_uuid IS NOT NULL)
    )
);

ALTER TABLE economy_claim_anchors ADD COLUMN IF NOT EXISTS territory_id UUID NULL;
ALTER TABLE economy_claims ADD COLUMN IF NOT EXISTS territory_id UUID NULL;

INSERT INTO economy_claim_territories(
    id, anchor_id, claim_type, group_id, owner_player_uuid, land_price, land_debt,
    anchor_paid_until_millis, created_at, updated_at
)
SELECT a.id, a.id, a.group_type, a.group_id,
       CASE WHEN a.group_type = 'PRIVATE_PROPERTY' THEN g.leader_player_uuid ELSE NULL END,
       0, 0, 0, a.created_at, CURRENT_TIMESTAMP
  FROM economy_claim_anchors a
  JOIN economy_groups g ON g.id = a.group_id
 WHERE a.active = TRUE AND a.removed_at IS NULL
ON CONFLICT (id) DO NOTHING;

UPDATE economy_claim_anchors a
   SET territory_id = a.id
 WHERE a.active = TRUE AND a.removed_at IS NULL AND a.territory_id IS NULL;

UPDATE economy_claims c
   SET territory_id = (
       SELECT a.id
         FROM economy_claim_anchors a
        WHERE a.group_id = c.group_id
          AND a.dimension = c.dimension
          AND a.active = TRUE
          AND a.removed_at IS NULL
        ORDER BY ABS(a.chunk_x - c.chunk_x) + ABS(a.chunk_z - c.chunk_z), a.created_at
        LIMIT 1
   )
 WHERE c.territory_id IS NULL
   AND EXISTS (
       SELECT 1 FROM economy_claim_anchors a
        WHERE a.group_id = c.group_id
          AND a.dimension = c.dimension
          AND a.active = TRUE
          AND a.removed_at IS NULL
   );

CREATE UNIQUE INDEX IF NOT EXISTS economy_claim_anchors_territory_unique
    ON economy_claim_anchors(territory_id) WHERE territory_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_claims_territory_idx
    ON economy_claims(territory_id) WHERE territory_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS economy_claim_territories_owner_idx
    ON economy_claim_territories(owner_player_uuid, claim_type);

CREATE TABLE IF NOT EXISTS economy_private_property_members (
    territory_id UUID NOT NULL REFERENCES economy_claim_territories(id) ON DELETE CASCADE,
    player_uuid UUID NOT NULL,
    invited_by_player_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (territory_id, player_uuid)
);

INSERT INTO economy_private_property_members(
    territory_id, player_uuid, invited_by_player_uuid, created_at
)
SELECT t.id, member.player_uuid, owner.player_uuid, member.joined_at
  FROM economy_claim_territories t
  JOIN economy_group_members member
    ON member.group_id = t.group_id AND member.group_type = 'PRIVATE_PROPERTY' AND member.role = 'MEMBER'
  JOIN economy_group_members owner
    ON owner.group_id = t.group_id AND owner.role IN ('LEADER', 'OWNER')
ON CONFLICT (territory_id, player_uuid) DO NOTHING;

CREATE TABLE IF NOT EXISTS economy_claim_invoices (
    id UUID PRIMARY KEY,
    territory_id UUID NOT NULL REFERENCES economy_claim_territories(id) ON DELETE CASCADE,
    invoice_type VARCHAR(24) NOT NULL,
    debtor_player_uuid UUID NOT NULL,
    issuer_player_uuid UUID NOT NULL,
    seller_player_uuid UUID NULL,
    seller_account_id UUID NULL REFERENCES economy_accounts(id),
    buyer_group_id UUID NULL REFERENCES economy_groups(id),
    amount BIGINT NOT NULL,
    minecraft_days INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    CONSTRAINT economy_claim_invoices_type_valid CHECK (invoice_type IN ('LAND', 'ANCHOR', 'SALE')),
    CONSTRAINT economy_claim_invoices_status_valid CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT economy_claim_invoices_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT economy_claim_invoices_days_non_negative CHECK (minecraft_days >= 0)
);

CREATE INDEX IF NOT EXISTS economy_claim_invoices_debtor_idx
    ON economy_claim_invoices(debtor_player_uuid, status);
CREATE INDEX IF NOT EXISTS economy_claim_invoices_territory_idx
    ON economy_claim_invoices(territory_id, status);

ALTER TABLE economy_group_members DROP CONSTRAINT IF EXISTS economy_group_members_role_valid;
UPDATE economy_group_members
   SET role = 'OWNER'
 WHERE group_type = 'PRIVATE_PROPERTY' AND role = 'LEADER';
ALTER TABLE economy_group_members
    ADD CONSTRAINT economy_group_members_role_valid CHECK (role IN ('OWNER', 'LEADER', 'VICE_LEADER', 'MEMBER'));
