package br.com.economiamod.server.gold;

public record GoldReserveAdjustmentResult(
        GoldReserveAdjustmentResultType type,
        long reserveBefore,
        long reserveAfter
) {
    public static GoldReserveAdjustmentResult completed(long before, long after) {
        return new GoldReserveAdjustmentResult(GoldReserveAdjustmentResultType.COMPLETED, before, after);
    }

    public static GoldReserveAdjustmentResult insufficient(long before) {
        return new GoldReserveAdjustmentResult(GoldReserveAdjustmentResultType.INSUFFICIENT_RESERVE, before, before);
    }
}
