package br.com.economiamod.server.transaction;

public record CreditPurchaseResult(CreditPurchaseResultType type) {
    public static CreditPurchaseResult completed() {
        return new CreditPurchaseResult(CreditPurchaseResultType.COMPLETED);
    }

    public static CreditPurchaseResult duplicateCompleted() {
        return new CreditPurchaseResult(CreditPurchaseResultType.DUPLICATE_COMPLETED);
    }

    public static CreditPurchaseResult invalidCard() {
        return new CreditPurchaseResult(CreditPurchaseResultType.INVALID_CARD);
    }

    public static CreditPurchaseResult creditNotAllowed() {
        return new CreditPurchaseResult(CreditPurchaseResultType.CREDIT_NOT_ALLOWED);
    }

    public static CreditPurchaseResult insufficientCredit() {
        return new CreditPurchaseResult(CreditPurchaseResultType.INSUFFICIENT_CREDIT);
    }

    public static CreditPurchaseResult inactiveAccount() {
        return new CreditPurchaseResult(CreditPurchaseResultType.INACTIVE_ACCOUNT);
    }
}

