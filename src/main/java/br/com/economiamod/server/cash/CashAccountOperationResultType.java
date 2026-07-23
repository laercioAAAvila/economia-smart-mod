package br.com.economiamod.server.cash;

public enum CashAccountOperationResultType {
    COMPLETED,
    NO_MONEY,
    INSUFFICIENT_BALANCE,
    INSUFFICIENT_INVENTORY_SPACE,
    INACTIVE_ACCOUNT
}

