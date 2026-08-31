package br.com.economiamod.server.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public final class EconomyDatabase {
    private static final AtomicReference<DatabaseConnectionProvider> PROVIDER = new AtomicReference<>();
    private static final AtomicReference<DatabaseEngine> ENGINE = new AtomicReference<>(DatabaseEngine.POSTGRESQL);

    private EconomyDatabase() {
    }

    public static void open(DatabaseSettings settings) throws SQLException {
        close();
        DriverManagerDatabaseConnectionProvider provider = new DriverManagerDatabaseConnectionProvider(settings);
        try (Connection ignored = provider.getConnection()) {
            PROVIDER.set(provider);
            ENGINE.set(settings.engine());
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

    public static DatabaseEngine engine() {
        return ENGINE.get();
    }

    public static boolean isPostgreSql() {
        return engine() == DatabaseEngine.POSTGRESQL;
    }

    public static boolean isSqlite() {
        return engine() == DatabaseEngine.SQLITE;
    }

    public static void close() {
        DatabaseConnectionProvider previous = PROVIDER.getAndSet(null);
        if (previous != null) {
            previous.close();
        }
    }
}
