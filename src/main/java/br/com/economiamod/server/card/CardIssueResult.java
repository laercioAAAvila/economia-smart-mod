package br.com.economiamod.server.card;

import br.com.economiamod.common.card.CardType;
import java.util.UUID;

public record CardIssueResult(
        CardIssueResultType type,
        UUID cardId,
        CardType cardType,
        int securityVersion,
        String accountNumber,
        String cardName,
        long individualCreditLimit
) {
    public static CardIssueResult issued(UUID cardId, CardType cardType, int securityVersion, String accountNumber, String cardName, long individualCreditLimit) {
        return new CardIssueResult(CardIssueResultType.ISSUED, cardId, cardType, securityVersion, accountNumber, cardName, individualCreditLimit);
    }

    public static CardIssueResult duplicateIssued(UUID cardId, CardType cardType, int securityVersion, String accountNumber, String cardName, long individualCreditLimit) {
        return new CardIssueResult(CardIssueResultType.DUPLICATE_ISSUED, cardId, cardType, securityVersion, accountNumber, cardName, individualCreditLimit);
    }

    public static CardIssueResult idempotencyConflict() {
        return new CardIssueResult(CardIssueResultType.IDEMPOTENCY_CONFLICT, null, null, 0, "", "", 0L);
    }

    public static CardIssueResult inactiveAccount() {
        return new CardIssueResult(CardIssueResultType.INACTIVE_ACCOUNT, null, null, 0, "", "", 0L);
    }

    public static CardIssueResult cardLimitReached() {
        return new CardIssueResult(CardIssueResultType.CARD_LIMIT_REACHED, null, null, 0, "", "", 0L);
    }

    public static CardIssueResult creditLimitUnavailable() {
        return new CardIssueResult(CardIssueResultType.CREDIT_LIMIT_UNAVAILABLE, null, null, 0, "", "", 0L);
    }

    public static CardIssueResult insufficientBalance() {
        return new CardIssueResult(CardIssueResultType.INSUFFICIENT_BALANCE, null, null, 0, "", "", 0L);
    }
}
