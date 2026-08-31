package br.com.economiamod.server.transaction;

public enum IdempotencyCheck {
    ABSENT,
    MATCH,
    CONFLICT
}
