package br.com.economiamod.server.web;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class WebApiSessionService {
    private static final int TOKEN_BYTES = 32;
    private static final int MAX_SESSIONS = 10_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    String create(UUID accountId, UUID playerUuid, int timeoutSeconds) {
        purgeExpired();
        if (sessions.size() >= MAX_SESSIONS) {
            throw new IllegalStateException("web session capacity reached");
        }
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(accountId, playerUuid, Instant.now().plusSeconds(timeoutSeconds)));
        return token;
    }

    Optional<Session> find(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(token, session);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    void clear() {
        sessions.clear();
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    record Session(UUID accountId, UUID playerUuid, Instant expiresAt) {
    }
}
