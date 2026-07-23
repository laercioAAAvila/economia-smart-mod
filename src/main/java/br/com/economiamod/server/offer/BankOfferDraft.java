package br.com.economiamod.server.offer;

import br.com.economiamod.common.pricing.ComparisonMode;
import br.com.economiamod.common.pricing.PricingMode;
import java.util.UUID;

public record BankOfferDraft(
        UUID commercialBlockId,
        int slotIndex,
        String itemId,
        String itemComponents,
        Integer itemDataVersion,
        int quantityPerOperation,
        Long baseBuyPrice,
        Long baseSellPrice,
        Long minimumBuyPrice,
        Long maximumSellPrice,
        Long targetQuantity,
        ComparisonMode comparisonMode,
        PricingMode pricingMode,
        Long quantityPerPriceLevel,
        Integer demandIncreaseBps,
        Integer supplyDecreaseBps,
        int recoveryLevelsPerIdleDay,
        int maximumDemandLevel,
        int maximumSupplyLevel,
        boolean buyEnabled,
        boolean sellEnabled
) {
}
