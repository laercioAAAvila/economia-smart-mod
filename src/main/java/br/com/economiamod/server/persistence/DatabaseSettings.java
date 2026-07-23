package br.com.economiamod.server.persistence;

import br.com.economiamod.server.config.EconomyServerConfig;

public record DatabaseSettings(
        String type,
        String host,
        int port,
        String name,
        String username,
        String password,
        boolean ssl,
        int minimumPoolSize,
        int maximumPoolSize,
        long connectionTimeoutMs,
        long queryTimeoutMs
) {
    public static DatabaseSettings fromConfig() {
        return new DatabaseSettings(
                EconomyServerConfig.DATABASE_TYPE.get(),
                EconomyServerConfig.DATABASE_HOST.get(),
                EconomyServerConfig.DATABASE_PORT.get(),
                EconomyServerConfig.DATABASE_NAME.get(),
                EconomyServerConfig.DATABASE_USERNAME.get(),
                EconomyServerConfig.DATABASE_PASSWORD.get(),
                EconomyServerConfig.DATABASE_SSL.get(),
                EconomyServerConfig.DATABASE_POOL_MINIMUM.get(),
                EconomyServerConfig.DATABASE_POOL_MAXIMUM.get(),
                EconomyServerConfig.DATABASE_CONNECTION_TIMEOUT_MS.get(),
                EconomyServerConfig.DATABASE_QUERY_TIMEOUT_MS.get()
        );
    }

    public String jdbcUrl() {
        String normalizedType = type.toLowerCase();
        if (!"postgresql".equals(normalizedType)) {
            throw new IllegalArgumentException("Unsupported database.type: " + type);
        }
        return "jdbc:postgresql://%s:%d/%s?ssl=%s".formatted(host, port, name, ssl);
    }
}

