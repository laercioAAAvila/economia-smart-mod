package br.com.economiamod.server.group;

public record ClaimUpgradeQuote(int currentLimit, int nextLimit, int maxLimit,
                                int percentageBasisPoints, long price,
                                boolean maximumReached, boolean configurationValid) {
}
