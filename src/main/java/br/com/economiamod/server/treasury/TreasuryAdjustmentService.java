package br.com.economiamod.server.treasury;

import br.com.economiamod.server.account.SystemAccountIds;
import br.com.economiamod.server.audit.AuditLogService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.EconomyTransactionType;
import br.com.economiamod.server.transaction.LedgerEntryType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

public final class TreasuryAdjustmentService {
    private final AuditLogService auditLogService = new AuditLogService();

    public TreasuryAdjustmentResult adjust(UUID adminPlayerUuid, long delta, String idempotencyKey) throws SQLException {
        if (delta == 0L) {
            throw new IllegalArgumentException("delta cannot be zero");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long before = lockBalance(connection);
                long after = before + delta;
                if (after < 0L) {
                    connection.rollback();
                    return TreasuryAdjustmentResult.insufficient(before);
                }
                UUID transactionId = UUID.randomUUID();
                updateBalance(connection, after);
                insertTransaction(connection, transactionId, idempotencyKey, Math.abs(delta), adminPlayerUuid, delta);
                insertLedger(connection, transactionId, Math.abs(delta), before, after);
                auditLogService.recordAdminChange(connection, adminPlayerUuid, "TREASURY_ADJUSTMENT", "ACCOUNT", SystemAccountIds.TREASURY, Long.toString(before), Long.toString(after));
                connection.commit();
                return TreasuryAdjustmentResult.completed(before, after);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private long lockBalance(Connection connection) throws SQLException {
        String sql = "SELECT balance FROM economy_accounts WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, SystemAccountIds.TREASURY);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("balance");
            }
        }
    }

    private void updateBalance(Connection connection, long balance) throws SQLException {
        String sql = """
                UPDATE economy_accounts
                   SET balance = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, balance);
            statement.setObject(2, SystemAccountIds.TREASURY);
            statement.executeUpdate();
        }
    }

    private void insertTransaction(Connection connection, UUID transactionId, String idempotencyKey, long amount, UUID adminPlayerUuid, long delta) throws SQLException {
        String sql = """
                INSERT INTO economy_transactions(
                    id, idempotency_key, transaction_type, status, amount, initiator_player_uuid,
                    source_account_id, destination_account_id, created_at, completed_at
                )
                VALUES (?, ?, ?, 'COMPLETED', ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            statement.setString(2, idempotencyKey);
            statement.setString(3, EconomyTransactionType.ADMIN_ADJUSTMENT.name());
            statement.setLong(4, amount);
            statement.setObject(5, adminPlayerUuid);
            setNullableUuid(statement, 6, delta < 0L ? SystemAccountIds.TREASURY : null);
            setNullableUuid(statement, 7, delta > 0L ? SystemAccountIds.TREASURY : null);
            statement.executeUpdate();
        }
    }

    private void insertLedger(Connection connection, UUID transactionId, long amount, long before, long after) throws SQLException {
        String sql = """
                INSERT INTO economy_ledger_entries(
                    id, transaction_id, account_id, entry_type, amount, balance_before, balance_after, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, transactionId);
            statement.setObject(3, SystemAccountIds.TREASURY);
            statement.setString(4, LedgerEntryType.ADJUSTMENT.name());
            statement.setLong(5, amount);
            statement.setLong(6, before);
            statement.setLong(7, after);
            statement.executeUpdate();
        }
    }

    private void setNullableUuid(PreparedStatement statement, int index, UUID value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.OTHER);
        } else {
            statement.setObject(index, value);
        }
    }
}
