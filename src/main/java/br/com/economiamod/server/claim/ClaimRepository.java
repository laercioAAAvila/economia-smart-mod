package br.com.economiamod.server.claim;

import br.com.economiamod.common.claim.ClaimRecord;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ClaimRepository {
    public Optional<ClaimRecord> claimAt(String dimension, int chunkX, int chunkZ) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            return claimAt(connection, dimension, chunkX, chunkZ);
        }
    }

    public Optional<ClaimRecord> claimAt(Connection connection, String dimension, int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT c.id, c.territory_id, c.group_id, t.owner_player_uuid, c.group_type,
                       c.dimension, c.chunk_x, c.chunk_z
                  FROM economy_claims c
                  LEFT JOIN economy_claim_territories t ON t.id = c.territory_id
                 WHERE c.dimension = ? AND c.chunk_x = ? AND c.chunk_z = ?
                """)) {
            statement.setString(1, dimension);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readClaim(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<ClaimAnchorRecord> anchorAt(String dimension, int blockX, int blockY, int blockZ) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT a.id, a.territory_id, a.group_id, a.placed_by_player_uuid, a.group_type,
                            a.dimension, a.block_x, a.block_y, a.block_z, a.chunk_x, a.chunk_z, a.active,
                            COALESCE(t.land_price, 0) land_price, COALESCE(t.land_debt, 0) land_debt,
                            COALESCE(t.anchor_paid_until_millis, 0) anchor_paid_until_millis
                       FROM economy_claim_anchors a
                       LEFT JOIN economy_claim_territories t ON t.id = a.territory_id
                      WHERE a.dimension = ? AND a.block_x = ? AND a.block_y = ? AND a.block_z = ?
                        AND a.removed_at IS NULL
                     """)) {
            statement.setString(1, dimension);
            statement.setInt(2, blockX);
            statement.setInt(3, blockY);
            statement.setInt(4, blockZ);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readAnchor(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<ClaimAnchorRecord> anchorById(UUID anchorId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT a.id, a.territory_id, a.group_id, a.placed_by_player_uuid, a.group_type,
                            a.dimension, a.block_x, a.block_y, a.block_z, a.chunk_x, a.chunk_z, a.active,
                            COALESCE(t.land_price, 0) land_price, COALESCE(t.land_debt, 0) land_debt,
                            COALESCE(t.anchor_paid_until_millis, 0) anchor_paid_until_millis
                       FROM economy_claim_anchors a
                       LEFT JOIN economy_claim_territories t ON t.id = a.territory_id
                      WHERE a.id = ? AND a.removed_at IS NULL
                     """)) {
            statement.setObject(1, anchorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readAnchor(resultSet)) : Optional.empty();
            }
        }
    }

    public List<ClaimRecord> claims(Connection connection, UUID groupId, String dimension) throws SQLException {
        List<ClaimRecord> claims = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT c.id, c.territory_id, c.group_id, t.owner_player_uuid, c.group_type,
                       c.dimension, c.chunk_x, c.chunk_z
                  FROM economy_claims c LEFT JOIN economy_claim_territories t ON t.id = c.territory_id
                 WHERE c.group_id = ? AND c.dimension = ?
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, dimension);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    claims.add(readClaim(resultSet));
                }
            }
        }
        return claims;
    }

    public List<ClaimRecord> claims(Connection connection, UUID groupId) throws SQLException {
        List<ClaimRecord> claims = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT c.id, c.territory_id, c.group_id, t.owner_player_uuid, c.group_type,
                       c.dimension, c.chunk_x, c.chunk_z
                  FROM economy_claims c LEFT JOIN economy_claim_territories t ON t.id = c.territory_id
                 WHERE c.group_id = ?
                """)) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    claims.add(readClaim(resultSet));
                }
            }
        }
        return claims;
    }

    public boolean sameGroupNear(Connection connection, UUID groupId, String dimension,
                                 int chunkX, int chunkZ, int distance) throws SQLException {
        if (distance <= 0) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM economy_claims c
                 WHERE c.group_id = ? AND c.dimension = ?
                   AND ABS(chunk_x - ?) <= ? AND ABS(chunk_z - ?) <= ?
                 LIMIT 1
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, dimension);
            statement.setInt(3, chunkX);
            statement.setInt(4, distance);
            statement.setInt(5, chunkZ);
            statement.setInt(6, distance);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<ClaimAnchorRecord> activeAnchors(Connection connection, UUID groupId, String dimension) throws SQLException {
        List<ClaimAnchorRecord> anchors = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT a.id, a.territory_id, a.group_id, a.placed_by_player_uuid, a.group_type,
                       a.dimension, a.block_x, a.block_y, a.block_z, a.chunk_x, a.chunk_z, a.active,
                       COALESCE(t.land_price, 0) land_price, COALESCE(t.land_debt, 0) land_debt,
                       COALESCE(t.anchor_paid_until_millis, 0) anchor_paid_until_millis
                  FROM economy_claim_anchors a
                  LEFT JOIN economy_claim_territories t ON t.id = a.territory_id
                 WHERE a.group_id = ? AND a.dimension = ? AND a.active = TRUE AND a.removed_at IS NULL
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, dimension);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    anchors.add(readAnchor(resultSet));
                }
            }
        }
        return anchors;
    }

    public int claimCount(Connection connection, UUID groupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM economy_claims WHERE group_id = ?")) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public List<ClaimRecord> claimsAround(String dimension, int centerChunkX, int centerChunkZ, int radius) throws SQLException {
        List<ClaimRecord> claims = new ArrayList<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT c.id, c.territory_id, c.group_id, t.owner_player_uuid, c.group_type,
                            c.dimension, c.chunk_x, c.chunk_z
                      FROM economy_claims c LEFT JOIN economy_claim_territories t ON t.id = c.territory_id
                      WHERE c.dimension = ? AND c.chunk_x BETWEEN ? AND ? AND c.chunk_z BETWEEN ? AND ?
                      ORDER BY ABS(c.chunk_x - ?) + ABS(c.chunk_z - ?), c.created_at
                      LIMIT 512
                     """)) {
            statement.setString(1, dimension);
            statement.setInt(2, centerChunkX - radius);
            statement.setInt(3, centerChunkX + radius);
            statement.setInt(4, centerChunkZ - radius);
            statement.setInt(5, centerChunkZ + radius);
            statement.setInt(6, centerChunkX);
            statement.setInt(7, centerChunkZ);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    claims.add(readClaim(resultSet));
                }
            }
        }
        return claims;
    }

    public boolean otherGroupNear(Connection connection, UUID groupId, String dimension, int chunkX, int chunkZ, int distance) throws SQLException {
        if (distance <= 0) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM economy_claims
                 WHERE group_id <> ? AND dimension = ?
                   AND ABS(chunk_x - ?) <= ? AND ABS(chunk_z - ?) <= ?
                 LIMIT 1
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, dimension);
            statement.setInt(3, chunkX);
            statement.setInt(4, distance);
            statement.setInt(5, chunkZ);
            statement.setInt(6, distance);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private ClaimRecord readClaim(ResultSet resultSet) throws SQLException {
        return new ClaimRecord(
                resultSet.getObject("id", UUID.class), resultSet.getObject("territory_id", UUID.class),
                resultSet.getObject("group_id", UUID.class), resultSet.getObject("owner_player_uuid", UUID.class),
                GroupType.valueOf(resultSet.getString("group_type")), resultSet.getString("dimension"),
                resultSet.getInt("chunk_x"), resultSet.getInt("chunk_z"));
    }

    private ClaimAnchorRecord readAnchor(ResultSet resultSet) throws SQLException {
        return new ClaimAnchorRecord(
                resultSet.getObject("id", UUID.class), resultSet.getObject("territory_id", UUID.class),
                resultSet.getObject("group_id", UUID.class), resultSet.getObject("placed_by_player_uuid", UUID.class),
                GroupType.valueOf(resultSet.getString("group_type")), resultSet.getString("dimension"),
                resultSet.getInt("block_x"), resultSet.getInt("block_y"), resultSet.getInt("block_z"),
                resultSet.getInt("chunk_x"), resultSet.getInt("chunk_z"), resultSet.getBoolean("active"),
                resultSet.getLong("land_price"), resultSet.getLong("land_debt"),
                resultSet.getLong("anchor_paid_until_millis"));
    }
}
