package br.com.economiamod.server.session;

import java.time.Instant;
import java.util.UUID;

public record BankSession(
        UUID playerUuid,
        UUID accountId,
        String username,
        String accountNumber,
        boolean showUsername,
        UUID loginCardId,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
