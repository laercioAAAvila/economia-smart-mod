package br.com.economiamod.server.persistence;

import br.com.economiamod.server.config.EconomyServerConfig;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public record DatabaseSettings(
        DatabaseEngine engine,
        String host,
        int port,
        String name,
        String username,
        String password,
        boolean ssl,
        Path sqliteFile,
        int minimumPoolSize,
        int maximumPoolSize,
        long connectionTimeoutMs,
        long queryTimeoutMs
) {
    public static DatabaseSettings fromConfig(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        Path sqliteFile = resolveSqlitePath(worldRoot, EconomyServerConfig.DATABASE_SQLITE_FILE.get());
        return fromConfig(sqliteFile);
    }

    public static DatabaseSettings fromConfig() {
        return fromConfig(Path.of(EconomyServerConfig.DATABASE_SQLITE_FILE.get()).toAbsolutePath().normalize());
    }

    private static DatabaseSettings fromConfig(Path sqliteFile) {
        DatabaseEngine engine = DatabaseEngine.parse(EconomyServerConfig.DATABASE_TYPE.get());
        int maxPool = engine == DatabaseEngine.SQLITE ? 1 : EconomyServerConfig.DATABASE_POOL_MAXIMUM.get();
        int minPool = engine == DatabaseEngine.SQLITE ? 0 : EconomyServerConfig.DATABASE_POOL_MINIMUM.get();
        return new DatabaseSettings(
                engine,
                EconomyServerConfig.DATABASE_HOST.get(),
                EconomyServerConfig.DATABASE_PORT.get(),
                EconomyServerConfig.DATABASE_NAME.get(),
                EconomyServerConfig.DATABASE_USERNAME.get(),
                EconomyServerConfig.DATABASE_PASSWORD.get(),
                EconomyServerConfig.DATABASE_SSL.get(),
                sqliteFile,
                minPool,
                maxPool,
                EconomyServerConfig.DATABASE_CONNECTION_TIMEOUT_MS.get(),
                EconomyServerConfig.DATABASE_QUERY_TIMEOUT_MS.get()
        );
    }

    private static Path resolveSqlitePath(Path worldRoot, String configured) {
        Path path = Path.of(configured == null || configured.isBlank() ? "economia/economia.db" : configured.trim());
        return path.isAbsolute() ? path.normalize() : worldRoot.resolve(path).normalize();
    }

    public String jdbcUrl() {
        return switch (engine) {
            case POSTGRESQL -> "jdbc:postgresql://%s:%d/%s?ssl=%s".formatted(host, port, name, ssl);
            case SQLITE -> "jdbc:sqlite:" + sqliteFile;
        };
    }

    public String safeTarget() {
        return switch (engine) {
            case POSTGRESQL -> "%s:%d/%s".formatted(host, port, name);
            case SQLITE -> sqliteFile.toString();
        };
    }
}
