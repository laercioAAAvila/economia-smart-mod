package br.com.economiamod.server.transaction;

import br.com.economiamod.server.config.EconomyServerConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public final class PaymentTransactionWriter {
    private final TransactionIdempotencyService idempotencyService = new TransactionIdempotencyService();

    public boolean completedTransactionExists(Connection connection, String idempotencyKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM economy_transactions WHERE idempotency_key = ? AND status = 'COMPLETED'")) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public IdempotencyCheck idempotencyCheck(Connection connection, String idempotencyKey, String fingerprint) throws SQLException {
        return idempotencyService.check(connection, idempotencyKey, fingerprint);
    }

    public void insertDebitTransaction(Connection connection, UUID transactionId, String idempotencyKey, long amount,
                                       UUID playerUuid, UUID sourceAccountId, UUID destinationAccountId, UUID cardId) throws SQLException {
        insertDebitTransaction(connection, transactionId, idempotencyKey, amount, playerUuid, sourceAccountId,
                destinationAccountId, cardId, null, TransactionOrigin.MINECRAFT);
    }

    public void insertDebitTransaction(Connection connection, UUID transactionId, String idempotencyKey, long amount,
                                       UUID playerUuid, UUID sourceAccountId, UUID destinationAccountId, UUID cardId,
                                       String fingerprint, TransactionOrigin origin) throws SQLException {
        insertPaymentTransaction(connection, transactionId, idempotencyKey, EconomyTransactionType.DEBIT_PURCHASE,
                amount, playerUuid, sourceAccountId, destinationAccountId, cardId, fingerprint, origin);
    }

    public void insertTransferTransaction(Connection connection, UUID transactionId, String idempotencyKey, long amount,
                                          UUID playerUuid, UUID sourceAccountId, UUID destinationAccountId) throws SQLException {
        insertTransferTransaction(connection, transactionId, idempotencyKey, amount, playerUuid, sourceAccountId,
                destinationAccountId, null, TransactionOrigin.MINECRAFT);
    }

    public void insertTransferTransaction(Connection connection, UUID transactionId, String idempotencyKey, long amount,
                                          UUID playerUuid, UUID sourceAccountId, UUID destinationAccountId,
                                          String fingerprint, TransactionOrigin origin) throws SQLException {
        insertPaymentTransaction(connection, transactionId, idempotencyKey, EconomyTransactionType.TRANSFER,
                amount, playerUuid, sourceAccountId, destinationAccountId, null, fingerprint, origin);
    }

    public void insertCreditTransaction(Connection connection, UUID transactionId, String idempotencyKey, long amount,
                                        UUID playerUuid, UUID sourceAccountId, UUID destinationAccountId, UUID cardId) throws SQLException {
        insertCreditTransaction(connection, transactionId, idempotencyKey, amount, playerUuid, sourceAccountId,
                destinationAccountId, cardId, null, TransactionOrigin.MINECRAFT);
    }

    public void insertCreditTransaction(Connection connection, UUID transactionId, String idempotencyKey, long amount,
                                        UUID playerUuid, UUID sourceAccountId, UUID destinationAccountId, UUID cardId,
                                        String fingerprint, TransactionOrigin origin) throws SQLException {
        insertPaymentTransaction(connection, transactionId, idempotencyKey, EconomyTransactionType.CREDIT_PURCHASE,
                amount, playerUuid, sourceAccountId, destinationAccountId, cardId, fingerprint, origin);
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

    public void insertCardPurchaseEntry(Connection connection, UUID transactionId, UUID cardId, long amount,
                                        String merchantName) throws SQLException {
        String sql = """
                INSERT INTO economy_card_entries(
                    id, card_id, transaction_id, entry_type, amount, remaining_amount, description,
                    merchant_name, interest_eligible_date, business_date, created_at
                )
                VALUES (?, ?, ?, 'PURCHASE', ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        LocalDate businessDate = LocalDate.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
        LocalDate eligibleDate = businessDate.plusDays(EconomyServerConfig.CREDIT_INTEREST_GRACE_DAYS.get());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, cardId);
            statement.setObject(3, transactionId);
            statement.setLong(4, amount);
            statement.setLong(5, amount);
            statement.setString(6, "Compra no credito");
            statement.setString(7, merchantName);
            statement.setObject(8, eligibleDate);
            statement.setObject(9, businessDate);
            statement.executeUpdate();
        }
    }

    private void insertPaymentTransaction(Connection connection, UUID transactionId, String idempotencyKey,
                                          EconomyTransactionType transactionType, long amount, UUID playerUuid,
                                          UUID sourceAccountId, UUID destinationAccountId, UUID cardId,
                                          String fingerprint, TransactionOrigin origin) throws SQLException {
        String sql = """
                INSERT INTO economy_transactions(
                    id, idempotency_key, transaction_type, status, amount, initiator_player_uuid,
                    source_account_id, destination_account_id, card_id, request_fingerprint, origin,
                    created_at, completed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
            statement.setObject(9, cardId);
            statement.setString(10, fingerprint);
            statement.setString(11, origin.name());
            statement.executeUpdate();
        }
    }
}
