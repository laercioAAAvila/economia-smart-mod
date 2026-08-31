package br.com.economiamod.server.account;

public enum AccountDeletionResultType {
    CLOSED,
    ALREADY_CLOSED,
    HAS_BALANCE_OR_DEBT,
    NOT_FOUND
}
