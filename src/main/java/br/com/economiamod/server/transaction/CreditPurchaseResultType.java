package br.com.economiamod.server.transaction;

public enum CreditPurchaseResultType {
    COMPLETED,
    DUPLICATE_COMPLETED,
    INVALID_CARD,
    CREDIT_NOT_ALLOWED,
    INSUFFICIENT_CREDIT,
    INACTIVE_ACCOUNT
}

