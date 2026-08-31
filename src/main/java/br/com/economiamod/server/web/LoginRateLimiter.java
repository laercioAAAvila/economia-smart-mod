package br.com.economiamod.server.web;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class LoginRateLimiter {
    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    boolean allow(String key, int maxAttempts, int windowSeconds) {
        long cutoff = Instant.now().minusSeconds(windowSeconds).toEpochMilli();
        Deque<Long> entries = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (entries) {
            while (!entries.isEmpty() && entries.peekFirst() < cutoff) {
                entries.removeFirst();
            }
            return entries.size() < maxAttempts;
        }
    }

    void recordFailure(String key, int windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long cutoff = now - windowSeconds * 1000L;
        Deque<Long> entries = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (entries) {
            while (!entries.isEmpty() && entries.peekFirst() < cutoff) {
                entries.removeFirst();
            }
            entries.addLast(now);
        }
    }

    void clear(String key) {
        failures.remove(key);
    }
}
