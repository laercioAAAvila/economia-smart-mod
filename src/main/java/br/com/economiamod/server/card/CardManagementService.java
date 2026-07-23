package br.com.economiamod.server.card;

import br.com.economiamod.common.card.CardStatus;
import br.com.economiamod.common.network.AtmCardsPayload;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CardManagementService {
    public List<AtmCardsPayload.CardSummary> cardsForAccount(UUID accountId) throws SQLException {
        if (accountId == null) {
            return List.of();
        }

        String sql = """
                SELECT id,
                       custom_name,
                       card_type,
                       status,
                       individual_credit_limit,
                       credit_principal_outstanding,
                       credit_interest_outstanding,
                       debit_daily_limit
                  FROM economy_cards
                 WHERE account_id = ?
                 ORDER BY created_at DESC, id
                 LIMIT 64
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AtmCardsPayload.CardSummary> cards = new ArrayList<>();
                while (resultSet.next()) {
                    long principal = resultSet.getLong("credit_principal_outstanding");
                    long interest = resultSet.getLong("credit_interest_outstanding");
                    String customName = resultSet.getString("custom_name");
                    cards.add(new AtmCardsPayload.CardSummary(
                            resultSet.getObject("id", UUID.class),
                            customName == null ? "" : customName,
                            resultSet.getString("card_type"),
                            resultSet.getString("status"),
                            resultSet.getLong("individual_credit_limit"),
                            Math.addExact(principal, interest),
                            resultSet.getLong("debit_daily_limit")
                    ));
                }
                return List.copyOf(cards);
            }
        }
    }

    public CardSecurityResultType blockCard(UUID accountId, UUID cardId) throws SQLException {
        return updateStatus(accountId, cardId, CardStatus.BLOCKED);
    }

    public CardSecurityResultType disableCard(UUID accountId, UUID cardId) throws SQLException {
        return updateStatus(accountId, cardId, CardStatus.DISABLED);
    }

    private CardSecurityResultType updateStatus(UUID accountId, UUID cardId, CardStatus nextStatus) throws SQLException {
        if (accountId == null || cardId == null) {
            return CardSecurityResultType.INVALID_CARD;
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                CardState card = lockCard(connection, cardId);
                if (card == null) {
                    connection.rollback();
                    return CardSecurityResultType.CARD_NOT_FOUND;
                }
                if (!accountId.equals(card.accountId())) {
                    connection.rollback();
                    return CardSecurityResultType.NOT_OWNER;
                }
                if ((card.status() == CardStatus.DISABLED || card.status() == CardStatus.EXPIRED) && card.status() != nextStatus) {
                    connection.rollback();
                    return CardSecurityResultType.INVALID_CARD;
                }
                if (card.status() == nextStatus) {
                    connection.rollback();
                    return CardSecurityResultType.UPDATED;
                }
                writeStatus(connection, cardId, nextStatus);
                connection.commit();
                return CardSecurityResultType.UPDATED;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private CardState lockCard(Connection connection, UUID cardId) throws SQLException {
        String sql = """
                SELECT account_id, status
                  FROM economy_cards
                 WHERE id = ?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? new CardState(resultSet.getObject("account_id", UUID.class), CardStatus.valueOf(resultSet.getString("status")))
                        : null;
            }
        }
    }

    private void writeStatus(Connection connection, UUID cardId, CardStatus status) throws SQLException {
        String sql = """
                UPDATE economy_cards
                   SET status = ?,
                       disabled_at = CASE WHEN ? = 'DISABLED' THEN CURRENT_TIMESTAMP ELSE disabled_at END,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, status.name());
            statement.setObject(3, cardId);
            statement.executeUpdate();
        }
    }

    private record CardState(UUID accountId, CardStatus status) {
    }
}
