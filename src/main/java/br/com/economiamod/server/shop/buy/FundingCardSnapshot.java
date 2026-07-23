package br.com.economiamod.server.shop.buy;

import br.com.economiamod.common.card.CardType;
import java.util.UUID;

public record FundingCardSnapshot(
        UUID cardId,
        UUID accountId,
        CardType cardType,
        String cardStatus,
        long individualCreditLimit,
        long cardPrincipalOutstanding,
        long cardInterestOutstanding
) {
}
