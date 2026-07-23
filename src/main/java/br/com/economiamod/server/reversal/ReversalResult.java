package br.com.economiamod.server.reversal;

import java.util.UUID;

public record ReversalResult(
        ReversalResultType type,
        UUID reversalTransactionId
) {
    public static ReversalResult completed(UUID transactionId) {
        return new ReversalResult(ReversalResultType.COMPLETED, transactionId);
    }

    public static ReversalResult duplicate(UUID transactionId) {
        return new ReversalResult(ReversalResultType.DUPLICATE_COMPLETED, transactionId);
    }

    public static ReversalResult invalid(ReversalResultType type) {
        return new ReversalResult(type, null);
    }
}
