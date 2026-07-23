package br.com.economiamod.server.card;

import br.com.economiamod.common.card.CardType;
import java.util.UUID;

public record CardValidationResult(
        CardValidationResultType type,
        UUID cardId,
        UUID accountId,
        CardType cardType,
        long individualCreditLimit,
        long creditPrincipalOutstanding,
        long creditInterestOutstanding
) {
    public static CardValidationResult valid(
            UUID cardId,
            UUID accountId,
            CardType cardType,
            long individualCreditLimit,
            long creditPrincipalOutstanding,
            long creditInterestOutstanding
    ) {
        return new CardValidationResult(
                CardValidationResultType.VALID,
                cardId,
                accountId,
                cardType,
                individualCreditLimit,
                creditPrincipalOutstanding,
                creditInterestOutstanding
        );
    }

    public static CardValidationResult invalid(CardValidationResultType type) {
        return new CardValidationResult(type, null, null, null, 0L, 0L, 0L);
    }
}

