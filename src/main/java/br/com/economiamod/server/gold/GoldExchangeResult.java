package br.com.economiamod.server.gold;

public record GoldExchangeResult(GoldExchangeResultType type, long goldNuggetUnits, long moneyAmount, long balanceAfter) {
    public static GoldExchangeResult completed(long goldNuggetUnits, long moneyAmount, long balanceAfter) {
        return new GoldExchangeResult(GoldExchangeResultType.COMPLETED, goldNuggetUnits, moneyAmount, balanceAfter);
    }

    public static GoldExchangeResult duplicateCompleted(long moneyAmount, long balanceAfter) {
        return new GoldExchangeResult(GoldExchangeResultType.DUPLICATE_COMPLETED, 0L, moneyAmount, balanceAfter);
    }

    public static GoldExchangeResult invalid(GoldExchangeResultType type) {
        return new GoldExchangeResult(type, 0L, 0L, 0L);
    }
}

