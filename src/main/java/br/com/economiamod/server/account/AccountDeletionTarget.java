package br.com.economiamod.server.account;

import java.util.UUID;

record AccountDeletionTarget(
        UUID accountId,
        UUID playerUuid,
        String username,
        String accountNumber,
        String status,
        long balance,
        long principalOutstanding,
        long interestOutstanding
) {
    boolean hasFundsOrDebt() {
        return balance != 0L || principalOutstanding != 0L || interestOutstanding != 0L;
    }
}
