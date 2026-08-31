package br.com.economiamod.server.group;

import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupSummary;
import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.MenuPaymentResult;
import br.com.economiamod.server.transaction.MenuPaymentService;
import br.com.economiamod.server.transaction.PaymentTransactionWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class ClaimLimitUpgradeService {
    private final GroupRepository groups = new GroupRepository();
    private final ClaimUpgradePricingService pricing = new ClaimUpgradePricingService();
    private final MenuPaymentService payments = new MenuPaymentService();
    private final PaymentTransactionWriter transactions = new PaymentTransactionWriter();

    public ClaimUpgradeQuote quote(UUID groupId) throws SQLException {
        GroupSummary group = groups.group(groupId).orElse(null);
        return group == null ? pricing.quote(EconomyServerConfig.CLAIM_MIN_CHUNKS.get())
                : pricing.quote(group.claimLimit());
    }

    public GroupOperationResult purchase(ServerPlayer player, UUID groupId, DirectPaymentMethod requestedMethod,
                                         ItemStack card, Container cash, int expectedCurrentLimit,
                                         long expectedAmount, UUID requestId) throws SQLException {
        UpgradeRecord record = prepare(player.getUUID(), groupId, requestedMethod,
                expectedCurrentLimit, expectedAmount, requestId == null ? UUID.randomUUID() : requestId);
        if (record == null) {
            return GroupOperationResult.denied("upgrade_unavailable");
        }
        if (record.completed()) {
            return GroupOperationResult.success(record.id());
        }

        MenuPaymentResult payment = payments.pay(player, record.method(), card, cash, record.amount(),
                "Upgrade de Claim", "claim-upgrade:" + record.id());
        if (!payment.success()) {
            if (!ambiguousPayment(payment)) {
                deletePending(record.id());
            }
            return GroupOperationResult.denied("payment_" + payment.code());
        }
        complete(record);
        return GroupOperationResult.success(record.id());
    }

    public void normalizeAll() throws SQLException {
        int minimum = EconomyServerConfig.CLAIM_MIN_CHUNKS.get();
        int maximum = EconomyServerConfig.CLAIM_MAX_CHUNKS.get();
        if (maximum < minimum) {
            return;
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_groups
                        SET claim_limit = GREATEST(?, LEAST(claim_limit, ?)), updated_at = CURRENT_TIMESTAMP
                      WHERE server_uuid = ? AND status = 'ACTIVE'
                        AND claim_limit <> GREATEST(?, LEAST(claim_limit, ?))
                     """)) {
            statement.setInt(1, minimum);
            statement.setInt(2, maximum);
            statement.setObject(3, BankServerIdentityService.INSTANCE.current());
            statement.setInt(4, minimum);
            statement.setInt(5, maximum);
            statement.executeUpdate();
        }
    }

    private UpgradeRecord prepare(UUID playerUuid, UUID groupId, DirectPaymentMethod method,
                                  int expectedCurrentLimit, long expectedAmount, UUID requestId)
            throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                GroupMembership membership = groups.membership(connection, playerUuid,
                        groupType(connection, groupId)).orElse(null);
                if (!authorized(membership) || !groupId.equals(membership.groupId())) {
                    connection.rollback();
                    return null;
                }
                int current = lockLimit(connection, groupId);
                ClaimUpgradeQuote quote = pricing.quote(current);
                UpgradeRecord completed = findById(connection, requestId);
                if (completed != null && completed.completed()) {
                    if (!completed.groupId().equals(groupId) || !completed.buyerUuid().equals(playerUuid)) {
                        connection.rollback();
                        return null;
                    }
                    connection.commit();
                    return completed;
                }
                UpgradeRecord pending = findPending(connection, groupId);
                if (pending != null) {
                    if (!pending.buyerUuid().equals(playerUuid)) {
                        connection.rollback();
                        return null;
                    }
                    boolean charged = transactions.completedTransactionExists(connection,
                            "claim-upgrade:" + pending.id());
                    boolean stillCurrent = quote.configurationValid() && !quote.maximumReached()
                            && pending.fromLimit() == quote.currentLimit()
                            && pending.toLimit() == quote.nextLimit()
                            && pending.percentageBasisPoints() == quote.percentageBasisPoints()
                            && pending.amount() == quote.price() && pending.method() == method;
                    if (charged || stillCurrent) {
                        connection.commit();
                        return pending;
                    }
                    deletePending(connection, pending.id());
                }
                if (expectedCurrentLimit != quote.currentLimit() || expectedAmount != quote.price()) {
                    connection.rollback();
                    return null;
                }
                if (!quote.configurationValid() || quote.maximumReached()) {
                    connection.rollback();
                    return null;
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO economy_claim_limit_upgrades(
                            id, group_id, buyer_player_uuid, from_limit, to_limit,
                            percentage_basis_points, amount, payment_method, status, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)
                        """)) {
                    statement.setObject(1, requestId);
                    statement.setObject(2, groupId);
                    statement.setObject(3, playerUuid);
                    statement.setInt(4, quote.currentLimit());
                    statement.setInt(5, quote.nextLimit());
                    statement.setInt(6, quote.percentageBasisPoints());
                    statement.setLong(7, quote.price());
                    statement.setString(8, method.name());
                    statement.executeUpdate();
                }
                connection.commit();
                return new UpgradeRecord(requestId, groupId, playerUuid, quote.currentLimit(),
                        quote.nextLimit(), quote.percentageBasisPoints(), quote.price(), method, false);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void complete(UpgradeRecord record) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                UpgradeRecord locked = findByIdForUpdate(connection, record.id());
                if (locked == null || locked.completed()) {
                    connection.commit();
                    return;
                }
                int current = lockLimit(connection, record.groupId());
                if (current < record.toLimit()) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            UPDATE economy_groups SET claim_limit = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                            """)) {
                        statement.setInt(1, record.toLimit());
                        statement.setObject(2, record.groupId());
                        statement.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE economy_claim_limit_upgrades
                           SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP
                         WHERE id = ?
                        """)) {
                    statement.setObject(1, record.id());
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

    private void deletePending(UUID id) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            deletePending(connection, id);
        }
    }

    private void deletePending(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM economy_claim_limit_upgrades WHERE id = ? AND status = 'PENDING'")) {
            statement.setObject(1, id);
            statement.executeUpdate();
        }
    }

    private br.com.economiamod.common.group.GroupType groupType(Connection connection, UUID groupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT group_type FROM economy_groups WHERE id = ? AND status = 'ACTIVE'")) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("group missing");
                }
                return br.com.economiamod.common.group.GroupType.valueOf(resultSet.getString(1));
            }
        }
    }

    private int lockLimit(Connection connection, UUID groupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT claim_limit FROM economy_groups WHERE id = ? AND status = 'ACTIVE' FOR UPDATE")) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("group missing");
                }
                return resultSet.getInt(1);
            }
        }
    }

    private boolean authorized(GroupMembership membership) {
        return membership != null && (membership.role() == GroupRole.OWNER || membership.role().leadsClan());
    }

    private boolean ambiguousPayment(MenuPaymentResult payment) {
        return "reconciliation_required".equals(payment.code())
                || "idempotency_conflict".equals(payment.code());
    }

    private UpgradeRecord findPending(Connection connection, UUID groupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, group_id, buyer_player_uuid, from_limit, to_limit,
                       percentage_basis_points, amount, payment_method, status
                  FROM economy_claim_limit_upgrades
                 WHERE group_id = ? AND status = 'PENDING'
                 ORDER BY created_at LIMIT 1
                """)) {
            statement.setObject(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? read(resultSet) : null;
            }
        }
    }

    private UpgradeRecord findById(Connection connection, UUID id) throws SQLException {
        return findById(connection, id, false);
    }

    private UpgradeRecord findByIdForUpdate(Connection connection, UUID id) throws SQLException {
        return findById(connection, id, true);
    }

    private UpgradeRecord findById(Connection connection, UUID id, boolean lock) throws SQLException {
        String sql = """
                SELECT id, group_id, buyer_player_uuid, from_limit, to_limit,
                       percentage_basis_points, amount, payment_method, status
                  FROM economy_claim_limit_upgrades WHERE id = ?
                """ + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? read(resultSet) : null;
            }
        }
    }

    private UpgradeRecord read(ResultSet resultSet) throws SQLException {
        return new UpgradeRecord(resultSet.getObject("id", UUID.class),
                resultSet.getObject("group_id", UUID.class),
                resultSet.getObject("buyer_player_uuid", UUID.class),
                resultSet.getInt("from_limit"), resultSet.getInt("to_limit"),
                resultSet.getInt("percentage_basis_points"), resultSet.getLong("amount"),
                DirectPaymentMethod.valueOf(resultSet.getString("payment_method")),
                "COMPLETED".equals(resultSet.getString("status")));
    }

    private record UpgradeRecord(UUID id, UUID groupId, UUID buyerUuid, int fromLimit, int toLimit,
                                 int percentageBasisPoints, long amount, DirectPaymentMethod method,
                                 boolean completed) {
    }
}
