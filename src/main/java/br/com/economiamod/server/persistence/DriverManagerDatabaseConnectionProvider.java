package br.com.economiamod.server.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DriverManagerDatabaseConnectionProvider implements DatabaseConnectionProvider {
    private final HikariDataSource dataSource;
    private final DatabaseEngine engine;

    public DriverManagerDatabaseConnectionProvider(DatabaseSettings settings) {
        this.engine = settings.engine();
        loadDriver(engine);
        if (engine == DatabaseEngine.SQLITE) {
            createSqliteDirectory(settings);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setMinimumIdle(settings.minimumPoolSize());
        config.setMaximumPoolSize(settings.maximumPoolSize());
        config.setConnectionTimeout(settings.connectionTimeoutMs());
        config.setValidationTimeout(Math.max(1000L, Math.min(settings.connectionTimeoutMs(), 5000L)));
        config.setPoolName("EconomiaMod-" + engine.name());

        if (engine == DatabaseEngine.POSTGRESQL) {
            Properties properties = new Properties();
            properties.setProperty("tcpKeepAlive", "true");
            properties.setProperty("connectTimeout", String.valueOf(Math.max(1L, settings.connectionTimeoutMs() / 1000L)));
            properties.setProperty("socketTimeout", String.valueOf(Math.max(1L, settings.queryTimeoutMs() / 1000L)));
            config.setUsername(settings.username());
            config.setPassword(settings.password());
            config.setDataSourceProperties(properties);
        } else {
            config.addDataSourceProperty("foreign_keys", "true");
            config.addDataSourceProperty("busy_timeout", String.valueOf(settings.queryTimeoutMs()));
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.addDataSourceProperty("transaction_mode", "IMMEDIATE");
            config.setConnectionTestQuery("SELECT 1");
        }
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        if (engine == DatabaseEngine.SQLITE) {
            try (var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 10000");
            }
        }
        return SqliteJdbcCompatibility.wrap(connection, engine);
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static void loadDriver(DatabaseEngine engine) {
        String className = engine == DatabaseEngine.POSTGRESQL ? "org.postgresql.Driver" : "org.sqlite.JDBC";
        try {
            Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(engine + " JDBC driver is not available.", exception);
        }
    }

    private static void createSqliteDirectory(DatabaseSettings settings) {
        try {
            if (settings.sqliteFile().getParent() != null) {
                Files.createDirectories(settings.sqliteFile().getParent());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create SQLite database directory: " + settings.sqliteFile(), exception);
        }
    }
}
