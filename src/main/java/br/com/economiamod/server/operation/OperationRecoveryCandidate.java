package br.com.economiamod.server.operation;

import java.util.UUID;

public record OperationRecoveryCandidate(
        UUID operationId,
        String idempotencyKey,
        EconomyOperationType operationType
) {
}
