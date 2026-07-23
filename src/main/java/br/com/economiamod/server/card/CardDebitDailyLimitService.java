package br.com.economiamod.server.card;

import br.com.economiamod.common.card.CardItemData;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.card.CardStatus;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class CardDebitDailyLimitService {
    private final CardItemDataService cardItemDataService;

    public CardDebitDailyLimitService(CardItemDataService cardItemDataService) {
        this.cardItemDataService = cardItemDataService;
    }

    public CardDebitDailyLimitResultType updateLimit(UUID accountId, ItemStack cardStack, long limit) throws SQLException {
        if (accountId == null || limit < 0L) {
            return CardDebitDailyLimitResultType.INVALID_LIMIT;
        }
        CardItemData itemData = cardItemDataService.read(cardStack).orElse(null);
        if (itemData == null || !itemData.cardType().hasDebit()) {
            return CardDebitDailyLimitResultType.INVALID_CARD;
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                CardState card = lockCard(connection, itemData.cardId());
                if (card == null || !accountId.equals(card.accountId())) {
                    connection.rollback();
                    return CardDebitDailyLimitResultType.NOT_OWNER;
                }
                if (!CardStatus.ACTIVE.name().equals(card.status())) {
                    connection.rollback();
                    return CardDebitDailyLimitResultType.INACTIVE_CARD;
                }
                updateCard(connection, itemData.cardId(), limit);
                connection.commit();
                cardItemDataService.setDebitDailyLimit(cardStack, limit);
                return CardDebitDailyLimitResultType.UPDATED;
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
                SELECT account_id,
                       status
                  FROM economy_cards
                 WHERE id = ?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? new CardState(resultSet.getObject("account_id", UUID.class), resultSet.getString("status"))
                        : null;
            }
        }
    }

    private void updateCard(Connection connection, UUID cardId, long limit) throws SQLException {
        String sql = """
                UPDATE economy_cards
                   SET debit_daily_limit = ?,
                       debit_daily_spent = CASE WHEN ? = 0 THEN 0 ELSE debit_daily_spent END,
                       debit_daily_spent_on = CASE WHEN ? = 0 THEN NULL ELSE debit_daily_spent_on END,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, limit);
            statement.setLong(2, limit);
            statement.setLong(3, limit);
            statement.setObject(4, cardId);
            statement.executeUpdate();
        }
    }

    private record CardState(UUID accountId, String status) {
    }
}
