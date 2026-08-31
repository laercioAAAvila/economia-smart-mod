package br.com.economiamod.server.gold;

public enum GoldExchangeResultType {
    COMPLETED,
    DUPLICATE_COMPLETED,
    IDEMPOTENCY_CONFLICT,
    INACTIVE_ACCOUNT,
    INSUFFICIENT_BALANCE,
    INSUFFICIENT_RESERVE,
    INVALID_GOLD
}

