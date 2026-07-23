package br.com.economiamod.server.transaction;

import java.util.UUID;

public record FinancialOperationResult(
        FinancialOperationResultType type,
        UUID transactionId,
        long balanceAfter
) {
    public static FinancialOperationResult completed(UUID transactionId, long balanceAfter) {
        return new FinancialOperationResult(FinancialOperationResultType.COMPLETED, transactionId, balanceAfter);
    }

    public static FinancialOperationResult duplicate(UUID transactionId, long balanceAfter) {
        return new FinancialOperationResult(FinancialOperationResultType.DUPLICATE_COMPLETED, transactionId, balanceAfter);
    }

    public static FinancialOperationResult insufficientBalance() {
        return new FinancialOperationResult(FinancialOperationResultType.INSUFFICIENT_BALANCE, null, 0L);
    }

    public static FinancialOperationResult inactiveAccount() {
        return new FinancialOperationResult(FinancialOperationResultType.INACTIVE_ACCOUNT, null, 0L);
    }
}

