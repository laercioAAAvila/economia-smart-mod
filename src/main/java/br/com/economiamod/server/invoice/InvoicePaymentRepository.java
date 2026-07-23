package br.com.economiamod.server.invoice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InvoicePaymentRepository {
    public Optional<InvoicePaymentResult> completedTransaction(Connection connection, String idempotencyKey) throws SQLException {
        String sql = "SELECT source_account_id, amount FROM economy_transactions WHERE idempotency_key = ? AND status = 'COMPLETED'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                UUID accountId = resultSet.getObject("source_account_id", UUID.class);
                long paidAmount = resultSet.getLong("amount");
                return Optional.of(InvoicePaymentResult.duplicateCompleted(paidAmount, currentBalance(connection, accountId)));
            }
        }
    }

    public Optional<AccountDebtSnapshot> lockAccount(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT status, balance, credit_principal_outstanding, credit_interest_outstanding
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AccountDebtSnapshot(
                        resultSet.getString("status"),
                        resultSet.getLong("balance"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                ));
            }
        }
    }

    public void lockCards(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM economy_cards WHERE account_id = ? ORDER BY id FOR UPDATE")) {
            statement.setObject(1, accountId);
            try (ResultSet ignored = statement.executeQuery()) {
            }
        }
    }

    public List<InvoiceEntryPayment> entriesToPay(Connection connection, UUID accountId, long amount) throws SQLException {
        String sql = """
                SELECT e.id, e.card_id, e.entry_type, e.remaining_amount
                  FROM economy_card_entries e
                  JOIN economy_cards c ON c.id = e.card_id
                 WHERE c.account_id = ?
                   AND e.remaining_amount > 0
                 ORDER BY CASE WHEN e.entry_type = 'DAILY_INTEREST' THEN 0 ELSE 1 END, e.created_at, e.id
                 FOR UPDATE OF e
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return collectPayments(resultSet, amount);
            }
        }
    }

    private List<InvoiceEntryPayment> collectPayments(ResultSet resultSet, long amount) throws SQLException {
        List<InvoiceEntryPayment> entries = new ArrayList<>();
        long remaining = amount;
        while (resultSet.next() && remaining > 0L) {
            long payAmount = Math.min(resultSet.getLong("remaining_amount"), remaining);
            entries.add(new InvoiceEntryPayment(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("card_id", UUID.class),
                    "DAILY_INTEREST".equals(resultSet.getString("entry_type")),
                    payAmount
            ));
            remaining -= payAmount;
        }
        return entries;
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

