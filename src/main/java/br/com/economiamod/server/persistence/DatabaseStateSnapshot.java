package br.com.economiamod.server.persistence;

import java.time.Instant;

public record DatabaseStateSnapshot(
        DatabaseAvailability availability,
        String message,
        int knownMigrations,
        Instant updatedAt
) {
    public boolean isAvailable() {
        return availability == DatabaseAvailability.AVAILABLE;
    }
}

