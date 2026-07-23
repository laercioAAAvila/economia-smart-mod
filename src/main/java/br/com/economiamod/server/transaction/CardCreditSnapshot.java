package br.com.economiamod.server.transaction;

import br.com.economiamod.common.card.CardType;

public record CardCreditSnapshot(
        CardType cardType,
        String status,
        long individualCreditLimit,
        long principalOutstanding,
        long interestOutstanding
) {
}

