package br.com.economiamod.server.offer;

import br.com.economiamod.common.pricing.ComparisonMode;
import br.com.economiamod.common.pricing.PricingMode;
import java.time.LocalDate;
import java.util.UUID;

public record BankOfferSnapshot(
        UUID id,
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
        long purchasedQuantity,
        ComparisonMode comparisonMode,
        PricingMode pricingMode,
        int demandLevel,
        int supplyLevel,
        Long quantityPerPriceLevel,
        Integer demandIncreaseBps,
        Integer supplyDecreaseBps,
        int recoveryLevelsPerIdleDay,
        int maximumDemandLevel,
        int maximumSupplyLevel,
        LocalDate lastPlayerPurchaseDate,
        LocalDate lastPlayerSaleDate,
        boolean buyEnabled,
        boolean sellEnabled,
        long version
) {
    public BankOfferSnapshot withDemandLevel(int demandLevel) {
        return new BankOfferSnapshot(id, commercialBlockId, slotIndex, itemId, itemComponents, itemDataVersion,
                quantityPerOperation, baseBuyPrice, baseSellPrice, minimumBuyPrice, maximumSellPrice,
                targetQuantity, purchasedQuantity, comparisonMode, pricingMode, demandLevel, supplyLevel,
                quantityPerPriceLevel, demandIncreaseBps, supplyDecreaseBps, recoveryLevelsPerIdleDay,
                maximumDemandLevel, maximumSupplyLevel, lastPlayerPurchaseDate, lastPlayerSaleDate,
                buyEnabled, sellEnabled, version);
    }

    public BankOfferSnapshot withSupplyLevel(int supplyLevel) {
        return new BankOfferSnapshot(id, commercialBlockId, slotIndex, itemId, itemComponents, itemDataVersion,
                quantityPerOperation, baseBuyPrice, baseSellPrice, minimumBuyPrice, maximumSellPrice,
                targetQuantity, purchasedQuantity, comparisonMode, pricingMode, demandLevel, supplyLevel,
                quantityPerPriceLevel, demandIncreaseBps, supplyDecreaseBps, recoveryLevelsPerIdleDay,
                maximumDemandLevel, maximumSupplyLevel, lastPlayerPurchaseDate, lastPlayerSaleDate,
                buyEnabled, sellEnabled, version);
    }
}
