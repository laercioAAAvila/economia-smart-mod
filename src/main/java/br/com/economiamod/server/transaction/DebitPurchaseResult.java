package br.com.economiamod.server.transaction;

public record DebitPurchaseResult(DebitPurchaseResultType type) {
    public static DebitPurchaseResult completed() {
        return new DebitPurchaseResult(DebitPurchaseResultType.COMPLETED);
    }

    public static DebitPurchaseResult duplicateCompleted() {
        return new DebitPurchaseResult(DebitPurchaseResultType.DUPLICATE_COMPLETED);
    }

    public static DebitPurchaseResult idempotencyConflict() {
        return new DebitPurchaseResult(DebitPurchaseResultType.IDEMPOTENCY_CONFLICT);
    }

    public static DebitPurchaseResult invalidCard() {
        return new DebitPurchaseResult(DebitPurchaseResultType.INVALID_CARD);
    }

    public static DebitPurchaseResult debitNotAllowed() {
        return new DebitPurchaseResult(DebitPurchaseResultType.DEBIT_NOT_ALLOWED);
    }

    public static DebitPurchaseResult insufficientBalance() {
        return new DebitPurchaseResult(DebitPurchaseResultType.INSUFFICIENT_BALANCE);
    }

    public static DebitPurchaseResult dailyLimitReached() {
        return new DebitPurchaseResult(DebitPurchaseResultType.DAILY_LIMIT_REACHED);
    }

    public static DebitPurchaseResult inactiveAccount() {
        return new DebitPurchaseResult(DebitPurchaseResultType.INACTIVE_ACCOUNT);
    }
}
