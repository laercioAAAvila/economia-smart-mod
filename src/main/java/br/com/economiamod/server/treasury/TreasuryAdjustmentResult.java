package br.com.economiamod.server.treasury;

public record TreasuryAdjustmentResult(
        TreasuryAdjustmentResultType type,
        long balanceBefore,
        long balanceAfter
) {
    public static TreasuryAdjustmentResult completed(long balanceBefore, long balanceAfter) {
        return new TreasuryAdjustmentResult(TreasuryAdjustmentResultType.COMPLETED, balanceBefore, balanceAfter);
    }

    public static TreasuryAdjustmentResult insufficient(long balanceBefore) {
        return new TreasuryAdjustmentResult(TreasuryAdjustmentResultType.INSUFFICIENT_TREASURY_BALANCE, balanceBefore, balanceBefore);
    }
}
