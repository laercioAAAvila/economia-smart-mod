package br.com.economiamod.common.pricing;

public final class DynamicPriceCalculator {
    private static final long BPS_DENOMINATOR = 10_000L;

    private DynamicPriceCalculator() {
    }

    public static long bankSellPrice(long baseSellPrice, int demandLevel, int increaseBps, long maximumSellPrice) {
        requireNonNegative(baseSellPrice, "baseSellPrice");
        requireNonNegative(demandLevel, "demandLevel");
        requireNonNegative(increaseBps, "increaseBps");
        requireNonNegative(maximumSellPrice, "maximumSellPrice");

        long multiplier = Math.addExact(BPS_DENOMINATOR, Math.multiplyExact((long) demandLevel, increaseBps));
        long price = divideCeil(Math.multiplyExact(baseSellPrice, multiplier), BPS_DENOMINATOR);
        return maximumSellPrice == 0L ? price : Math.min(price, maximumSellPrice);
    }

    public static long bankBuyPrice(long baseBuyPrice, int supplyLevel, int decreaseBps, long minimumBuyPrice) {
        requireNonNegative(baseBuyPrice, "baseBuyPrice");
        requireNonNegative(supplyLevel, "supplyLevel");
        requireNonNegative(decreaseBps, "decreaseBps");
        requireNonNegative(minimumBuyPrice, "minimumBuyPrice");

        long reduction = Math.multiplyExact((long) supplyLevel, decreaseBps);
        long multiplier = Math.max(0L, BPS_DENOMINATOR - reduction);
        long price = Math.multiplyExact(baseBuyPrice, multiplier) / BPS_DENOMINATOR;
        return Math.max(price, minimumBuyPrice);
    }

    public static int levelForQuantity(long quantity, long quantityPerLevel, int maximumLevel) {
        requireNonNegative(quantity, "quantity");
        if (quantityPerLevel <= 0L) {
            throw new IllegalArgumentException("quantityPerLevel must be positive");
        }
        requireNonNegative(maximumLevel, "maximumLevel");

        long level = quantity / quantityPerLevel;
        return (int) Math.min(level, maximumLevel);
    }

    public static int recoverLevel(int currentLevel, int recoveryLevels) {
        requireNonNegative(currentLevel, "currentLevel");
        requireNonNegative(recoveryLevels, "recoveryLevels");
        return Math.max(0, currentLevel - recoveryLevels);
    }

    private static long divideCeil(long value, long divisor) {
        return Math.floorDiv(Math.addExact(value, divisor - 1L), divisor);
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }
}

