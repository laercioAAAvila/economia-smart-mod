package br.com.economiamod.server.transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class AccountTransactionWriter {
    public Optional<FinancialOperationResult> findCompletedTransaction(Connection connection, String idempotencyKey) throws SQLException {
        String sql = """
                SELECT id, destination_account_id, source_account_id
                  FROM economy_transactions
                 WHERE idempotency_key = ?
                   AND status = 'COMPLETED'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                UUID transactionId = resultSet.getObject("id", UUID.class);
                UUID accountId = resultSet.getObject("destination_account_id", UUID.class);
                if (accountId == null) {
                    accountId = resultSet.getObject("source_account_id", UUID.class);
                }

                long balanceAfter = accountId == null ? 0L : currentBalance(connection, accountId);
                return Optional.of(FinancialOperationResult.duplicate(transactionId, balanceAfter));
            }
        }
    }

    public void insertTransaction(Connection connection, UUID transactionId, String idempotencyKey,
                                  EconomyTransactionType transactionType, long amount, UUID playerUuid,
                                  UUID sourceAccountId, UUID destinationAccountId) throws SQLException {
        insertTransaction(connection, transactionId, idempotencyKey, transactionType, amount, playerUuid,
                sourceAccountId, destinationAccountId, null, TransactionOrigin.MINECRAFT);
    }

    public void insertTransaction(Connection connection, UUID transactionId, String idempotencyKey,
                                  EconomyTransactionType transactionType, long amount, UUID playerUuid,
                                  UUID sourceAccountId, UUID destinationAccountId, String requestFingerprint,
                                  TransactionOrigin origin) throws SQLException {
        String sql = """
                INSERT INTO economy_transactions(
                    id, idempotency_key, transaction_type, status, amount, initiator_player_uuid,
                    source_account_id, destination_account_id, request_fingerprint, origin,
                    created_at, completed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            statement.setString(2, idempotencyKey);
            statement.setString(3, transactionType.name());
            statement.setString(4, EconomyTransactionStatus.COMPLETED.name());
            statement.setLong(5, amount);
            statement.setObject(6, playerUuid);
            statement.setObject(7, sourceAccountId);
            statement.setObject(8, destinationAccountId);
            statement.setString(9, requestFingerprint);
            statement.setString(10, origin.name());
            statement.executeUpdate();
        }
    }

    public void insertLedger(Connection connection, UUID transactionId, UUID accountId, LedgerEntryType entryType,
                             long amount, long balanceBefore, long balanceAfter) throws SQLException {
        String sql = """
                INSERT INTO economy_ledger_entries(
                    id, transaction_id, account_id, entry_type, amount, balance_before, balance_after, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, transactionId);
            statement.setObject(3, accountId);
            statement.setString(4, entryType.name());
            statement.setLong(5, amount);
            statement.setLong(6, balanceBefore);
            statement.setLong(7, balanceAfter);
            statement.executeUpdate();
        }
    }

    private long currentBalance(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT balance FROM economy_accounts WHERE id = ?")) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("balance") : 0L;
            }
        }
    }
}
