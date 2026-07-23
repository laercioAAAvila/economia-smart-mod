package br.com.economiamod.server.transaction;

public enum DebitPurchaseResultType {
    COMPLETED,
    DUPLICATE_COMPLETED,
    INVALID_CARD,
    DEBIT_NOT_ALLOWED,
    INSUFFICIENT_BALANCE,
    DAILY_LIMIT_REACHED,
    INACTIVE_ACCOUNT
}
