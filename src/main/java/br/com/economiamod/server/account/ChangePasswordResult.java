package br.com.economiamod.server.account;

public record ChangePasswordResult(ChangePasswordResultType type) {
    public static ChangePasswordResult changed() {
        return new ChangePasswordResult(ChangePasswordResultType.CHANGED);
    }

    public static ChangePasswordResult invalidPassword() {
        return new ChangePasswordResult(ChangePasswordResultType.INVALID_PASSWORD);
    }

    public static ChangePasswordResult inactiveAccount() {
        return new ChangePasswordResult(ChangePasswordResultType.INACTIVE_ACCOUNT);
    }

    public static ChangePasswordResult notFound() {
        return new ChangePasswordResult(ChangePasswordResultType.NOT_FOUND);
    }
}
