package br.com.economiamod.server.persistence;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class EconomyDatabaseState {
    private static final AtomicReference<DatabaseStateSnapshot> STATE = new AtomicReference<>(
            new DatabaseStateSnapshot(DatabaseAvailability.NOT_INITIALIZED, "Banco ainda nao inicializado.", 0, Instant.EPOCH)
    );

    private EconomyDatabaseState() {
    }

    public static DatabaseStateSnapshot current() {
        return STATE.get();
    }

    public static boolean isAvailable() {
        return current().isAvailable();
    }

    public static void initializing(int knownMigrations) {
        update(DatabaseAvailability.INITIALIZING, "Inicializando persistencia SQL.", knownMigrations);
    }

    public static void available(int knownMigrations) {
        update(DatabaseAvailability.AVAILABLE, "Persistencia SQL disponivel.", knownMigrations);
    }

    public static void unavailable(String message, int knownMigrations) {
        update(DatabaseAvailability.UNAVAILABLE, message, knownMigrations);
    }

    private static void update(DatabaseAvailability availability, String message, int knownMigrations) {
        STATE.set(new DatabaseStateSnapshot(availability, message, knownMigrations, Instant.now()));
    }
}

