package br.com.economiamod.server.interest;

import br.com.economiamod.common.credit.InterestMode;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.transaction.EconomyTransactionStatus;
import br.com.economiamod.server.transaction.EconomyTransactionType;
import br.com.economiamod.server.transaction.LedgerEntryType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public final class InterestAccrualWriter {
    public void increaseInterest(Connection connection, CardCandidate candidate, long amount, long remainderAfter) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE economy_cards SET credit_interest_outstanding = credit_interest_outstanding + ?, interest_rounding_remainder = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setLong(1, amount);
            statement.setLong(2, remainderAfter);
            statement.setObject(3, candidate.cardId());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("UPDATE economy_accounts SET credit_interest_outstanding = credit_interest_outstanding + ?, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ?")) {
            statement.setLong(1, amount);
            statement.setObject(2, candidate.accountId());
            statement.executeUpdate();
        }
    }

    public void updateRemainder(Connection connection, UUID cardId, long remainderAfter) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE economy_cards SET interest_rounding_remainder = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setLong(1, remainderAfter);
            statement.setObject(2, cardId);
            statement.executeUpdate();
        }
    }

    public void insertTransaction(Connection connection, UUID transactionId, CardCandidate candidate, LocalDate accrualDate, long amount) throws SQLException {
        String sql = """
                INSERT INTO economy_transactions(
                    id, idempotency_key, transaction_type, status, amount, source_account_id, card_id, created_at, completed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            statement.setString(2, "interest:%s:%s".formatted(candidate.cardId(), accrualDate));
            statement.setString(3, EconomyTransactionType.DAILY_INTEREST.name());
            statement.setString(4, EconomyTransactionStatus.COMPLETED.name());
            statement.setLong(5, amount);
            statement.setObject(6, candidate.accountId());
            statement.setObject(7, candidate.cardId());
            statement.executeUpdate();
        }
    }

    public void insertLedger(Connection connection, UUID transactionId, UUID accountId, long amount, long balance) throws SQLException {
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
            statement.setString(4, LedgerEntryType.CREDIT_INTEREST_INCREASE.name());
            statement.setLong(5, amount);
            statement.setLong(6, balance);
            statement.setLong(7, balance);
            statement.executeUpdate();
        }
    }

    public void insertCardInterestEntry(Connection connection, UUID transactionId, UUID cardId, long amount, LocalDate accrualDate) throws SQLException {
        String sql = """
                INSERT INTO economy_card_entries(
                    id, card_id, transaction_id, entry_type, amount, remaining_amount, description,
                    interest_eligible_date, business_date, created_at
                )
                VALUES (?, ?, ?, 'DAILY_INTEREST', ?, ?, 'Juros diarios', ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, cardId);
            statement.setObject(3, transactionId);
            statement.setLong(4, amount);
            statement.setLong(5, amount);
            statement.setObject(6, accrualDate);
            statement.setObject(7, accrualDate);
            statement.executeUpdate();
        }
    }

    public void insertAccrual(Connection connection, CardCandidate candidate, LocalDate accrualDate, InterestMode mode, long calculationBase, long remainderBefore, long interestAmount, long remainderAfter, UUID transactionId) throws SQLException {
        String sql = """
                INSERT INTO economy_interest_accruals(
                    id, card_id, account_id, accrual_date, interest_mode, rate_bps, calculation_base,
                    remainder_before, interest_amount, remainder_after, transaction_id, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, candidate.cardId());
            statement.setObject(3, candidate.accountId());
            statement.setObject(4, accrualDate);
            statement.setString(5, mode.name());
            statement.setInt(6, EconomyServerConfig.CREDIT_INTEREST_DAILY_RATE_BPS.get());
            statement.setLong(7, calculationBase);
            statement.setLong(8, remainderBefore);
            statement.setLong(9, interestAmount);
            statement.setLong(10, remainderAfter);
            statement.setObject(11, transactionId);
            statement.executeUpdate();
        }
    }
}

