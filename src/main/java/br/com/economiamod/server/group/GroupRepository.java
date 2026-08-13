package br.com.economiamod.server.group;

import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupSummary;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.account.BankServerIdentityService;
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
                SELECT m.group_id, m.group_type, m.player_uuid, m.role, m.permission_mask, m.last_active_millis
                  FROM economy_group_members m
                  JOIN economy_groups g ON g.id = m.group_id
                 WHERE m.player_uuid = ? AND m.group_type = ? AND g.server_uuid = ? AND g.status = 'ACTIVE'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerUuid);
            statement.setString(2, type.name());
            statement.setObject(3, BankServerIdentityService.INSTANCE.current());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readMembership(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<GroupMembership> membership(UUID playerUuid, UUID groupId) throws SQLException {
        String sql = """
                SELECT m.group_id, m.group_type, m.player_uuid, m.role, m.permission_mask, m.last_active_millis
                  FROM economy_group_members m
                  JOIN economy_groups g ON g.id = m.group_id
                 WHERE m.player_uuid = ? AND m.group_id = ? AND g.server_uuid = ? AND g.status = 'ACTIVE'
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerUuid);
            statement.setObject(2, groupId);
            statement.setObject(3, BankServerIdentityService.INSTANCE.current());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readMembership(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<GroupSummary> group(UUID groupId) throws SQLException {
        String sql = """
                SELECT id, group_type, name, leader_player_uuid, vice_leader_player_uuid,
                       account_id, support_account_id, claim_limit
                  FROM economy_groups
                 WHERE id = ? AND server_uuid = ? AND status = 'ACTIVE'
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, groupId);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
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
                        resultSet.getInt("claim_limit")
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
                UPDATE economy_group_members m
                   SET last_active_millis = GREATEST(last_active_millis, ?), updated_at = CURRENT_TIMESTAMP
                  FROM economy_groups g
                 WHERE m.group_id = g.id AND m.player_uuid = ? AND g.server_uuid = ? AND g.status = 'ACTIVE'
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activeMillis);
            statement.setObject(2, playerUuid);
            statement.setObject(3, BankServerIdentityService.INSTANCE.current());
            statement.executeUpdate();
        }
    }

    public List<GroupMemberView> members(UUID groupId) throws SQLException {
        List<GroupMemberView> members = new ArrayList<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT m.player_uuid, m.role, m.permission_mask, m.last_active_millis
                       FROM economy_group_members m
                       JOIN economy_groups g ON g.id = m.group_id
                      WHERE m.group_id = ? AND g.server_uuid = ? AND g.status = 'ACTIVE'
                      ORDER BY CASE m.role WHEN 'OWNER' THEN 0 WHEN 'LEADER' THEN 1 WHEN 'VICE_LEADER' THEN 2 ELSE 3 END,
                               m.last_active_millis DESC
                     """)) {
            statement.setObject(1, groupId);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
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
                        AND g.server_uuid = ?
                      ORDER BY i.created_at DESC
                     """)) {
            statement.setObject(1, playerUuid);
            statement.setString(2, type.name());
            statement.setObject(3, BankServerIdentityService.INSTANCE.current());
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
