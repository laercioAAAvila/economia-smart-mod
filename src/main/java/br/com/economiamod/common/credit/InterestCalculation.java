package br.com.economiamod.common.credit;

public record InterestCalculation(
        long calculationBase,
        long remainderBefore,
        long interestAmount,
        long remainderAfter
) {
}

