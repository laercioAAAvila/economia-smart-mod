package br.com.economiamod.server.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public final class EconomyDatabase {
    private static final AtomicReference<DatabaseConnectionProvider> PROVIDER = new AtomicReference<>();

    private EconomyDatabase() {
    }

    public static void open(DatabaseSettings settings) throws SQLException {
        close();
        DriverManagerDatabaseConnectionProvider provider = new DriverManagerDatabaseConnectionProvider(settings);
        try (Connection ignored = provider.getConnection()) {
            PROVIDER.set(provider);
        } catch (SQLException exception) {
            provider.close();
            throw exception;
        }
    }

    public static Connection getConnection() throws SQLException {
        DatabaseConnectionProvider provider = PROVIDER.get();
        if (provider == null) {
            throw new SQLException("Economy database is not connected");
        }
        return provider.getConnection();
    }

    public static void close() {
        DatabaseConnectionProvider previous = PROVIDER.getAndSet(null);
        if (previous != null) {
            previous.close();
        }
    }
}
