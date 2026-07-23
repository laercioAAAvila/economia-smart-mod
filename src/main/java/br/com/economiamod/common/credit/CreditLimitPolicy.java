package br.com.economiamod.common.credit;

import java.math.BigInteger;

public final class CreditLimitPolicy {
    private static final long FIRST_TIER_MAX_EXCLUSIVE = 250_000L;
    private static final long SECOND_TIER_MAX_EXCLUSIVE = 5_000_000L;
    private static final long THIRD_TIER_MAX_EXCLUSIVE = 500_000_000L;

    private CreditLimitPolicy() {
    }

    public static long limitForBalance(long balance) {
        if (balance <= 0L) {
            return 0L;
        }
        BigInteger value = BigInteger.valueOf(balance)
                .multiply(BigInteger.valueOf(percentForBalance(balance)))
                .divide(BigInteger.valueOf(100L));
        return value.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    public static long effectiveLimit(long balance, long configuredCreditLimit) {
        if (configuredCreditLimit <= 0L) {
            return 0L;
        }
        return Math.min(configuredCreditLimit, limitForBalance(balance));
    }

    public static long percentForBalance(long balance) {
        if (balance < FIRST_TIER_MAX_EXCLUSIVE) {
            return 40L;
        }
        if (balance < SECOND_TIER_MAX_EXCLUSIVE) {
            return 60L;
        }
        if (balance < THIRD_TIER_MAX_EXCLUSIVE) {
            return 80L;
        }
        return 95L;
    }
}
