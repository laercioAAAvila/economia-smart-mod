package br.com.economiamod.server.group;

import br.com.economiamod.server.config.EconomyServerConfig;
import java.math.BigInteger;

public final class ClaimUpgradePricingService {
    private static final BigInteger BASIS = BigInteger.valueOf(10_000L);

    public ClaimUpgradeQuote quote(int storedLimit) {
        int minimum = EconomyServerConfig.CLAIM_MIN_CHUNKS.get();
        int maximum = EconomyServerConfig.CLAIM_MAX_CHUNKS.get();
        int minimumPercentage = EconomyServerConfig.CLAIM_UPGRADE_MIN_PERCENTAGE.get();
        int maximumPercentage = EconomyServerConfig.CLAIM_UPGRADE_MAX_PERCENTAGE.get();
        boolean valid = maximum >= minimum && maximumPercentage >= minimumPercentage;
        int current = Math.max(minimum, Math.min(storedLimit, Math.max(minimum, maximum)));
        if (!valid || current >= maximum) {
            return new ClaimUpgradeQuote(current, current, maximum, 0, 0L, current >= maximum, valid);
        }

        int upgradeCount = maximum - minimum;
        int upgradeIndex = current - minimum;
        long price = EconomyServerConfig.CLAIM_UPGRADE_BASE_PRICE.get();
        int percentage = 0;
        for (int index = 0; index <= upgradeIndex; index++) {
            percentage = percentageBasisPoints(index, upgradeCount, minimumPercentage, maximumPercentage);
            price = applyPercentage(price, percentage);
        }
        return new ClaimUpgradeQuote(current, current + 1, maximum, percentage, price, false, true);
    }

    private int percentageBasisPoints(int index, int count, int minimumPercentage, int maximumPercentage) {
        long minimum = Math.multiplyExact((long) minimumPercentage, 100L);
        long maximum = Math.multiplyExact((long) maximumPercentage, 100L);
        if (count <= 1) {
            return Math.toIntExact(minimum);
        }
        BigInteger numerator = BigInteger.valueOf(maximum - minimum).multiply(BigInteger.valueOf(index));
        long interpolated = divideHalfUp(numerator, BigInteger.valueOf(count - 1)).longValueExact();
        return Math.toIntExact(minimum + interpolated);
    }

    private long applyPercentage(long previousPrice, int percentageBasisPoints) {
        BigInteger numerator = BigInteger.valueOf(previousPrice)
                .multiply(BASIS.add(BigInteger.valueOf(percentageBasisPoints)));
        BigInteger rounded = divideHalfUp(numerator, BASIS);
        return rounded.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? Long.MAX_VALUE : rounded.longValueExact();
    }

    private BigInteger divideHalfUp(BigInteger numerator, BigInteger denominator) {
        BigInteger[] result = numerator.divideAndRemainder(denominator);
        return result[1].shiftLeft(1).compareTo(denominator) >= 0 ? result[0].add(BigInteger.ONE) : result[0];
    }
}
