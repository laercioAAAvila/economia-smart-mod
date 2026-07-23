package br.com.economiamod.server.shop.buy;

import br.com.economiamod.common.card.CardType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class FundingCardRepository {
    public void lockCard(Connection connection, UUID cardId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM economy_cards WHERE id = ? FOR UPDATE")) {
            statement.setObject(1, cardId);
            try (ResultSet ignored = statement.executeQuery()) {
            }
        }
    }

    public Optional<FundingCardSnapshot> find(Connection connection, UUID cardId) throws SQLException {
        String sql = """
                SELECT account_id, card_type, status, individual_credit_limit,
                       credit_principal_outstanding, credit_interest_outstanding
                  FROM economy_cards
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new FundingCardSnapshot(
                        cardId,
                        resultSet.getObject("account_id", UUID.class),
                        CardType.valueOf(resultSet.getString("card_type")),
                        resultSet.getString("status"),
                        resultSet.getLong("individual_credit_limit"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                ));
            }
        }
    }
}
