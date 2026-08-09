package br.com.economiamod.server.claim;

import java.util.UUID;

public record ClaimOperationResult(boolean success, String code, UUID id) {
    public static ClaimOperationResult success(UUID id) {
        return new ClaimOperationResult(true, "success", id);
    }

    public static ClaimOperationResult denied(String code) {
        return new ClaimOperationResult(false, code, null);
    }
}
