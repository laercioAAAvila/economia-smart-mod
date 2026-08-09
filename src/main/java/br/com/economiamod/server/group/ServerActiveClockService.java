package br.com.economiamod.server.group;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public final class ServerActiveClockService {
    public static final ServerActiveClockService INSTANCE = new ServerActiveClockService();
    private static final long FLUSH_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);

    private long persistedMillis;
    private long startedAtNanos;
    private long lastFlushNanos;

    private ServerActiveClockService() {
    }

    public synchronized void start() throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT active_millis FROM economy_server_clock WHERE id = 1")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                persistedMillis = resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
        startedAtNanos = System.nanoTime();
        lastFlushNanos = startedAtNanos;
    }

    public synchronized long currentMillis() {
        if (startedAtNanos == 0L) {
            return persistedMillis;
        }
        return persistedMillis + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    public synchronized void tick() {
        if (!EconomyDatabaseState.isAvailable() || startedAtNanos == 0L) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastFlushNanos < FLUSH_INTERVAL_NANOS) {
            return;
        }
        try {
            flushAt(now);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao persistir relógio ativo do servidor.", exception);
        }
    }

    public synchronized void stop() {
        if (startedAtNanos == 0L || !EconomyDatabaseState.isAvailable()) {
            startedAtNanos = 0L;
            return;
        }
        try {
            flushAt(System.nanoTime());
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao finalizar relógio ativo do servidor.", exception);
        } finally {
            startedAtNanos = 0L;
        }
    }

    private void flushAt(long nowNanos) throws SQLException {
        long elapsedMillis = Math.max(0L, TimeUnit.NANOSECONDS.toMillis(nowNanos - startedAtNanos));
        long current = Math.addExact(persistedMillis, elapsedMillis);
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_server_clock
                        SET active_millis = ?, updated_at = CURRENT_TIMESTAMP
                      WHERE id = 1
                     """)) {
            statement.setLong(1, current);
            statement.executeUpdate();
        }
        persistedMillis = current;
        startedAtNanos = nowNanos;
        lastFlushNanos = nowNanos;
    }
}
