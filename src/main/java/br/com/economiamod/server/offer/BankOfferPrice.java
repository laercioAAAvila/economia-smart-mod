package br.com.economiamod.server.offer;

import java.util.UUID;

public record BankOfferPrice(
        UUID offerId,
        Long bankBuyPrice,
        Long bankSellPrice,
        boolean buyEnabled,
        boolean sellEnabled
) {
}
