package br.com.economiamod.server.operation;

public record OperationRecoveryResult(
        int rolledBack,
        int completed,
        int rollbackRequired,
        int financiallyReversed
) {
    public int totalTouched() {
        return rolledBack + completed + rollbackRequired + financiallyReversed;
    }
}
