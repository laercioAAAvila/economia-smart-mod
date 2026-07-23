package br.com.economiamod.server.reversal;

import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.EconomyTransactionStatus;
import br.com.economiamod.server.transaction.EconomyTransactionType;
import br.com.economiamod.server.transaction.LedgerEntryType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ReversalRepository {
    public Connection openConnection() throws SQLException {
        return EconomyDatabase.getConnection();
    }

    public Optional<UUID> completedByIdempotencyKey(Connection connection, String idempotencyKey) throws SQLException {
        String sql = "SELECT id FROM economy_transactions WHERE idempotency_key = ? AND status = 'COMPLETED'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getObject("id", UUID.class)) : Optional.empty();
            }
        }
    }

    public Optional<UUID> transactionByIdempotencyKey(Connection connection, String idempotencyKey) throws SQLException {
        String sql = "SELECT id FROM economy_transactions WHERE idempotency_key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getObject("id", UUID.class)) : Optional.empty();
            }
        }
    }

    public Optional<ReversalTarget> lockTarget(Connection connection, UUID transactionId) throws SQLException {
        String sql = "SELECT id, status, amount, initiator_player_uuid FROM economy_transactions WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ReversalTarget(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("status"),
                        resultSet.getLong("amount"),
                        resultSet.getObject("initiator_player_uuid", UUID.class)
                ));
            }
        }
    }

    public List<LedgerImpact> impacts(Connection connection, UUID transactionId) throws SQLException {
        String sql = """
                SELECT account_id, entry_type, amount, balance_before, balance_after
                  FROM economy_ledger_entries
                 WHERE transaction_id = ?
                 ORDER BY created_at, id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<LedgerImpact> impacts = new ArrayList<>();
                while (resultSet.next()) {
                    impacts.add(new LedgerImpact(
                            resultSet.getObject("account_id", UUID.class),
                            LedgerEntryType.valueOf(resultSet.getString("entry_type")),
                            resultSet.getLong("amount"),
                            resultSet.getLong("balance_before"),
                            resultSet.getLong("balance_after")
                    ));
                }
                return List.copyOf(impacts);
            }
        }
    }

    public long lockBalance(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT balance FROM economy_accounts WHERE id = ? FOR UPDATE")) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("balance");
            }
        }
    }

    public void updateBalance(Connection connection, UUID accountId, long balance) throws SQLException {
        String sql = "UPDATE economy_accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, balance);
            statement.setObject(2, accountId);
            statement.executeUpdate();
        }
    }

    public void markReversed(Connection connection, UUID transactionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE economy_transactions SET status = 'REVERSED' WHERE id = ?")) {
            statement.setObject(1, transactionId);
            statement.executeUpdate();
        }
    }

    public void insertReversalTransaction(Connection connection, UUID transactionId, String idempotencyKey, ReversalTarget target) throws SQLException {
        String sql = """
                INSERT INTO economy_transactions(
                    id, idempotency_key, transaction_type, status, amount, initiator_player_uuid, created_at, completed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            statement.setString(2, idempotencyKey);
            statement.setString(3, EconomyTransactionType.REVERSAL.name());
            statement.setString(4, EconomyTransactionStatus.COMPLETED.name());
            statement.setLong(5, target.amount());
            statement.setObject(6, target.initiatorPlayerUuid());
            statement.executeUpdate();
        }
    }

    public void insertLedger(Connection connection, UUID transactionId, UUID accountId, long amount, long before, long after) throws SQLException {
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
            statement.setString(4, LedgerEntryType.ADJUSTMENT.name());
            statement.setLong(5, amount);
            statement.setLong(6, before);
            statement.setLong(7, after);
            statement.executeUpdate();
        }
    }
}
