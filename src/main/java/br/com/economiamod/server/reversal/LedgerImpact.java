package br.com.economiamod.server.reversal;

import br.com.economiamod.server.transaction.LedgerEntryType;
import java.util.UUID;

public record LedgerImpact(
        UUID accountId,
        LedgerEntryType entryType,
        long amount,
        long balanceBefore,
        long balanceAfter
) {
    public long delta() {
        return balanceAfter - balanceBefore;
    }

    public long reversalDelta() {
        return -delta();
    }
}
