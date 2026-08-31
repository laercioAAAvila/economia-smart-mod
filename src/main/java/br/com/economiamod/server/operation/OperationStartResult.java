package br.com.economiamod.server.operation;

import java.util.UUID;

public record OperationStartResult(OperationStartType type, UUID operationId, EconomyOperationState state) {
}
