package br.com.economiamod.server.session;

import br.com.economiamod.server.config.EconomyServerConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class BankSessionService {
    public static final BankSessionService INSTANCE = new BankSessionService();

    private final Map<UUID, BankSession> sessions = new ConcurrentHashMap<>();

    private BankSessionService() {
    }

    public BankSession startSession(ServerPlayer player, UUID accountId, String username, String accountNumber, boolean showUsername) {
        return startSession(player, accountId, username, accountNumber, showUsername, null);
    }

    public BankSession startSession(ServerPlayer player, UUID accountId, String username, String accountNumber, boolean showUsername, UUID loginCardId) {
        Instant now = Instant.now();
        BankSession session = new BankSession(
                player.getUUID(),
                accountId,
                username,
                accountNumber == null ? "" : accountNumber,
                showUsername,
                loginCardId,
                0,
                now,
                now.plus(sessionTimeout())
        );
        sessions.put(player.getUUID(), session);
        return session;
    }

    public Optional<BankSession> findActiveSession(ServerPlayer player) {
        BankSession session = sessions.get(player.getUUID());
        if (session == null) {
            return Optional.empty();
        }

        if (session.isExpired(Instant.now())) {
            sessions.remove(player.getUUID());
            return Optional.empty();
        }

        return Optional.of(session);
    }

    public boolean logout(ServerPlayer player) {
        return sessions.remove(player.getUUID()) != null;
    }

    public int recordInvalidPassword(ServerPlayer player) {
        BankSession updated = sessions.computeIfPresent(player.getUUID(), (ignored, session) -> new BankSession(
                session.playerUuid(), session.accountId(), session.username(), session.accountNumber(),
                session.showUsername(), session.loginCardId(), session.failedPasswordAttempts() + 1,
                session.createdAt(), session.expiresAt()));
        return updated == null ? 0 : updated.failedPasswordAttempts();
    }

    public void resetInvalidPasswords(ServerPlayer player) {
        sessions.computeIfPresent(player.getUUID(), (ignored, session) -> new BankSession(
                session.playerUuid(), session.accountId(), session.username(), session.accountNumber(),
                session.showUsername(), session.loginCardId(), 0, session.createdAt(), session.expiresAt()));
    }

    public void logout(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    public void logout(UUID playerUuid, UUID accountId) {
        sessions.computeIfPresent(playerUuid,
                (ignored, session) -> session.accountId().equals(accountId) ? null : session);
    }

    public void clear() {
        sessions.clear();
    }

    private Duration sessionTimeout() {
        return Duration.ofSeconds(EconomyServerConfig.ECONOMY_SESSION_TIMEOUT_SECONDS.get());
    }
}
