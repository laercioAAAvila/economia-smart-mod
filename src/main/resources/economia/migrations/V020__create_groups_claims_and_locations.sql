ALTER TABLE economy_accounts
    DROP CONSTRAINT IF EXISTS economy_accounts_type_valid;

ALTER TABLE economy_accounts
    ADD CONSTRAINT economy_accounts_type_valid CHECK (
        account_type IN (
            'PLAYER',
            'SYSTEM_TREASURY',
            'SYSTEM_CASH',
            'SYSTEM_CURRENCY_ISSUANCE',
            'CLAN_TREASURY',
            'CLAN_SUPPORT',
            'PRIVATE_PROPERTY'
        )
    );

CREATE TABLE IF NOT EXISTS economy_server_clock (
    id SMALLINT PRIMARY KEY,
    active_millis BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_server_clock_singleton CHECK (id = 1),
    CONSTRAINT economy_server_clock_non_negative CHECK (active_millis >= 0)
);

INSERT INTO economy_server_clock(id, active_millis, updated_at)
VALUES (1, 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS economy_groups (
    id UUID PRIMARY KEY,
    group_type VARCHAR(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    normalized_name VARCHAR(64) NOT NULL,
    leader_player_uuid UUID NOT NULL,
    vice_leader_player_uuid UUID NULL,
    account_id UUID NOT NULL REFERENCES economy_accounts(id),
    support_account_id UUID NULL REFERENCES economy_accounts(id),
    claim_limit INTEGER NOT NULL,
    visitor_use_buy_shop BOOLEAN NOT NULL DEFAULT FALSE,
    visitor_use_sell_shop BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP NULL,
    CONSTRAINT economy_groups_type_valid CHECK (group_type IN ('CLAN', 'PRIVATE_PROPERTY')),
    CONSTRAINT economy_groups_status_valid CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT economy_groups_claim_limit_positive CHECK (claim_limit > 0),
    CONSTRAINT economy_groups_id_type_unique UNIQUE (id, group_type)
);

CREATE UNIQUE INDEX IF NOT EXISTS economy_groups_name_active_unique
    ON economy_groups(group_type, normalized_name)
    WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS economy_group_members (
    group_id UUID NOT NULL,
    group_type VARCHAR(16) NOT NULL,
    player_uuid UUID NOT NULL,
    role VARCHAR(24) NOT NULL,
    permission_mask INTEGER NOT NULL,
    last_active_millis BIGINT NOT NULL DEFAULT 0,
    joined_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (group_id, player_uuid),
    CONSTRAINT economy_group_members_group_fk
        FOREIGN KEY (group_id, group_type) REFERENCES economy_groups(id, group_type) ON DELETE CASCADE,
    CONSTRAINT economy_group_members_type_valid CHECK (group_type IN ('CLAN', 'PRIVATE_PROPERTY')),
    CONSTRAINT economy_group_members_role_valid CHECK (role IN ('LEADER', 'VICE_LEADER', 'MEMBER')),
    CONSTRAINT economy_group_members_activity_non_negative CHECK (last_active_millis >= 0),
    CONSTRAINT economy_group_members_one_group_per_type UNIQUE (player_uuid, group_type)
);

CREATE INDEX IF NOT EXISTS economy_group_members_group_idx
    ON economy_group_members(group_id, role);

CREATE TABLE IF NOT EXISTS economy_group_invites (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES economy_groups(id) ON DELETE CASCADE,
    group_type VARCHAR(16) NOT NULL,
    invited_player_uuid UUID NOT NULL,
    invited_by_player_uuid UUID NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP NULL,
    CONSTRAINT economy_group_invites_type_valid CHECK (group_type IN ('CLAN', 'PRIVATE_PROPERTY')),
    CONSTRAINT economy_group_invites_status_valid CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS economy_group_invites_pending_unique
    ON economy_group_invites(group_id, invited_player_uuid)
    WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS economy_claim_anchors (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES economy_groups(id) ON DELETE CASCADE,
    group_type VARCHAR(16) NOT NULL,
    dimension VARCHAR(255) NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    placed_by_player_uuid UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    removed_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS economy_claim_anchors_active_position_unique
    ON economy_claim_anchors(dimension, block_x, block_y, block_z)
    WHERE removed_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS economy_claim_anchors_active_chunk_unique
    ON economy_claim_anchors(dimension, chunk_x, chunk_z)
    WHERE removed_at IS NULL;

CREATE TABLE IF NOT EXISTS economy_claims (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES economy_groups(id) ON DELETE CASCADE,
    group_type VARCHAR(16) NOT NULL,
    dimension VARCHAR(255) NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    created_by_player_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT economy_claims_position_unique UNIQUE (dimension, chunk_x, chunk_z)
);

CREATE INDEX IF NOT EXISTS economy_claims_group_idx
    ON economy_claims(group_id, dimension);

CREATE TABLE IF NOT EXISTS economy_player_locations (
    id UUID PRIMARY KEY,
    player_uuid UUID NOT NULL,
    name VARCHAR(64) NOT NULL,
    dimension VARCHAR(255) NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS economy_player_locations_player_idx
    ON economy_player_locations(player_uuid, created_at);
