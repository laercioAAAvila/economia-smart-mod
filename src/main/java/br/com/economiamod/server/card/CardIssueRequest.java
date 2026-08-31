package br.com.economiamod.server.card;

import br.com.economiamod.common.card.CardType;
import java.util.UUID;

public record CardIssueRequest(
        UUID accountId,
        CardType cardType,
        String customName,
        long individualCreditLimit,
        UUID playerUuid,
        UUID requestId
) {
    public CardIssueRequest(UUID accountId, CardType cardType, String customName, long individualCreditLimit) {
        this(accountId, cardType, customName, individualCreditLimit, null, UUID.randomUUID());
    }
}
