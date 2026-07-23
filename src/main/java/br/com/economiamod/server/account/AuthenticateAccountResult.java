package br.com.economiamod.server.account;

import java.util.UUID;

public record AuthenticateAccountResult(AuthenticateAccountResultType type, UUID accountId, String username, String accountNumber) {
    public static AuthenticateAccountResult authenticated(UUID accountId, String username, String accountNumber) {
        return new AuthenticateAccountResult(AuthenticateAccountResultType.AUTHENTICATED, accountId, username, accountNumber);
    }

    public static AuthenticateAccountResult notFound() {
        return new AuthenticateAccountResult(AuthenticateAccountResultType.NOT_FOUND, null, null, "");
    }

    public static AuthenticateAccountResult invalidPassword() {
        return new AuthenticateAccountResult(AuthenticateAccountResultType.INVALID_PASSWORD, null, null, "");
    }

    public static AuthenticateAccountResult inactiveAccount() {
        return new AuthenticateAccountResult(AuthenticateAccountResultType.INACTIVE_ACCOUNT, null, null, "");
    }
}
