package br.com.economiamod.server.account;

import java.util.UUID;

public record AccountDeletionResult(
        AccountDeletionResultType type,
        UUID accountId,
        UUID playerUuid,
        String username,
        String accountNumber,
        int affectedRows
) {
    public static AccountDeletionResult notFound() {
        return new AccountDeletionResult(AccountDeletionResultType.NOT_FOUND, null, null, null, null, 0);
    }

    public static AccountDeletionResult of(AccountDeletionResultType type, AccountDeletionTarget target, int affectedRows) {
        return new AccountDeletionResult(type, target.accountId(), target.playerUuid(), target.username(),
                target.accountNumber(), affectedRows);
    }
}
