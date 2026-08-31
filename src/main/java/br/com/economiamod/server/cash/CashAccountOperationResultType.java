package br.com.economiamod.server.cash;

public enum CashAccountOperationResultType {
    COMPLETED,
    NO_MONEY,
    INSUFFICIENT_BALANCE,
    INSUFFICIENT_INVENTORY_SPACE,
    INVALID_DENOMINATION,
    INACTIVE_ACCOUNT,
    RECONCILIATION_REQUIRED
}
