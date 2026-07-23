package br.com.economiamod.common.credit;

public final class InterestCalculator {
    private static final long BPS_DENOMINATOR = 10_000L;

    private InterestCalculator() {
    }

    public static InterestCalculation calculate(
            InterestMode mode,
            long principalOutstanding,
            long interestOutstanding,
            long previousRemainder,
            int dailyRateBps
    ) {
        if (dailyRateBps < 0) {
            throw new IllegalArgumentException("dailyRateBps cannot be negative");
        }
        if (previousRemainder < 0L) {
            throw new IllegalArgumentException("previousRemainder cannot be negative");
        }

        long base = switch (mode) {
            case SIMPLE -> principalOutstanding;
            case COMPOUND -> CreditMath.debtTotal(principalOutstanding, interestOutstanding);
        };

        long calculated = Math.addExact(Math.multiplyExact(base, dailyRateBps), previousRemainder);
        long interestAmount = calculated / BPS_DENOMINATOR;
        long remainderAfter = calculated % BPS_DENOMINATOR;

        return new InterestCalculation(base, previousRemainder, interestAmount, remainderAfter);
    }
}

