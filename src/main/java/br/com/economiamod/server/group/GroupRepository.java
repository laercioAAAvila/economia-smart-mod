package br.com.economiamod.server.group;

import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupSummary;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GroupRepository {
    public Optional<GroupMembership> membership(UUID playerUuid, GroupType type) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            return membership(connection, playerUuid, type);
        }
    }

    public Optional<GroupMembership> membership(Connection connection, UUID playerUuid, GroupType type) throws SQLException {
        String sql = """
                SELECT group_id, group_type, player_uuid, role, permission_mask, last_active_millis
                  FROM economy_group_members
                 WHERE player_uuid = ? AND group_type = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerUuid);
            statement.setString(2, type.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readMembership(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<GroupMembership> membership(UUID playerUuid, UUID groupId) throws SQLException {
        String sql = """
                SELECT group_id, group_type, player_uuid, role, permission_mask, last_active_millis
                  FROM economy_group_members
                 WHERE player_uuid = ? AND group_id = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerUuid);
            statement.setObject(2, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readMembership(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<GroupSummary> group(UUID groupId) throws SQLException {
        String sql = """
                SELECT id, group_type, name, leader_player_uuid, vice_leader_player_uuid,
                       account_id, support_account_id, claim_limit,
                       visitor_use_buy_shop, visitor_use_sell_shop
                  FROM economy_groups
                 WHERE id = ? AND status = 'ACTIVE'
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new GroupSummary(
                        resultSet.getObject("id", UUID.class),
                        GroupType.valueOf(resultSet.getString("group_type")),
                        resultSet.getString("name"),
                        resultSet.getObject("leader_player_uuid", UUID.class),
                        resultSet.getObject("vice_leader_player_uuid", UUID.class),
                        resultSet.getObject("account_id", UUID.class),
                        resultSet.getObject("support_account_id", UUID.class),
                        resultSet.getInt("claim_limit"),
                        resultSet.getBoolean("visitor_use_buy_shop"),
                        resultSet.getBoolean("visitor_use_sell_shop")
                ));
            }
        }
    }

    public int memberCount(Connection connection, UUID groupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM economy_group_members WHERE group_id = ?")) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public void updateLastActivity(UUID playerUuid, long activeMillis) throws SQLException {
        String sql = """
                UPDATE economy_group_members
                   SET last_active_millis = GREATEST(last_active_millis, ?), updated_at = CURRENT_TIMESTAMP
                 WHERE player_uuid = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activeMillis);
            statement.setObject(2, playerUuid);
            statement.executeUpdate();
        }
    }

    public List<GroupMemberView> members(UUID groupId) throws SQLException {
        List<GroupMemberView> members = new ArrayList<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT player_uuid, role, permission_mask, last_active_millis
                       FROM economy_group_members WHERE group_id = ?
                      ORDER BY CASE role WHEN 'OWNER' THEN 0 WHEN 'LEADER' THEN 1 WHEN 'VICE_LEADER' THEN 2 ELSE 3 END,
                               last_active_millis DESC
                     """)) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    members.add(new GroupMemberView(
                            resultSet.getObject("player_uuid", UUID.class),
                            GroupRole.valueOf(resultSet.getString("role")),
                            resultSet.getInt("permission_mask"),
                            resultSet.getLong("last_active_millis")));
                }
            }
        }
        return members;
    }

    public List<GroupInviteView> pendingInvites(UUID playerUuid, GroupType type) throws SQLException {
        List<GroupInviteView> invites = new ArrayList<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT i.id, i.group_id, g.name
                       FROM economy_group_invites i
                       JOIN economy_groups g ON g.id = i.group_id AND g.status = 'ACTIVE'
                      WHERE i.invited_player_uuid = ? AND i.group_type = ? AND i.status = 'PENDING'
                      ORDER BY i.created_at DESC
                     """)) {
            statement.setObject(1, playerUuid);
            statement.setString(2, type.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    invites.add(new GroupInviteView(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getObject("group_id", UUID.class),
                            resultSet.getString("name")));
                }
            }
        }
        return invites;
    }

    private GroupMembership readMembership(ResultSet resultSet) throws SQLException {
        return new GroupMembership(
                resultSet.getObject("group_id", UUID.class),
                GroupType.valueOf(resultSet.getString("group_type")),
                resultSet.getObject("player_uuid", UUID.class),
                GroupRole.valueOf(resultSet.getString("role")),
                resultSet.getInt("permission_mask"),
                resultSet.getLong("last_active_millis")
        );
    }
}
