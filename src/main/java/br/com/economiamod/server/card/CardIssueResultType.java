package br.com.economiamod.server.card;

public enum CardIssueResultType {
    ISSUED,
    DUPLICATE_ISSUED,
    IDEMPOTENCY_CONFLICT,
    INACTIVE_ACCOUNT,
    CARD_LIMIT_REACHED,
    CREDIT_LIMIT_UNAVAILABLE,
    INSUFFICIENT_BALANCE
}
