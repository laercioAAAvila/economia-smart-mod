package br.com.economiamod.server.card;

public enum CardCreditLimitResultType {
    UPDATED,
    INVALID_CARD,
    CARD_NOT_FOUND,
    NOT_OWNER,
    CARD_INACTIVE,
    CREDIT_NOT_SUPPORTED,
    LIMIT_UNAVAILABLE,
    LIMIT_BELOW_DEBT,
    INACTIVE_ACCOUNT
}
