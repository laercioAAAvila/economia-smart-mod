package br.com.economiamod.server.card;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.card.CardItemData;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.card.CardStatus;
import br.com.economiamod.common.card.CardType;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.minecraft.world.item.ItemStack;

public final class CardValidationService {
    private final CardItemDataService cardItemDataService;

    public CardValidationService(CardItemDataService cardItemDataService) {
        this.cardItemDataService = cardItemDataService;
    }

    public CardValidationResult validate(ItemStack stack) throws SQLException {
        CardItemData itemData = cardItemDataService.read(stack).orElse(null);
        if (itemData == null) {
            return CardValidationResult.invalid(CardValidationResultType.INVALID_ITEM);
        }

        String sql = """
                SELECT c.account_id,
                       c.card_type,
                       c.status AS card_status,
                       c.individual_credit_limit,
                       c.credit_principal_outstanding,
                       c.credit_interest_outstanding,
                       c.security_version,
                       a.status AS account_status
                  FROM economy_cards c
                  JOIN economy_accounts a ON a.id = c.account_id
                 WHERE c.id = ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, itemData.cardId());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return CardValidationResult.invalid(CardValidationResultType.NOT_FOUND);
                }

                int securityVersion = resultSet.getInt("security_version");
                if (securityVersion != itemData.securityVersion()) {
                    return CardValidationResult.invalid(CardValidationResultType.SECURITY_VERSION_MISMATCH);
                }

                CardType cardType = CardType.valueOf(resultSet.getString("card_type"));
                if (cardType != itemData.cardType()) {
                    return CardValidationResult.invalid(CardValidationResultType.INVALID_ITEM);
                }

                CardStatus cardStatus = CardStatus.valueOf(resultSet.getString("card_status"));
                if (cardStatus != CardStatus.ACTIVE) {
                    return switch (cardStatus) {
                        case DISABLED -> CardValidationResult.invalid(CardValidationResultType.DISABLED);
                        case BLOCKED -> CardValidationResult.invalid(CardValidationResultType.BLOCKED);
                        case EXPIRED -> CardValidationResult.invalid(CardValidationResultType.EXPIRED);
                        case ACTIVE -> throw new IllegalStateException("unreachable");
                    };
                }

                AccountStatus accountStatus = AccountStatus.valueOf(resultSet.getString("account_status"));
                if (accountStatus != AccountStatus.ACTIVE) {
                    return CardValidationResult.invalid(CardValidationResultType.ACCOUNT_INACTIVE);
                }

                return CardValidationResult.valid(
                        itemData.cardId(),
                        resultSet.getObject("account_id", java.util.UUID.class),
                        cardType,
                        resultSet.getLong("individual_credit_limit"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                );
            }
        }
    }
}

