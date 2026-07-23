package br.com.economiamod.server.gold;

import br.com.economiamod.server.transaction.EconomyTransactionStatus;
import br.com.economiamod.server.transaction.EconomyTransactionType;
import br.com.economiamod.server.transaction.LedgerEntryType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;

public final class GoldExchangeWriter {
    public Optional<GoldExchangeResult> completed(Connection connection, String idempotencyKey) throws SQLException {
        String sql = "SELECT destination_account_id, source_account_id, amount FROM economy_transactions WHERE idempotency_key = ? AND status = 'COMPLETED'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                UUID accountId = resultSet.getObject("destination_account_id", UUID.class);
                if (accountId == null) {
                    accountId = resultSet.getObject("source_account_id", UUID.class);
                }
                return Optional.of(GoldExchangeResult.duplicateCompleted(resultSet.getLong("amount"), currentBalance(connection, accountId)));
            }
        }
    }

    public void insertTransaction(Connection connection, UUID transactionId, String idempotencyKey, EconomyTransactionType type, long amount, UUID playerUuid, UUID accountId, UUID commercialBlockId) throws SQLException {
        String sql = """
                INSERT INTO economy_transactions(
                    id, idempotency_key, transaction_type, status, amount, initiator_player_uuid,
                    source_account_id, destination_account_id, commercial_block_id, created_at, completed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            statement.setString(2, idempotencyKey);
            statement.setString(3, type.name());
            statement.setString(4, EconomyTransactionStatus.COMPLETED.name());
            statement.setLong(5, amount);
            statement.setObject(6, playerUuid);
            setNullableUuid(statement, 7, type == EconomyTransactionType.GOLD_REDEMPTION ? accountId : null);
            setNullableUuid(statement, 8, type == EconomyTransactionType.GOLD_MINT ? accountId : null);
            statement.setObject(9, commercialBlockId);
            statement.executeUpdate();
        }
    }

    public void insertLedger(Connection connection, UUID transactionId, UUID accountId, LedgerEntryType type, long amount, long before, long after) throws SQLException {
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
            statement.setString(4, type.name());
            statement.setLong(5, amount);
            statement.setLong(6, before);
            statement.setLong(7, after);
            statement.executeUpdate();
        }
    }

    public void insertGoldEntry(Connection connection, UUID transactionId, UUID playerUuid, String operationType, String itemId, long itemCount, long goldUnits, long unitValue, long moneyAmount, UUID commercialBlockId) throws SQLException {
        String sql = """
                INSERT INTO economy_gold_exchange_entries(
                    id, transaction_id, player_uuid, operation_type, gold_item_id, gold_item_count,
                    gold_nugget_units, unit_value, money_amount, commercial_block_id, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, transactionId);
            statement.setObject(3, playerUuid);
            statement.setString(4, operationType);
            statement.setString(5, itemId);
            statement.setLong(6, itemCount);
            statement.setLong(7, goldUnits);
            statement.setLong(8, unitValue);
            statement.setLong(9, moneyAmount);
            statement.setObject(10, commercialBlockId);
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

    private void setNullableUuid(PreparedStatement statement, int index, UUID value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.OTHER);
        } else {
            statement.setObject(index, value);
        }
    }
}
