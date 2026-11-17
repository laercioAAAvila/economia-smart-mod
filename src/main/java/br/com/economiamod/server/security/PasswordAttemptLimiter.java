package br.com.economiamod.server.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits password work done for ATM packets. Every password check runs PBKDF2 (600 000 iterations, ~0.3 s) on the
 * server thread, and the ATM login had no limit: a modified client could try passwords for any username without end
 * and, by sending many packets, hold the server thread busy.
 *
 * <ul>
 *   <li>at most {@value #HASHES_PER_WINDOW} password checks per player every {@value #HASH_WINDOW_SECONDS} s, for any
 *   action that hashes or verifies a password;</li>
 *   <li>a username with {@value #FAILURES_PER_USERNAME} failed logins in {@value #FAILURE_WINDOW_SECONDS} s is locked
 *   for that window, whoever tries it;</li>
 *   <li>a player with {@value #FAILURES_PER_PLAYER} failed logins in that window cannot try any username.</li>
 * </ul>
 */
public final class PasswordAttemptLimiter {
    public static final PasswordAttemptLimiter INSTANCE = new PasswordAttemptLimiter();

    static final int HASHES_PER_WINDOW = 4;
    static final int HASH_WINDOW_SECONDS = 10;
    static final int FAILURES_PER_USERNAME = 5;
    static final int FAILURES_PER_PLAYER = 10;
    static final int FAILURE_WINDOW_SECONDS = 300;

    private final Map<String, Deque<Long>> events = new ConcurrentHashMap<>();

    private PasswordAttemptLimiter() {
    }

    /** Takes one password check from the player's budget; false means the packet must be refused unhashed. */
    public boolean tryHash(UUID player) {
        return tryRecord("hash:" + player, HASHES_PER_WINDOW, HASH_WINDOW_SECONDS, System.currentTimeMillis());
    }

    /** False while the username or the player is locked after too many failed logins. */
    public boolean loginAllowed(UUID player, String normalizedUsername) {
        long now = System.currentTimeMillis();
        return count("user:" + normalizedUsername, FAILURE_WINDOW_SECONDS, now) < FAILURES_PER_USERNAME
                && count("player:" + player, FAILURE_WINDOW_SECONDS, now) < FAILURES_PER_PLAYER;
    }

    public void recordLoginFailure(UUID player, String normalizedUsername) {
        long now = System.currentTimeMillis();
        record("user:" + normalizedUsername, FAILURE_WINDOW_SECONDS, now);
        record("player:" + player, FAILURE_WINDOW_SECONDS, now);
    }

    public void clearLoginFailures(UUID player, String normalizedUsername) {
        events.remove("user:" + normalizedUsername);
        events.remove("player:" + player);
    }

    public void forget(UUID player) {
        events.remove("hash:" + player);
    }

    private boolean tryRecord(String key, int max, int windowSeconds, long now) {
        Deque<Long> entries = events.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (entries) {
            prune(entries, windowSeconds, now);
            if (entries.size() >= max) {
                return false;
            }
            entries.addLast(now);
            return true;
        }
    }

    private int count(String key, int windowSeconds, long now) {
        Deque<Long> entries = events.get(key);
        if (entries == null) {
            return 0;
        }
        synchronized (entries) {
            prune(entries, windowSeconds, now);
            return entries.size();
        }
    }

    private void record(String key, int windowSeconds, long now) {
        Deque<Long> entries = events.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (entries) {
            prune(entries, windowSeconds, now);
            entries.addLast(now);
        }
    }

    private static void prune(Deque<Long> entries, int windowSeconds, long now) {
        long cutoff = now - windowSeconds * 1000L;
        while (!entries.isEmpty() && entries.peekFirst() < cutoff) {
            entries.removeFirst();
        }
    }
}
