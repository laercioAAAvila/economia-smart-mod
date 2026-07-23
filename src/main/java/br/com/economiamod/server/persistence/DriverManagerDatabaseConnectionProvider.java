package br.com.economiamod.server.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DriverManagerDatabaseConnectionProvider implements DatabaseConnectionProvider {
    private final HikariDataSource dataSource;

    public DriverManagerDatabaseConnectionProvider(DatabaseSettings settings) {
        loadPostgreSqlDriver();

        Properties properties = new Properties();
        properties.setProperty("tcpKeepAlive", "true");
        properties.setProperty("connectTimeout", String.valueOf(Math.max(1L, settings.connectionTimeoutMs() / 1000L)));
        properties.setProperty("socketTimeout", String.valueOf(Math.max(1L, settings.queryTimeoutMs() / 1000L)));

        int maximumPoolSize = Math.max(1, settings.maximumPoolSize());
        int minimumIdle = Math.min(maximumPoolSize, Math.max(0, settings.minimumPoolSize()));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMinimumIdle(minimumIdle);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(settings.connectionTimeoutMs());
        config.setValidationTimeout(Math.max(1000L, Math.min(settings.connectionTimeoutMs(), 5000L)));
        config.setPoolName("EconomiaModPool");
        config.setDataSourceProperties(properties);
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static void loadPostgreSqlDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("PostgreSQL JDBC driver is not available.", exception);
        }
    }
}
