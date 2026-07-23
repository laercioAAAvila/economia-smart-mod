package br.com.economiamod.server.account;

public record AccountBalanceSummary(
        String username,
        String accountNumber,
        long balance,
        long availableBalance,
        long configuredCreditLimit,
        long creditPrincipalOutstanding,
        long creditInterestOutstanding,
        long totalDebt,
        long globalCreditAvailable
) {
}
