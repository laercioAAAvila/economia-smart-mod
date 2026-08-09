package br.com.economiamod.server.group;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClanLeadershipService {
    private final GroupService groupService = new GroupService();

    public void process(long activeMillis) throws SQLException {
        long inactiveThreshold = Math.max(0L, activeMillis - Duration.ofDays(
                EconomyServerConfig.CLAN_LEADERSHIP_INACTIVITY_DAYS.get()).toMillis());
        for (InactiveClan clan : inactiveClans(inactiveThreshold)) {
            processClan(clan, activeMillis);
        }
    }

    private List<InactiveClan> inactiveClans(long inactiveThreshold) throws SQLException {
        List<InactiveClan> clans = new ArrayList<>();
        String sql = """
                SELECT g.id, g.leader_player_uuid
                  FROM economy_groups g
                  JOIN economy_group_members leader
                    ON leader.group_id = g.id AND leader.player_uuid = g.leader_player_uuid
                  LEFT JOIN economy_group_members vice
                    ON vice.group_id = g.id AND vice.player_uuid = g.vice_leader_player_uuid
                 WHERE g.group_type = 'CLAN' AND g.status = 'ACTIVE'
                   AND leader.last_active_millis <= ?
                   AND (g.vice_leader_player_uuid IS NULL OR vice.last_active_millis <= ?)
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, inactiveThreshold);
            statement.setLong(2, inactiveThreshold);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    clans.add(new InactiveClan(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getObject("leader_player_uuid", UUID.class)));
                }
            }
        }
        return clans;
    }

    private void processClan(InactiveClan clan, long activeMillis) throws SQLException {
        long candidateThreshold = Math.max(0L, activeMillis - Duration.ofDays(
                EconomyServerConfig.CLAN_LEADERSHIP_CANDIDATE_ACTIVE_DAYS.get()).toMillis());
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                UUID candidate = findCandidate(connection, clan.id(), candidateThreshold);
                if (candidate == null) {
                    connection.rollback();
                    GroupOperationResult result = groupService.close(
                            clan.leaderUuid(), clan.id(), GroupType.CLAN, true);
                    if (!result.success() && "balance_destination_required".equals(result.code())) {
                        EconomiaMod.LOGGER.warn(
                                "Clã {} sem liderança ativa não foi encerrado porque possui saldo e o destino ainda não foi configurado.",
                                clan.id());
                    }
                    return;
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE economy_group_members SET role = 'MEMBER', updated_at = CURRENT_TIMESTAMP
                         WHERE group_id = ? AND role IN ('LEADER', 'VICE_LEADER')
                        """)) {
                    statement.setObject(1, clan.id());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE economy_group_members SET role = 'LEADER', updated_at = CURRENT_TIMESTAMP
                         WHERE group_id = ? AND player_uuid = ?
                        """)) {
                    statement.setObject(1, clan.id());
                    statement.setObject(2, candidate);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE economy_groups
                           SET leader_player_uuid = ?, vice_leader_player_uuid = NULL, updated_at = CURRENT_TIMESTAMP
                         WHERE id = ? AND status = 'ACTIVE'
                        """)) {
                    statement.setObject(1, candidate);
                    statement.setObject(2, clan.id());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE economy_claim_invoices i
                           SET debtor_player_uuid = ?
                          FROM economy_claim_territories t
                         WHERE i.territory_id = t.id
                           AND t.claim_type = 'CLAN'
                           AND t.group_id = ?
                           AND i.status = 'PENDING'
                           AND i.invoice_type IN ('LAND', 'ANCHOR')
                        """)) {
                    statement.setObject(1, candidate);
                    statement.setObject(2, clan.id());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private UUID findCandidate(Connection connection, UUID groupId, long threshold) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_uuid FROM economy_group_members
                 WHERE group_id = ? AND role = 'MEMBER' AND last_active_millis >= ?
                 ORDER BY last_active_millis DESC, joined_at ASC LIMIT 1 FOR UPDATE
                """)) {
            statement.setObject(1, groupId);
            statement.setLong(2, threshold);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject("player_uuid", UUID.class) : null;
            }
        }
    }

    private record InactiveClan(UUID id, UUID leaderUuid) {
    }
}
