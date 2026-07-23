package br.com.economiamod.server.transaction;

public record PaymentAccountSnapshot(
        String status,
        long balance,
        long configuredCreditLimit,
        long principalOutstanding,
        long interestOutstanding
) {
}

