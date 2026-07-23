package br.com.economiamod.server.interest;

public record InterestAccrualResult(int processedCards, int skippedCards, long interestAmount) {
}

