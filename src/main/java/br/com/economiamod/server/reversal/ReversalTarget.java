package br.com.economiamod.server.reversal;

import java.util.UUID;

public record ReversalTarget(
        UUID transactionId,
        String status,
        long amount,
        UUID initiatorPlayerUuid
) {
}
