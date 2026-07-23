package br.com.economiamod.common.credit;

public final class CreditMath {
    private CreditMath() {
    }

    public static long debtTotal(long principalOutstanding, long interestOutstanding) {
        requireNonNegative(principalOutstanding, "principalOutstanding");
        requireNonNegative(interestOutstanding, "interestOutstanding");
        return Math.addExact(principalOutstanding, interestOutstanding);
    }

    public static long availableBalance(long balance, long principalOutstanding, long interestOutstanding) {
        requireNonNegative(balance, "balance");
        long debtTotal = debtTotal(principalOutstanding, interestOutstanding);
        return Math.max(0L, balance - debtTotal);
    }

    public static long globalCreditAvailable(long configuredCreditLimit, long principalOutstanding, long interestOutstanding) {
        requireNonNegative(configuredCreditLimit, "configuredCreditLimit");
        long debtTotal = debtTotal(principalOutstanding, interestOutstanding);
        return Math.max(0L, configuredCreditLimit - debtTotal);
    }

    public static long individualCreditAvailable(
            long individualCreditLimit,
            long cardPrincipalOutstanding,
            long cardInterestOutstanding,
            long globalCreditAvailable
    ) {
        requireNonNegative(individualCreditLimit, "individualCreditLimit");
        requireNonNegative(globalCreditAvailable, "globalCreditAvailable");
        long cardDebtTotal = debtTotal(cardPrincipalOutstanding, cardInterestOutstanding);
        return Math.max(0L, Math.min(individualCreditLimit - cardDebtTotal, globalCreditAvailable));
    }

    public static long clampConfiguredLimitToBalance(long configuredCreditLimit, long balance) {
        requireNonNegative(configuredCreditLimit, "configuredCreditLimit");
        requireNonNegative(balance, "balance");
        return Math.min(configuredCreditLimit, balance);
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }
}

