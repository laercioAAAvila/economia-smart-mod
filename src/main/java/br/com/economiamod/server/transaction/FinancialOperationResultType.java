package br.com.economiamod.server.transaction;

public enum FinancialOperationResultType {
    COMPLETED,
    DUPLICATE_COMPLETED,
    IDEMPOTENCY_CONFLICT,
    INSUFFICIENT_BALANCE,
    INACTIVE_ACCOUNT
}
