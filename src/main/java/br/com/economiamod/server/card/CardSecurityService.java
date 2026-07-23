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

public final class CardSecurityService {
    private final CardItemDataService cardItemDataService = new CardItemDataService();

    public CardSecurityResultType blockCard(UUID accountId, UUID cardId) throws SQLException {
        if (accountId == null || cardId == null) {
            return CardSecurityResultType.INVALID_CARD;
        }
        return updateStatus(accountId, cardId, null, CardStatus.BLOCKED, false);
    }

    public CardSecurityResultType unblockCard(UUID accountId, ItemStack cardStack) throws SQLException {
        CardItemData itemData = cardItemDataService.read(cardStack).orElse(null);
        if (itemData == null) {
            return CardSecurityResultType.INVALID_CARD;
        }
        return updateStatus(accountId, itemData.cardId(), itemData.securityVersion(), CardStatus.ACTIVE, true);
    }

    public boolean isBlockedOwnerCard(UUID accountId, ItemStack cardStack) throws SQLException {
        CardItemData itemData = cardItemDataService.read(cardStack).orElse(null);
        if (accountId == null || itemData == null) {
            return false;
        }
        CardSecurityState card = readCard(itemData.cardId());
        return card != null
                && accountId.equals(card.accountId())
                && itemData.securityVersion() == card.securityVersion()
                && card.status() == CardStatus.BLOCKED;
    }

    private CardSecurityResultType updateStatus(UUID accountId, UUID cardId, Integer securityVersion, CardStatus nextStatus, boolean requireBlocked) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                CardSecurityState card = lockCard(connection, cardId);
                if (card == null) {
                    connection.rollback();
                    return CardSecurityResultType.CARD_NOT_FOUND;
                }
                if (!accountId.equals(card.accountId())) {
                    connection.rollback();
                    return CardSecurityResultType.NOT_OWNER;
                }
                if (securityVersion != null && securityVersion != card.securityVersion()) {
                    connection.rollback();
                    return CardSecurityResultType.INVALID_CARD;
                }
                if (requireBlocked && card.status() != CardStatus.BLOCKED) {
                    connection.rollback();
                    return card.status() == CardStatus.ACTIVE ? CardSecurityResultType.ALREADY_ACTIVE : CardSecurityResultType.CARD_NOT_BLOCKED;
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

    private CardSecurityState lockCard(Connection connection, UUID cardId) throws SQLException {
        String sql = """
                SELECT account_id, status, security_version
                  FROM economy_cards
                 WHERE id = ?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new CardSecurityState(
                        resultSet.getObject("account_id", UUID.class),
                        CardStatus.valueOf(resultSet.getString("status")),
                        resultSet.getInt("security_version")
                );
            }
        }
    }

    private CardSecurityState readCard(UUID cardId) throws SQLException {
        String sql = """
                SELECT account_id, status, security_version
                  FROM economy_cards
                 WHERE id = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new CardSecurityState(
                        resultSet.getObject("account_id", UUID.class),
                        CardStatus.valueOf(resultSet.getString("status")),
                        resultSet.getInt("security_version")
                );
            }
        }
    }

    private void writeStatus(Connection connection, UUID cardId, CardStatus status) throws SQLException {
        String sql = """
                UPDATE economy_cards
                   SET status = ?,
                       disabled_at = CASE WHEN ? = 'ACTIVE' THEN NULL ELSE disabled_at END,
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

    private record CardSecurityState(UUID accountId, CardStatus status, int securityVersion) {
    }
}
