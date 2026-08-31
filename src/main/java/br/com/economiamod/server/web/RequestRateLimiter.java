package br.com.economiamod.server.web;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RequestRateLimiter {
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    boolean allowAndRecord(String key, int maxRequests, int windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long cutoff = now - windowSeconds * 1000L;
        Deque<Long> entries = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (entries) {
            while (!entries.isEmpty() && entries.peekFirst() < cutoff) {
                entries.removeFirst();
            }
            if (entries.size() >= maxRequests) {
                return false;
            }
            entries.addLast(now);
            return true;
        }
    }

    void clear() {
        requests.clear();
    }
}
