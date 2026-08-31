package br.com.economiamod.server.transaction;

public final class IdempotencyKeys {
    private static final int MAX_LENGTH = 128;

    private IdempotencyKeys() {
    }

    public static String requireValid(String key) {
        if (key == null) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        String normalized = key.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey must contain 1-128 characters");
        }
        return normalized;
    }
}
