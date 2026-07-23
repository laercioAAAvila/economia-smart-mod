package br.com.economiamod.server.cash;

public record CashAccountOperationResult(CashAccountOperationResultType type, long amount, long balanceAfter) {
    public static CashAccountOperationResult completed(long amount, long balanceAfter) {
        return new CashAccountOperationResult(CashAccountOperationResultType.COMPLETED, amount, balanceAfter);
    }

    public static CashAccountOperationResult noMoney() {
        return new CashAccountOperationResult(CashAccountOperationResultType.NO_MONEY, 0L, 0L);
    }

    public static CashAccountOperationResult insufficientBalance() {
        return new CashAccountOperationResult(CashAccountOperationResultType.INSUFFICIENT_BALANCE, 0L, 0L);
    }

    public static CashAccountOperationResult insufficientInventorySpace() {
        return new CashAccountOperationResult(CashAccountOperationResultType.INSUFFICIENT_INVENTORY_SPACE, 0L, 0L);
    }

    public static CashAccountOperationResult inactiveAccount() {
        return new CashAccountOperationResult(CashAccountOperationResultType.INACTIVE_ACCOUNT, 0L, 0L);
    }
}

