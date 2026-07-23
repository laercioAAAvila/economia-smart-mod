package br.com.economiamod.server.invoice;

import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.transaction.EconomyTransactionStatus;
import br.com.economiamod.server.transaction.EconomyTransactionType;
import br.com.economiamod.server.transaction.LedgerEntryType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public final class InvoicePaymentWriter {
    public void reduceEntriesAndCards(Connection connection, List<InvoiceEntryPayment> entries) throws SQLException {
        for (InvoiceEntryPayment entry : entries) {
            reduceCardEntry(connection, entry.entryId(), entry.payAmount());
            reduceCardDebt(connection, entry.cardId(), entry.isInterest(), entry.payAmount());
        }
    }

    public void reduceAccountDebtAndBalance(Connection connection, UUID accountId, long balanceAfter, long principalPaid, long interestPaid) throws SQLException {
        String sql = """
                UPDATE economy_accounts
                   SET balance = ?,
                       configured_credit_limit = LEAST(configured_credit_limit, ?),
                       credit_principal_outstanding = credit_principal_outstanding - ?,
                       credit_interest_outstanding = credit_interest_outstanding - ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, balanceAfter);
            statement.setLong(2, CreditLimitPolicy.limitForBalance(balanceAfter));
            statement.setLong(3, principalPaid);
            statement.setLong(4, interestPaid);
            statement.setObject(5, accountId);
            statement.executeUpdate();
        }
    }

    public void insertTransaction(Connection connection, UUID transactionId, String idempotencyKey, long amount, UUID playerUuid, UUID accountId) throws SQLException {
        String sql = """
                INSERT INTO economy_transactions(
                    id, idempotency_key, transaction_type, status, amount, initiator_player_uuid,
                    source_account_id, created_at, completed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            statement.setString(2, idempotencyKey);
            statement.setString(3, EconomyTransactionType.INVOICE_PAYMENT.name());
            statement.setString(4, EconomyTransactionStatus.COMPLETED.name());
            statement.setLong(5, amount);
            statement.setObject(6, playerUuid);
            statement.setObject(7, accountId);
            statement.executeUpdate();
        }
    }

    public void insertLedger(Connection connection, UUID transactionId, UUID accountId, LedgerEntryType entryType, long amount, long balanceBefore, long balanceAfter) throws SQLException {
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

    public void insertPaymentEntries(Connection connection, UUID transactionId, List<InvoiceEntryPayment> entries) throws SQLException {
        String sql = """
                INSERT INTO economy_card_entries(
                    id, card_id, transaction_id, entry_type, amount, remaining_amount,
                    description, business_date, created_at, paid_at
                )
                VALUES (?, ?, ?, 'PAYMENT', ?, 0, 'Pagamento de fatura', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        LocalDate today = LocalDate.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (InvoiceEntryPayment entry : entries) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, entry.cardId());
                statement.setObject(3, transactionId);
                statement.setLong(4, entry.payAmount());
                statement.setObject(5, today);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void reduceCardEntry(Connection connection, UUID entryId, long amount) throws SQLException {
        String sql = """
                UPDATE economy_card_entries
                   SET remaining_amount = remaining_amount - ?,
                       paid_at = CASE WHEN remaining_amount - ? = 0 THEN CURRENT_TIMESTAMP ELSE paid_at END
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, amount);
            statement.setLong(2, amount);
            statement.setObject(3, entryId);
            statement.executeUpdate();
        }
    }

    private void reduceCardDebt(Connection connection, UUID cardId, boolean interest, long amount) throws SQLException {
        String column = interest ? "credit_interest_outstanding" : "credit_principal_outstanding";
        String sql = "UPDATE economy_cards SET " + column + " = " + column + " - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, amount);
            statement.setObject(2, cardId);
            statement.executeUpdate();
        }
    }
}
