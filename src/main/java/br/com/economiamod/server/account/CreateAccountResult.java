package br.com.economiamod.server.account;

import java.util.UUID;

public record CreateAccountResult(CreateAccountResultType type, UUID accountId, long openingFee,
                                  boolean alreadyActive) {
    public static CreateAccountResult created(UUID accountId, long openingFee, boolean alreadyActive) {
        return new CreateAccountResult(CreateAccountResultType.CREATED, accountId, openingFee, alreadyActive);
    }

    public static CreateAccountResult playerAlreadyHasAccount() {
        return new CreateAccountResult(CreateAccountResultType.PLAYER_ALREADY_HAS_ACCOUNT, null, 0L, false);
    }

    public static CreateAccountResult usernameAlreadyUsed() {
        return new CreateAccountResult(CreateAccountResultType.USERNAME_ALREADY_USED, null, 0L, false);
    }
}
