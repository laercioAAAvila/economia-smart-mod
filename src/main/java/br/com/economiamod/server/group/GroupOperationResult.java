package br.com.economiamod.server.group;

import java.util.UUID;

public record GroupOperationResult(boolean success, String code, UUID groupId) {
    public static GroupOperationResult success(UUID groupId) {
        return new GroupOperationResult(true, "success", groupId);
    }

    public static GroupOperationResult denied(String code) {
        return new GroupOperationResult(false, code, null);
    }
}
