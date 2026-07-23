package br.com.economiamod.server.interest;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InterestAccrualRepository {
    public List<CardCandidate> candidates() throws SQLException {
        return candidatesAfter(null, null, Integer.MAX_VALUE);
    }

    public List<CardCandidate> candidatesAfter(UUID lastAccountId, UUID lastCardId, int limit) throws SQLException {
        if (lastAccountId == null || lastCardId == null) {
            return firstCandidates(limit);
        }

        String sql = """
                SELECT c.id, c.account_id
                  FROM economy_cards c
                  JOIN economy_accounts a ON a.id = c.account_id
                 WHERE c.card_type IN ('CREDIT', 'DEBIT_CREDIT')
                   AND c.status IN ('ACTIVE', 'DISABLED')
                   AND a.status = 'ACTIVE'
                   AND (c.credit_principal_outstanding + c.credit_interest_outstanding) > 0
                   AND (c.account_id > ? OR (c.account_id = ? AND c.id > ?))
                 ORDER BY c.account_id, c.id
                 LIMIT ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, lastAccountId);
            statement.setObject(2, lastAccountId);
            statement.setObject(3, lastCardId);
            statement.setInt(4, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                return readCandidates(resultSet);
            }
        }
    }

    private List<CardCandidate> firstCandidates(int limit) throws SQLException {
        String sql = """
                SELECT c.id, c.account_id
                  FROM economy_cards c
                  JOIN economy_accounts a ON a.id = c.account_id
                 WHERE c.card_type IN ('CREDIT', 'DEBIT_CREDIT')
                   AND c.status IN ('ACTIVE', 'DISABLED')
                   AND a.status = 'ACTIVE'
                   AND (c.credit_principal_outstanding + c.credit_interest_outstanding) > 0
                 ORDER BY c.account_id, c.id
                 LIMIT ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                return readCandidates(resultSet);
            }
        }
    }

    private List<CardCandidate> readCandidates(ResultSet resultSet) throws SQLException {
        List<CardCandidate> cards = new ArrayList<>();
        while (resultSet.next()) {
            cards.add(new CardCandidate(resultSet.getObject("id", UUID.class), resultSet.getObject("account_id", UUID.class)));
        }
        return cards;
    }

    public void lockAccount(Connection connection, UUID accountId) throws SQLException {
        lock(connection, "SELECT id FROM economy_accounts WHERE id = ? FOR UPDATE", accountId);
    }

    public void lockCard(Connection connection, UUID cardId) throws SQLException {
        lock(connection, "SELECT id FROM economy_cards WHERE id = ? FOR UPDATE", cardId);
    }

    public boolean alreadyAccrued(Connection connection, UUID cardId, LocalDate accrualDate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM economy_interest_accruals WHERE card_id = ? AND accrual_date = ?")) {
            statement.setObject(1, cardId);
            statement.setObject(2, accrualDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Optional<InterestCardDebt> cardDebt(Connection connection, UUID cardId) throws SQLException {
        String sql = "SELECT credit_interest_outstanding, interest_rounding_remainder FROM economy_cards WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new InterestCardDebt(resultSet.getLong("credit_interest_outstanding"), resultSet.getLong("interest_rounding_remainder")));
            }
        }
    }

    public Optional<InterestAccountDebt> accountDebt(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT status, balance FROM economy_accounts WHERE id = ?")) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new InterestAccountDebt(resultSet.getString("status"), resultSet.getLong("balance")));
            }
        }
    }

    public long eligiblePrincipal(Connection connection, UUID cardId, LocalDate accrualDate) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(remaining_amount), 0) AS eligible_principal
                  FROM economy_card_entries
                 WHERE card_id = ?
                   AND entry_type = 'PURCHASE'
                   AND remaining_amount > 0
                   AND interest_eligible_date <= ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            statement.setObject(2, accrualDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("eligible_principal");
            }
        }
    }

    private void lock(Connection connection, String sql, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet ignored = statement.executeQuery()) {
            }
        }
    }
}
