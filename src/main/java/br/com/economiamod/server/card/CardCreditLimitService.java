package br.com.economiamod.server.card;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.card.CardItemData;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.card.CardStatus;
import br.com.economiamod.common.card.CardType;
import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.common.credit.CreditMath;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class CardCreditLimitService {
    private final CardItemDataService cardItemDataService = new CardItemDataService();

    public CardCreditLimitResultType updateLimit(UUID accountId, ItemStack cardStack, long limit) throws SQLException {
        if (accountId == null || limit < 0L) {
            return CardCreditLimitResultType.LIMIT_UNAVAILABLE;
        }

        CardItemData itemData = cardItemDataService.read(cardStack).orElse(null);
        if (itemData == null) {
            return CardCreditLimitResultType.INVALID_CARD;
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                CardLimitState card = lockCard(connection, itemData.cardId());
                if (card == null || card.securityVersion() != itemData.securityVersion() || card.cardType() != itemData.cardType()) {
                    connection.rollback();
                    return CardCreditLimitResultType.CARD_NOT_FOUND;
                }
                if (!accountId.equals(card.accountId())) {
                    connection.rollback();
                    return CardCreditLimitResultType.NOT_OWNER;
                }
                if (card.cardStatus() != CardStatus.ACTIVE) {
                    connection.rollback();
                    return CardCreditLimitResultType.CARD_INACTIVE;
                }
                if (!card.cardType().hasCredit()) {
                    connection.rollback();
                    return CardCreditLimitResultType.CREDIT_NOT_SUPPORTED;
                }

                AccountLimitState account = lockAccount(connection, accountId);
                if (account == null || account.accountStatus() != AccountStatus.ACTIVE) {
                    connection.rollback();
                    return CardCreditLimitResultType.INACTIVE_ACCOUNT;
                }

                long debt = CreditMath.debtTotal(card.principalOutstanding(), card.interestOutstanding());
                if (limit < debt) {
                    connection.rollback();
                    return CardCreditLimitResultType.LIMIT_BELOW_DEBT;
                }

                long reserved = reservedCreditExcluding(connection, accountId, itemData.cardId());
                long effectiveAccountLimit = CreditLimitPolicy.effectiveLimit(account.balance(), account.configuredCreditLimit());
                if (reserved + limit > effectiveAccountLimit) {
                    connection.rollback();
                    return CardCreditLimitResultType.LIMIT_UNAVAILABLE;
                }

                updateCardLimit(connection, itemData.cardId(), limit);
                connection.commit();
                cardItemDataService.setCreditLimit(cardStack, limit);
                return CardCreditLimitResultType.UPDATED;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private CardLimitState lockCard(Connection connection, UUID cardId) throws SQLException {
        String sql = """
                SELECT account_id,
                       card_type,
                       status,
                       individual_credit_limit,
                       credit_principal_outstanding,
                       credit_interest_outstanding,
                       security_version
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
                return new CardLimitState(
                        resultSet.getObject("account_id", UUID.class),
                        CardType.valueOf(resultSet.getString("card_type")),
                        CardStatus.valueOf(resultSet.getString("status")),
                        resultSet.getLong("individual_credit_limit"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding"),
                        resultSet.getInt("security_version")
                );
            }
        }
    }

    private AccountLimitState lockAccount(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT status, balance, configured_credit_limit
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new AccountLimitState(
                        AccountStatus.valueOf(resultSet.getString("status")),
                        resultSet.getLong("balance"),
                        resultSet.getLong("configured_credit_limit")
                );
            }
        }
    }

    private long reservedCreditExcluding(Connection connection, UUID accountId, UUID excludedCardId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN status = 'ACTIVE' THEN individual_credit_limit
                        WHEN status = 'DISABLED' AND (credit_principal_outstanding + credit_interest_outstanding) > 0
                            THEN credit_principal_outstanding + credit_interest_outstanding
                        ELSE 0
                    END
                ), 0) AS reserved
                  FROM economy_cards
                 WHERE account_id = ?
                   AND id <> ?
                   AND card_type IN ('CREDIT', 'DEBIT_CREDIT')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setObject(2, excludedCardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("reserved");
            }
        }
    }

    private void updateCardLimit(Connection connection, UUID cardId, long limit) throws SQLException {
        String sql = """
                UPDATE economy_cards
                   SET individual_credit_limit = ?,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, limit);
            statement.setObject(2, cardId);
            statement.executeUpdate();
        }
    }

    private record CardLimitState(
            UUID accountId,
            CardType cardType,
            CardStatus cardStatus,
            long individualCreditLimit,
            long principalOutstanding,
            long interestOutstanding,
            int securityVersion
    ) {
    }

    private record AccountLimitState(AccountStatus accountStatus, long balance, long configuredCreditLimit) {
    }
}
