package br.com.economiamod.server.account;

public enum ChangePasswordResultType {
    CHANGED,
    INVALID_PASSWORD,
    INACTIVE_ACCOUNT,
    USERNAME_MISMATCH,
    NOT_FOUND
}
