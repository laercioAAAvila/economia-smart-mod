package br.com.economiamod.server.group;

import br.com.economiamod.common.account.AccountType;
import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.group.TerritoryPermission;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class GroupService {
    private final GroupRepository repository = new GroupRepository();

    public GroupOperationResult invite(ServerPlayer actor, String targetName, GroupType type) throws SQLException {
        if (type == GroupType.PRIVATE_PROPERTY) {
            return GroupOperationResult.denied("private_invite_at_claim");
        }
        if (targetName == null || targetName.isBlank() || actor.getServer() == null) {
            return GroupOperationResult.denied("target_offline");
        }
        ServerPlayer target = actor.getServer().getPlayerList().getPlayerByName(targetName.strip());
        if (target == null) {
            return GroupOperationResult.denied("target_offline");
        }
        return invite(actor.getUUID(), target.getUUID(), type);
    }

    public GroupOperationResult create(UUID playerUuid, GroupType type, String requestedName, long activeMillis) throws SQLException {
        if (type == GroupType.PRIVATE_PROPERTY) {
            return GroupOperationResult.denied("private_property_created_by_claim");
        }
        String name = requestedName == null ? "" : requestedName.strip();
        if (!validName(name)) {
            return GroupOperationResult.denied("invalid_name");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (repository.membership(connection, playerUuid, type).isPresent()) {
                    connection.rollback();
                    return GroupOperationResult.denied("already_member");
                }
                UUID groupId = UUID.randomUUID();
                UUID accountId = createAccount(connection, type == GroupType.CLAN ? AccountType.CLAN_TREASURY : AccountType.PRIVATE_PROPERTY, name);
                UUID supportAccountId = type == GroupType.CLAN ? createAccount(connection, AccountType.CLAN_SUPPORT, name + " - Fundo") : null;
                insertGroup(connection, groupId, type, name, playerUuid, accountId, supportAccountId);
                insertMember(connection, groupId, type, playerUuid, GroupRole.LEADER, defaultMask(type), activeMillis);
                connection.commit();
                return GroupOperationResult.success(groupId);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                if (isConstraintViolation(exception)) {
                    return GroupOperationResult.denied("duplicate_or_invalid");
                }
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public UUID ensurePrivatePropertyPortfolio(UUID playerUuid, long activeMillis) throws SQLException {
        GroupMembership existing = repository.membership(playerUuid, GroupType.PRIVATE_PROPERTY).orElse(null);
        if (existing != null && existing.role() == GroupRole.OWNER) {
            return existing.groupId();
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                existing = repository.membership(connection, playerUuid, GroupType.PRIVATE_PROPERTY).orElse(null);
                if (existing != null && existing.role() == GroupRole.OWNER) {
                    connection.commit();
                    return existing.groupId();
                }
                if (existing != null) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            DELETE FROM economy_group_members
                             WHERE player_uuid = ? AND group_type = 'PRIVATE_PROPERTY'
                            """)) {
                        statement.setObject(1, playerUuid);
                        statement.executeUpdate();
                    }
                }
                UUID groupId = UUID.randomUUID();
                String name = "Propriedade " + playerUuid.toString().substring(0, 8);
                UUID accountId = createAccount(connection, AccountType.PRIVATE_PROPERTY, name);
                insertGroup(connection, groupId, GroupType.PRIVATE_PROPERTY, name, playerUuid, accountId, null);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE economy_groups SET claim_limit = ? WHERE id = ?")) {
                    statement.setInt(1, Math.max(EconomyServerConfig.PRIVATE_PROPERTY_INITIAL_CLAIM_LIMIT.get(),
                            EconomyServerConfig.PRIVATE_PROPERTY_MAX_TERRITORIES.get()));
                    statement.setObject(2, groupId);
                    statement.executeUpdate();
                }
                insertMember(connection, groupId, GroupType.PRIVATE_PROPERTY, playerUuid, GroupRole.OWNER,
                        defaultMask(GroupType.PRIVATE_PROPERTY), activeMillis);
                connection.commit();
                return groupId;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public GroupOperationResult invite(UUID actorUuid, UUID targetUuid, GroupType type) throws SQLException {
        if (type == GroupType.PRIVATE_PROPERTY) {
            return GroupOperationResult.denied("private_invite_at_claim");
        }
        GroupMembership actor = repository.membership(actorUuid, type).orElse(null);
        if (actor == null || !canInvite(actor)) {
            return GroupOperationResult.denied("not_allowed");
        }
        if (repository.membership(targetUuid, type).isPresent()) {
            return GroupOperationResult.denied("target_already_member");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            int limit = type == GroupType.CLAN ? EconomyServerConfig.CLAN_MEMBER_LIMIT.get() : EconomyServerConfig.PRIVATE_PROPERTY_MEMBER_LIMIT.get();
            if (repository.memberCount(connection, actor.groupId()) >= limit) {
                return GroupOperationResult.denied("member_limit");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO economy_group_invites(
                        id, group_id, group_type, invited_player_uuid, invited_by_player_uuid, status, created_at
                    ) VALUES (?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)
                    """)) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, actor.groupId());
                statement.setString(3, type.name());
                statement.setObject(4, targetUuid);
                statement.setObject(5, actorUuid);
                statement.executeUpdate();
            } catch (SQLException exception) {
                if (isConstraintViolation(exception)) {
                    return GroupOperationResult.denied("invite_exists");
                }
                throw exception;
            }
            return GroupOperationResult.success(actor.groupId());
        }
    }

    public GroupOperationResult respondToInvite(UUID playerUuid, UUID inviteId, boolean accept, long activeMillis) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Invite invite = lockInvite(connection, inviteId, playerUuid);
                if (invite == null) {
                    connection.rollback();
                    return GroupOperationResult.denied("invite_not_found");
                }
                if (!accept) {
                    updateInvite(connection, inviteId, "DECLINED");
                    connection.commit();
                    return GroupOperationResult.success(invite.groupId());
                }
                if (repository.membership(connection, playerUuid, invite.type()).isPresent()) {
                    connection.rollback();
                    return GroupOperationResult.denied("already_member");
                }
                int limit = invite.type() == GroupType.CLAN ? EconomyServerConfig.CLAN_MEMBER_LIMIT.get() : EconomyServerConfig.PRIVATE_PROPERTY_MEMBER_LIMIT.get();
                if (repository.memberCount(connection, invite.groupId()) >= limit) {
                    connection.rollback();
                    return GroupOperationResult.denied("member_limit");
                }
                insertMember(connection, invite.groupId(), invite.type(), playerUuid, GroupRole.MEMBER, defaultMask(invite.type()), activeMillis);
                updateInvite(connection, inviteId, "ACCEPTED");
                connection.commit();
                return GroupOperationResult.success(invite.groupId());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public GroupOperationResult updatePermissions(UUID actorUuid, UUID memberUuid, int permissionMask) throws SQLException {
        GroupMembership actor = repository.membership(actorUuid, GroupType.CLAN).orElse(null);
        GroupMembership member = actor == null ? null : repository.membership(memberUuid, actor.groupId()).orElse(null);
        if (actor == null || member == null || !actor.role().leadsClan() || member.role() != GroupRole.MEMBER) {
            return GroupOperationResult.denied("not_allowed");
        }
        int allowedMask = TerritoryPermission.USE.bit() | TerritoryPermission.DESTROY.bit() | TerritoryPermission.PLACE.bit();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_group_members SET permission_mask = ?, updated_at = CURRENT_TIMESTAMP
                      WHERE group_id = ? AND player_uuid = ? AND role = 'MEMBER'
                     """)) {
            statement.setInt(1, permissionMask & allowedMask);
            statement.setObject(2, actor.groupId());
            statement.setObject(3, memberUuid);
            statement.executeUpdate();
        }
        return GroupOperationResult.success(actor.groupId());
    }

    public GroupOperationResult appointViceLeader(UUID leaderUuid, UUID memberUuid) throws SQLException {
        GroupMembership leader = repository.membership(leaderUuid, GroupType.CLAN).orElse(null);
        GroupMembership member = leader == null ? null : repository.membership(memberUuid, leader.groupId()).orElse(null);
        if (leader == null || leader.role() != GroupRole.LEADER || member == null || member.role() == GroupRole.LEADER) {
            return GroupOperationResult.denied("not_allowed");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE economy_group_members SET role = 'MEMBER', updated_at = CURRENT_TIMESTAMP WHERE group_id = ? AND role = 'VICE_LEADER'")) {
                    statement.setObject(1, leader.groupId());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE economy_group_members SET role = 'VICE_LEADER', updated_at = CURRENT_TIMESTAMP WHERE group_id = ? AND player_uuid = ?")) {
                    statement.setObject(1, leader.groupId());
                    statement.setObject(2, memberUuid);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE economy_groups SET vice_leader_player_uuid = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    statement.setObject(1, memberUuid);
                    statement.setObject(2, leader.groupId());
                    statement.executeUpdate();
                }
                connection.commit();
                return GroupOperationResult.success(leader.groupId());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public GroupOperationResult removeMember(UUID actorUuid, UUID memberUuid, GroupType type) throws SQLException {
        GroupMembership actor = repository.membership(actorUuid, type).orElse(null);
        GroupMembership member = actor == null ? null : repository.membership(memberUuid, actor.groupId()).orElse(null);
        if (actor == null || member == null || member.role() == GroupRole.LEADER || member.role() == GroupRole.OWNER || !canInvite(actor)
                || (actor.role() == GroupRole.VICE_LEADER && member.role() != GroupRole.MEMBER)) {
            return GroupOperationResult.denied("not_allowed");
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM economy_group_members WHERE group_id = ? AND player_uuid = ?")) {
            statement.setObject(1, actor.groupId());
            statement.setObject(2, memberUuid);
            statement.executeUpdate();
        }
        if (member.role() == GroupRole.VICE_LEADER) {
            clearViceLeader(actor.groupId());
        }
        return GroupOperationResult.success(actor.groupId());
    }

    public GroupOperationResult leave(UUID playerUuid, GroupType type) throws SQLException {
        GroupMembership member = repository.membership(playerUuid, type).orElse(null);
        if (member == null || member.role() == GroupRole.LEADER || member.role() == GroupRole.OWNER) {
            return GroupOperationResult.denied(member == null ? "not_member" : "leader_must_close");
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM economy_group_members WHERE group_id = ? AND player_uuid = ?")) {
            statement.setObject(1, member.groupId());
            statement.setObject(2, playerUuid);
            statement.executeUpdate();
        }
        if (member.role() == GroupRole.VICE_LEADER) {
            clearViceLeader(member.groupId());
        }
        return GroupOperationResult.success(member.groupId());
    }

    public GroupOperationResult rename(UUID actorUuid, GroupType type, String requestedName) throws SQLException {
        GroupMembership actor = repository.membership(actorUuid, type).orElse(null);
        String name = requestedName == null ? "" : requestedName.strip();
        if (actor == null || (actor.role() != GroupRole.LEADER && actor.role() != GroupRole.OWNER
                && !(type == GroupType.CLAN && actor.role() == GroupRole.VICE_LEADER))) {
            return GroupOperationResult.denied("not_allowed");
        }
        if (!validName(name)) {
            return GroupOperationResult.denied("invalid_name");
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_groups SET name = ?, normalized_name = ?, updated_at = CURRENT_TIMESTAMP
                      WHERE id = ? AND status = 'ACTIVE'
                     """)) {
            statement.setString(1, name);
            statement.setString(2, normalize(name));
            statement.setObject(3, actor.groupId());
            statement.executeUpdate();
            return GroupOperationResult.success(actor.groupId());
        } catch (SQLException exception) {
            return isConstraintViolation(exception) ? GroupOperationResult.denied("duplicate_name") : throwSql(exception);
        }
    }

    public GroupOperationResult updateVisitorShopPermissions(UUID leaderUuid, GroupType type,
                                                              boolean buyShop, boolean sellShop) throws SQLException {
        GroupMembership leader = repository.membership(leaderUuid, type).orElse(null);
        if (leader == null || (leader.role() != GroupRole.LEADER && leader.role() != GroupRole.OWNER)) {
            return GroupOperationResult.denied("not_allowed");
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_groups
                        SET visitor_use_buy_shop = ?, visitor_use_sell_shop = ?, updated_at = CURRENT_TIMESTAMP
                      WHERE id = ? AND status = 'ACTIVE'
                     """)) {
            statement.setBoolean(1, buyShop);
            statement.setBoolean(2, sellShop);
            statement.setObject(3, leader.groupId());
            statement.executeUpdate();
        }
        return GroupOperationResult.success(leader.groupId());
    }

    public GroupOperationResult close(UUID actorUuid, UUID requestedGroupId, GroupType type, boolean authorizedAdministrator) throws SQLException {
        GroupMembership actor = repository.membership(actorUuid, type).orElse(null);
        if (!authorizedAdministrator && (actor == null || (actor.role() != GroupRole.LEADER && actor.role() != GroupRole.OWNER))) {
            return GroupOperationResult.denied("not_allowed");
        }
        UUID groupId = authorizedAdministrator ? requestedGroupId : actor.groupId();
        if (groupId == null) {
            return GroupOperationResult.denied("group_required");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (hasActiveTerritories(connection, groupId)) {
                    connection.rollback();
                    return GroupOperationResult.denied("active_territories");
                }
                if (hasCollectiveBalance(connection, groupId)) {
                    connection.rollback();
                    return GroupOperationResult.denied("balance_destination_required");
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM economy_claims WHERE group_id = ?")) {
                    statement.setObject(1, groupId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE economy_claim_anchors SET active = FALSE, removed_at = CURRENT_TIMESTAMP
                         WHERE group_id = ? AND removed_at IS NULL
                        """)) {
                    statement.setObject(1, groupId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE economy_group_invites SET status = 'CANCELLED', responded_at = CURRENT_TIMESTAMP
                         WHERE group_id = ? AND status = 'PENDING'
                        """)) {
                    statement.setObject(1, groupId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM economy_group_members WHERE group_id = ?")) {
                    statement.setObject(1, groupId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE economy_groups SET status = 'CLOSED', closed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                         WHERE id = ? AND status = 'ACTIVE'
                        """)) {
                    statement.setObject(1, groupId);
                    statement.executeUpdate();
                }
                connection.commit();
                return GroupOperationResult.success(groupId);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void clearViceLeader(UUID groupId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE economy_groups SET vice_leader_player_uuid = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setObject(1, groupId);
            statement.executeUpdate();
        }
    }

    private boolean hasCollectiveBalance(Connection connection, UUID groupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM economy_groups g
                JOIN economy_accounts a ON a.id = g.account_id OR a.id = g.support_account_id
                WHERE g.id = ? AND a.balance <> 0 LIMIT 1
                """)) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean hasActiveTerritories(Connection connection, UUID groupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM economy_claim_territories WHERE group_id = ? LIMIT 1
                """)) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private GroupOperationResult throwSql(SQLException exception) throws SQLException {
        throw exception;
    }

    private void insertGroup(Connection connection, UUID groupId, GroupType type, String name, UUID leaderUuid,
                             UUID accountId, UUID supportAccountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_groups(
                    id, group_type, name, normalized_name, leader_player_uuid, vice_leader_player_uuid,
                    account_id, support_account_id, claim_limit, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, type.name());
            statement.setString(3, name);
            statement.setString(4, normalize(name));
            statement.setObject(5, leaderUuid);
            statement.setObject(6, accountId);
            statement.setObject(7, supportAccountId);
            statement.setInt(8, type == GroupType.CLAN ? EconomyServerConfig.CLAN_INITIAL_CLAIM_LIMIT.get() : EconomyServerConfig.PRIVATE_PROPERTY_INITIAL_CLAIM_LIMIT.get());
            statement.executeUpdate();
        }
    }

    private UUID createAccount(Connection connection, AccountType accountType, String displayName) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_accounts(
                    id, player_uuid, username, username_normalized, password_hash, password_salt,
                    password_algorithm, account_type, status, balance, configured_credit_limit,
                    credit_principal_outstanding, credit_interest_outstanding, created_at, updated_at,
                    last_login_at, version
                ) VALUES (?, NULL, ?, NULL, NULL, NULL, NULL, ?, 'ACTIVE', 0, 0, 0, 0,
                          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0)
                """)) {
            statement.setObject(1, id);
            statement.setString(2, displayName);
            statement.setString(3, accountType.name());
            statement.executeUpdate();
        }
        return id;
    }

    private void insertMember(Connection connection, UUID groupId, GroupType type, UUID playerUuid,
                              GroupRole role, int permissionMask, long activeMillis) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_group_members(
                    group_id, group_type, player_uuid, role, permission_mask,
                    last_active_millis, joined_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            statement.setObject(1, groupId);
            statement.setString(2, type.name());
            statement.setObject(3, playerUuid);
            statement.setString(4, role.name());
            statement.setInt(5, permissionMask);
            statement.setLong(6, Math.max(0L, activeMillis));
            statement.executeUpdate();
        }
    }

    private Invite lockInvite(Connection connection, UUID inviteId, UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT group_id, group_type FROM economy_group_invites
                 WHERE id = ? AND invited_player_uuid = ? AND status = 'PENDING' FOR UPDATE
                """)) {
            statement.setObject(1, inviteId);
            statement.setObject(2, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Invite(resultSet.getObject("group_id", UUID.class), GroupType.valueOf(resultSet.getString("group_type"))) : null;
            }
        }
    }

    private void updateInvite(Connection connection, UUID inviteId, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE economy_group_invites SET status = ?, responded_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setString(1, status);
            statement.setObject(2, inviteId);
            statement.executeUpdate();
        }
    }

    private boolean canInvite(GroupMembership membership) {
        return membership.role() == GroupRole.LEADER
                || membership.role() == GroupRole.OWNER
                || (membership.groupType() == GroupType.CLAN && membership.role() == GroupRole.VICE_LEADER);
    }

    private int defaultMask(GroupType type) {
        return type == GroupType.CLAN ? TerritoryPermission.clanDefaultMask() : TerritoryPermission.privatePropertyDefaultMask();
    }

    private boolean validName(String name) {
        int length = name.codePointCount(0, name.length());
        return length >= EconomyServerConfig.GROUP_NAME_MIN_LENGTH.get()
                && length <= EconomyServerConfig.GROUP_NAME_MAX_LENGTH.get()
                && name.codePoints().allMatch(codePoint -> Character.isLetterOrDigit(codePoint)
                || codePoint == ' ' || codePoint == '_' || codePoint == '-');
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private boolean isConstraintViolation(Exception exception) {
        if (!(exception instanceof SQLException sqlException) || sqlException.getSQLState() == null) {
            return false;
        }
        return sqlException.getSQLState().startsWith("23");
    }

    private record Invite(UUID groupId, GroupType type) {
    }
}
