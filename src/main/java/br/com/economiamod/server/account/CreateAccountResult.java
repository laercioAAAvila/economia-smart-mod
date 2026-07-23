package br.com.economiamod.server.account;

public record CreateAccountResult(CreateAccountResultType type) {
    public static CreateAccountResult created() {
        return new CreateAccountResult(CreateAccountResultType.CREATED);
    }

    public static CreateAccountResult playerAlreadyHasAccount() {
        return new CreateAccountResult(CreateAccountResultType.PLAYER_ALREADY_HAS_ACCOUNT);
    }

    public static CreateAccountResult usernameAlreadyUsed() {
        return new CreateAccountResult(CreateAccountResultType.USERNAME_ALREADY_USED);
    }
}

