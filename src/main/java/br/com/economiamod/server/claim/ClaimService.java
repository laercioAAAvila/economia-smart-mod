package br.com.economiamod.server.claim;

import br.com.economiamod.common.claim.ClaimRecord;
import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupSummary;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.group.GroupRepository;
import br.com.economiamod.server.group.GroupService;
import br.com.economiamod.server.group.ServerActiveClockService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClaimService {
    private final GroupRepository groupRepository = new GroupRepository();
    private final GroupService groupService = new GroupService();
    private final ClaimRepository claimRepository = new ClaimRepository();
    private final ClaimPriceService priceService = new ClaimPriceService();

    public ClaimOperationResult placeAnchor(UUID playerUuid, GroupType type, String dimension,
                                            int blockX, int blockY, int blockZ) throws SQLException {
        GroupMembership membership = groupRepository.membership(playerUuid, type).orElse(null);
        if (type == GroupType.CLAN && !canPlaceAnchor(membership)) {
            return ClaimOperationResult.denied("role_denied");
        }
        UUID portfolioGroupId = membership != null && membership.role() == GroupRole.OWNER
                ? membership.groupId() : null;
        UUID groupId = type == GroupType.CLAN && membership != null ? membership.groupId() : null;
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (claimRepository.claimAt(connection, dimension, chunkX, chunkZ).isPresent()
                        || anchorChunkExists(connection, dimension, chunkX, chunkZ)
                        || anyClaimNear(connection, type == GroupType.PRIVATE_PROPERTY ? portfolioGroupId : groupId,
                        dimension, chunkX, chunkZ, EconomyServerConfig.CLAIM_EXTERNAL_DISTANCE.get())
                        || (type == GroupType.PRIVATE_PROPERTY && portfolioGroupId != null
                        && claimRepository.sameGroupNear(connection, portfolioGroupId, dimension, chunkX, chunkZ,
                        EconomyServerConfig.PRIVATE_PROPERTY_CLAIM_DISTANCE.get()))) {
                    connection.rollback();
                    return ClaimOperationResult.denied("invalid_position");
                }
                UUID anchorId = UUID.randomUUID();
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO economy_claim_anchors(
                            id, group_id, group_type, dimension, block_x, block_y, block_z,
                            chunk_x, chunk_z, placed_by_player_uuid, active, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP)
                        """)) {
                    statement.setObject(1, anchorId);
                    statement.setObject(2, groupId);
                    statement.setString(3, type.name());
                    statement.setString(4, dimension);
                    statement.setInt(5, blockX);
                    statement.setInt(6, blockY);
                    statement.setInt(7, blockZ);
                    statement.setInt(8, chunkX);
                    statement.setInt(9, chunkZ);
                    statement.setObject(10, playerUuid);
                    statement.executeUpdate();
                }
                connection.commit();
                return ClaimOperationResult.success(anchorId);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                if (!connection.isClosed()) {
                    connection.setAutoCommit(previousAutoCommit);
                }
            }
        }
    }

    public ClaimOperationResult activateAnchor(UUID playerUuid, UUID anchorId) throws SQLException {
        return activateAnchor(playerUuid, anchorId, false);
    }

    public ClaimOperationResult activatePaidAnchor(UUID playerUuid, UUID anchorId) throws SQLException {
        return activateAnchor(playerUuid, anchorId, true);
    }

    private ClaimOperationResult activateAnchor(UUID playerUuid, UUID anchorId, boolean paid) throws SQLException {
        ClaimAnchorRecord preview = claimRepository.anchorById(anchorId).orElse(null);
        if (preview == null) {
            return ClaimOperationResult.denied("anchor_missing");
        }
        UUID targetGroupId = preview.groupId();
        if (preview.groupType() == GroupType.PRIVATE_PROPERTY) {
            if (!playerUuid.equals(preview.placedByPlayerUuid())) {
                return ClaimOperationResult.denied("owner_required");
            }
            targetGroupId = groupService.ensurePrivatePropertyPortfolio(
                    playerUuid, ServerActiveClockService.INSTANCE.currentMillis());
        }
        final UUID groupId = targetGroupId;
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ClaimAnchorRecord anchor = lockAnchor(connection, anchorId);
                if (anchor == null || anchor.active()) {
                    connection.rollback();
                    return ClaimOperationResult.denied("already_claimed");
                }
                if (anchor.groupType() == GroupType.CLAN && !isLeader(playerUuid, anchor.groupId())) {
                    connection.rollback();
                    return ClaimOperationResult.denied("leader_required");
                }
                if (claimRepository.claimAt(connection, anchor.dimension(), anchor.chunkX(), anchor.chunkZ()).isPresent()) {
                    connection.rollback();
                    return ClaimOperationResult.denied("already_claimed");
                }
                if (anyClaimNear(connection, groupId, anchor.dimension(), anchor.chunkX(), anchor.chunkZ(),
                        EconomyServerConfig.CLAIM_EXTERNAL_DISTANCE.get())
                        || (anchor.groupType() == GroupType.PRIVATE_PROPERTY
                        && claimRepository.sameGroupNear(connection, groupId, anchor.dimension(),
                        anchor.chunkX(), anchor.chunkZ(), EconomyServerConfig.PRIVATE_PROPERTY_CLAIM_DISTANCE.get()))) {
                    connection.rollback();
                    return ClaimOperationResult.denied("invalid_position");
                }
                if (!hasClaimCapacity(connection, groupId)) {
                    connection.rollback();
                    return ClaimOperationResult.denied("claim_limit");
                }
                if (storedTerritoryCount(connection, anchor.groupType(), groupId, playerUuid) >= maxTerritories(anchor.groupType())) {
                    connection.rollback();
                    return ClaimOperationResult.denied("territory_limit");
                }
                long landPrice = priceService.landPrice(anchor.dimension(), anchor.blockX(), anchor.blockZ());
                UUID territoryId = UUID.randomUUID();
                insertTerritory(connection, territoryId, anchor.id(), anchor.groupType(), groupId,
                        anchor.groupType() == GroupType.PRIVATE_PROPERTY ? playerUuid : null, landPrice,
                        paid ? 0L : landPrice);
                insertClaim(connection, territoryId, groupId, anchor.groupType(), anchor.dimension(),
                        anchor.chunkX(), anchor.chunkZ(), playerUuid);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE economy_claim_anchors SET territory_id = ?, group_id = ?, active = TRUE WHERE id = ?")) {
                    statement.setObject(1, territoryId);
                    statement.setObject(2, groupId);
                    statement.setObject(3, anchorId);
                    statement.executeUpdate();
                }
                UUID resultId = territoryId;
                if (!paid) {
                    resultId = insertInvoice(connection, territoryId, "LAND", playerUuid, playerUuid,
                            null, null, null, landPrice, 0);
                }
                connection.commit();
                return ClaimOperationResult.success(resultId);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                if (!connection.isClosed()) {
                    connection.setAutoCommit(previousAutoCommit);
                }
            }
        }
    }

    public ClaimOperationResult toggleChunk(UUID playerUuid, UUID groupId, String dimension, int chunkX, int chunkZ) throws SQLException {
        if (!controlsGroup(playerUuid, groupId)) {
            return ClaimOperationResult.denied("owner_required");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ClaimRecord existing = claimRepository.claimAt(connection, dimension, chunkX, chunkZ).orElse(null);
                if (existing != null) {
                    if (!existing.groupId().equals(groupId) || activeAnchorInChunk(connection, groupId, dimension, chunkX, chunkZ)) {
                        connection.rollback();
                        return ClaimOperationResult.denied("anchor_or_foreign_claim");
                    }
                    deleteClaim(connection, existing.id());
                    pruneUnanchored(connection, groupId, dimension);
                    connection.commit();
                    return ClaimOperationResult.success(existing.id());
                }

                connection.rollback();
                return ClaimOperationResult.denied("purchase_required");
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public ClaimOperationResult purchaseChunk(UUID playerUuid, UUID anchorId, String dimension,
                                              int chunkX, int chunkZ) throws SQLException {
        ClaimAnchorRecord preview = claimRepository.anchorById(anchorId).orElse(null);
        if (preview == null || !preview.active() || preview.territoryId() == null
                || !preview.dimension().equals(dimension)
                || !controlsGroup(playerUuid, preview.groupId())) {
            return ClaimOperationResult.denied("owner_required");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ClaimAnchorRecord anchor = lockAnchor(connection, anchorId);
                if (anchor == null || !anchor.active() || anchor.territoryId() == null
                        || !anchor.dimension().equals(dimension)
                        || !controlsGroup(playerUuid, anchor.groupId())) {
                    connection.rollback();
                    return ClaimOperationResult.denied("owner_required");
                }
                if (!lockGroup(connection, anchor.groupId()) || !hasClaimCapacity(connection, anchor.groupId())) {
                    connection.rollback();
                    return ClaimOperationResult.denied("claim_limit");
                }
                if (claimRepository.claimAt(connection, dimension, chunkX, chunkZ).isPresent()) {
                    connection.rollback();
                    return ClaimOperationResult.denied("already_claimed");
                }
                if (!adjacentToTerritory(connection, anchor.territoryId(), dimension, chunkX, chunkZ)
                        || claimRepository.otherGroupNear(connection, anchor.groupId(), dimension, chunkX, chunkZ,
                        EconomyServerConfig.CLAIM_EXTERNAL_DISTANCE.get())) {
                    connection.rollback();
                    return ClaimOperationResult.denied("not_adjacent_or_too_close");
                }

                long chunkPrice = priceService.landPrice(dimension, chunkX * 16 + 8, chunkZ * 16 + 8);
                insertClaim(connection, anchor.territoryId(), anchor.groupId(), anchor.groupType(),
                        dimension, chunkX, chunkZ, playerUuid);
                addChunkPriceAndDebt(connection, anchor.territoryId(), chunkPrice);
                UUID invoiceId = insertInvoice(connection, anchor.territoryId(), "LAND", playerUuid, playerUuid,
                        null, null, null, chunkPrice, 0);
                connection.commit();
                return ClaimOperationResult.success(invoiceId);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                if (!connection.isClosed()) {
                    connection.setAutoCommit(previousAutoCommit);
                }
            }
        }
    }

    public ClaimOperationResult removeAnchor(UUID playerUuid, String dimension, int blockX, int blockY, int blockZ) throws SQLException {
        ClaimAnchorRecord anchor = claimRepository.anchorAt(dimension, blockX, blockY, blockZ).orElse(null);
        if (anchor == null || (anchor.active()
                ? !controlsGroup(playerUuid, anchor.groupId())
                : !playerUuid.equals(anchor.placedByPlayerUuid()))) {
            return ClaimOperationResult.denied("owner_required");
        }
        if (anchor.active() && anchor.landDebt() > 0L) {
            return ClaimOperationResult.denied("land_debt");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE economy_claim_anchors SET active = FALSE, removed_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    statement.setObject(1, anchor.id());
                    statement.executeUpdate();
                }
                if (anchor.territoryId() != null) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM economy_claims WHERE territory_id = ?")) {
                        statement.setObject(1, anchor.territoryId());
                        statement.executeUpdate();
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM economy_claim_territories WHERE id = ?")) {
                        statement.setObject(1, anchor.territoryId());
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                return ClaimOperationResult.success(anchor.id());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void pruneUnanchored(Connection connection, UUID groupId, String dimension) throws SQLException {
        List<ClaimRecord> claims = claimRepository.claims(connection, groupId, dimension);
        Set<ChunkKey> remaining = new HashSet<>();
        Map<ChunkKey, UUID> ids = new HashMap<>();
        for (ClaimRecord claim : claims) {
            ChunkKey key = new ChunkKey(claim.chunkX(), claim.chunkZ());
            remaining.add(key);
            ids.put(key, claim.id());
        }
        Set<ChunkKey> anchored = new HashSet<>();
        for (ClaimAnchorRecord anchor : claimRepository.activeAnchors(connection, groupId, dimension)) {
            anchored.add(new ChunkKey(anchor.chunkX(), anchor.chunkZ()));
        }
        Set<ChunkKey> valid = new HashSet<>();
        ArrayDeque<ChunkKey> queue = new ArrayDeque<>(anchored);
        while (!queue.isEmpty()) {
            ChunkKey current = queue.removeFirst();
            if (!remaining.contains(current) || !valid.add(current)) {
                continue;
            }
            queue.add(new ChunkKey(current.x() + 1, current.z()));
            queue.add(new ChunkKey(current.x() - 1, current.z()));
            queue.add(new ChunkKey(current.x(), current.z() + 1));
            queue.add(new ChunkKey(current.x(), current.z() - 1));
        }
        for (ChunkKey key : remaining) {
            if (!valid.contains(key)) {
                deleteClaim(connection, ids.get(key));
            }
        }
    }

    private boolean canPlaceAnchor(GroupMembership membership) {
        return membership != null && (membership.role() == GroupRole.LEADER
                || (membership.groupType() == GroupType.CLAN && membership.role() == GroupRole.VICE_LEADER));
    }

    private boolean isLeader(UUID playerUuid, UUID groupId) throws SQLException {
        return groupId != null && groupRepository.membership(playerUuid, groupId)
                .map(m -> m.role() == GroupRole.LEADER).orElse(false);
    }

    private boolean controlsGroup(UUID playerUuid, UUID groupId) throws SQLException {
        return groupId != null && groupRepository.membership(playerUuid, groupId)
                .map(m -> m.role() == GroupRole.LEADER || m.role() == GroupRole.OWNER).orElse(false);
    }

    private boolean hasClaimCapacity(Connection connection, UUID groupId) throws SQLException {
        GroupSummary group = groupRepository.group(groupId).orElse(null);
        return group != null && claimRepository.claimCount(connection, groupId) < group.claimLimit();
    }

    private boolean lockGroup(Connection connection, UUID groupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM economy_groups WHERE id = ? AND status = 'ACTIVE' FOR UPDATE")) {
            statement.setObject(1, groupId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private int maxTerritories(GroupType type) {
        return type == GroupType.CLAN
                ? EconomyServerConfig.CLAN_MAX_TERRITORIES.get()
                : EconomyServerConfig.PRIVATE_PROPERTY_MAX_TERRITORIES.get();
    }

    private int territoryCount(Connection connection, UUID groupId) throws SQLException {
        List<ClaimRecord> claims = claimRepository.claims(connection, groupId);
        Set<TerritoryChunk> remaining = new HashSet<>();
        for (ClaimRecord claim : claims) {
            remaining.add(new TerritoryChunk(claim.dimension(), claim.chunkX(), claim.chunkZ()));
        }
        int territories = 0;
        ArrayDeque<TerritoryChunk> queue = new ArrayDeque<>();
        while (!remaining.isEmpty()) {
            territories++;
            TerritoryChunk start = remaining.iterator().next();
            remaining.remove(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                TerritoryChunk current = queue.removeFirst();
                visitTerritoryNeighbor(remaining, queue,
                        new TerritoryChunk(current.dimension(), current.x() + 1, current.z()));
                visitTerritoryNeighbor(remaining, queue,
                        new TerritoryChunk(current.dimension(), current.x() - 1, current.z()));
                visitTerritoryNeighbor(remaining, queue,
                        new TerritoryChunk(current.dimension(), current.x(), current.z() + 1));
                visitTerritoryNeighbor(remaining, queue,
                        new TerritoryChunk(current.dimension(), current.x(), current.z() - 1));
            }
        }
        return territories;
    }

    private int storedTerritoryCount(Connection connection, GroupType type, UUID groupId,
                                     UUID ownerUuid) throws SQLException {
        String sql = type == GroupType.CLAN
                ? "SELECT COUNT(*) FROM economy_claim_territories WHERE claim_type = 'CLAN' AND group_id = ?"
                : "SELECT COUNT(*) FROM economy_claim_territories WHERE claim_type = 'PRIVATE_PROPERTY' AND owner_player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, type == GroupType.CLAN ? groupId : ownerUuid);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private boolean anyClaimNear(Connection connection, UUID ownGroupId, String dimension,
                                 int chunkX, int chunkZ, int distance) throws SQLException {
        if (distance <= 0) {
            return false;
        }
        String sql = ownGroupId == null ? """
                SELECT 1 FROM economy_claims WHERE dimension = ?
                  AND ABS(chunk_x - ?) <= ? AND ABS(chunk_z - ?) <= ? LIMIT 1
                """ : """
                SELECT 1 FROM economy_claims WHERE (group_id IS NULL OR group_id <> ?) AND dimension = ?
                  AND ABS(chunk_x - ?) <= ? AND ABS(chunk_z - ?) <= ? LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (ownGroupId != null) {
                statement.setObject(index++, ownGroupId);
            }
            statement.setString(index++, dimension);
            statement.setInt(index++, chunkX);
            statement.setInt(index++, distance);
            statement.setInt(index++, chunkZ);
            statement.setInt(index, distance);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void visitTerritoryNeighbor(Set<TerritoryChunk> remaining, ArrayDeque<TerritoryChunk> queue,
                                        TerritoryChunk neighbor) {
        if (remaining.remove(neighbor)) {
            queue.add(neighbor);
        }
    }

    private boolean anchorChunkExists(Connection connection, String dimension, int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM economy_claim_anchors
                 WHERE dimension = ? AND chunk_x = ? AND chunk_z = ? AND removed_at IS NULL LIMIT 1
                """)) {
            statement.setString(1, dimension);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean activeAnchorInChunk(Connection connection, UUID groupId, String dimension, int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM economy_claim_anchors
                 WHERE group_id = ? AND dimension = ? AND chunk_x = ? AND chunk_z = ?
                   AND active = TRUE AND removed_at IS NULL LIMIT 1
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, dimension);
            statement.setInt(3, chunkX);
            statement.setInt(4, chunkZ);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean adjacentToGroup(Connection connection, UUID groupId, String dimension, int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM economy_claims WHERE group_id = ? AND dimension = ? AND (
                    (chunk_x = ? AND ABS(chunk_z - ?) = 1) OR
                    (chunk_z = ? AND ABS(chunk_x - ?) = 1)
                ) LIMIT 1
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, dimension);
            statement.setInt(3, chunkX);
            statement.setInt(4, chunkZ);
            statement.setInt(5, chunkZ);
            statement.setInt(6, chunkX);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private UUID adjacentTerritory(Connection connection, UUID groupId, String dimension,
                                   int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT territory_id FROM economy_claims WHERE group_id = ? AND dimension = ? AND (
                    (chunk_x = ? AND ABS(chunk_z - ?) = 1) OR
                    (chunk_z = ? AND ABS(chunk_x - ?) = 1)
                ) AND territory_id IS NOT NULL LIMIT 1
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, dimension);
            statement.setInt(3, chunkX);
            statement.setInt(4, chunkZ);
            statement.setInt(5, chunkZ);
            statement.setInt(6, chunkX);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject(1, UUID.class) : null;
            }
        }
    }

    private boolean adjacentToTerritory(Connection connection, UUID territoryId, String dimension,
                                        int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM economy_claims WHERE territory_id = ? AND dimension = ? AND (
                    (chunk_x = ? AND ABS(chunk_z - ?) = 1) OR
                    (chunk_z = ? AND ABS(chunk_x - ?) = 1)
                ) LIMIT 1
                """)) {
            statement.setObject(1, territoryId);
            statement.setString(2, dimension);
            statement.setInt(3, chunkX);
            statement.setInt(4, chunkZ);
            statement.setInt(5, chunkZ);
            statement.setInt(6, chunkX);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void addChunkPriceAndDebt(Connection connection, UUID territoryId, long chunkPrice)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE economy_claim_territories
                   SET land_price = land_price + ?, land_debt = land_debt + ?,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """)) {
            statement.setLong(1, chunkPrice);
            statement.setLong(2, chunkPrice);
            statement.setObject(3, territoryId);
            statement.executeUpdate();
        }
    }

    private ClaimAnchorRecord lockAnchor(Connection connection, UUID anchorId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT a.id, a.territory_id, a.group_id, a.placed_by_player_uuid, a.group_type,
                       a.dimension, a.block_x, a.block_y, a.block_z, a.chunk_x, a.chunk_z, a.active,
                       COALESCE(t.land_price, 0) land_price, COALESCE(t.land_debt, 0) land_debt,
                       COALESCE(t.anchor_paid_until_millis, 0) anchor_paid_until_millis
                  FROM economy_claim_anchors a
                  LEFT JOIN economy_claim_territories t ON t.id = a.territory_id
                 WHERE a.id = ? AND a.removed_at IS NULL FOR UPDATE OF a
                """)) {
            statement.setObject(1, anchorId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? new ClaimAnchorRecord(
                        resultSet.getObject("id", UUID.class), resultSet.getObject("territory_id", UUID.class),
                        resultSet.getObject("group_id", UUID.class), resultSet.getObject("placed_by_player_uuid", UUID.class),
                        GroupType.valueOf(resultSet.getString("group_type")), resultSet.getString("dimension"),
                        resultSet.getInt("block_x"), resultSet.getInt("block_y"), resultSet.getInt("block_z"),
                        resultSet.getInt("chunk_x"), resultSet.getInt("chunk_z"), resultSet.getBoolean("active"),
                        resultSet.getLong("land_price"), resultSet.getLong("land_debt"),
                        resultSet.getLong("anchor_paid_until_millis")) : null;
            }
        }
    }

    private UUID insertClaim(Connection connection, UUID territoryId, UUID groupId, GroupType type, String dimension,
                             int chunkX, int chunkZ, UUID playerUuid) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_claims(id, territory_id, group_id, group_type, dimension, chunk_x, chunk_z,
                                           created_by_player_uuid, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """)) {
            statement.setObject(1, id);
            statement.setObject(2, territoryId);
            statement.setObject(3, groupId);
            statement.setString(4, type.name());
            statement.setString(5, dimension);
            statement.setInt(6, chunkX);
            statement.setInt(7, chunkZ);
            statement.setObject(8, playerUuid);
            statement.executeUpdate();
        }
        return id;
    }

    private void insertTerritory(Connection connection, UUID territoryId, UUID anchorId, GroupType type,
                                 UUID groupId, UUID ownerUuid, long price, long debt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_claim_territories(
                    id, anchor_id, claim_type, group_id, owner_player_uuid, land_price, land_debt,
                    anchor_paid_until_millis, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            statement.setObject(1, territoryId);
            statement.setObject(2, anchorId);
            statement.setString(3, type.name());
            statement.setObject(4, groupId);
            statement.setObject(5, ownerUuid);
            statement.setLong(6, price);
            statement.setLong(7, debt);
            statement.executeUpdate();
        }
    }

    private UUID insertInvoice(Connection connection, UUID territoryId, String invoiceType,
                               UUID debtorUuid, UUID issuerUuid, UUID sellerUuid, UUID sellerAccountId,
                               UUID buyerGroupId, long amount, int days) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_claim_invoices(
                    id, territory_id, invoice_type, debtor_player_uuid, issuer_player_uuid,
                    seller_player_uuid, seller_account_id, buyer_group_id, amount, minecraft_days,
                    status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)
                """)) {
            statement.setObject(1, id);
            statement.setObject(2, territoryId);
            statement.setString(3, invoiceType);
            statement.setObject(4, debtorUuid);
            statement.setObject(5, issuerUuid);
            statement.setObject(6, sellerUuid);
            statement.setObject(7, sellerAccountId);
            statement.setObject(8, buyerGroupId);
            statement.setLong(9, amount);
            statement.setInt(10, days);
            statement.executeUpdate();
        }
        return id;
    }

    private void deleteClaim(Connection connection, UUID claimId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM economy_claims WHERE id = ?")) {
            statement.setObject(1, claimId);
            statement.executeUpdate();
        }
    }

    private void deleteClaimUnchecked(Connection connection, UUID claimId) {
        try {
            deleteClaim(connection, claimId);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record ChunkKey(int x, int z) {
    }

    private record TerritoryChunk(String dimension, int x, int z) {
    }
}
