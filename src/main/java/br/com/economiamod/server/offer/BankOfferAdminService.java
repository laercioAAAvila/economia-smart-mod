package br.com.economiamod.server.offer;

import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.common.pricing.PricingMode;
import br.com.economiamod.server.commercial.CommercialBlockTypeRepository;
import java.sql.SQLException;
import java.util.UUID;

public final class BankOfferAdminService {
    private final BankOfferWriteRepository repository = new BankOfferWriteRepository();
    private final CommercialBlockTypeRepository blockTypeRepository = new CommercialBlockTypeRepository();

    public UUID saveOffer(BankOfferDraft draft) throws SQLException {
        validate(draft);
        validatePricingScope(draft);
        return repository.saveAdminOffer(draft);
    }

    public void resetPricing(UUID offerId) throws SQLException {
        repository.resetPricing(offerId);
    }

    private void validate(BankOfferDraft draft) {
        require(draft.commercialBlockId() != null, "commercialBlockId is required");
        require(draft.itemId() != null && !draft.itemId().isBlank(), "itemId is required");
        require(draft.quantityPerOperation() > 0, "quantityPerOperation must be positive");
        require(draft.slotIndex() >= 0 && draft.slotIndex() < 16, "slotIndex must be between 0 and 15");
        require(draft.comparisonMode() != null, "comparisonMode is required");
        require(draft.pricingMode() != null, "pricingMode is required");
        require(draft.recoveryLevelsPerIdleDay() >= 0, "recoveryLevelsPerIdleDay cannot be negative");
        require(draft.maximumDemandLevel() >= 0, "maximumDemandLevel cannot be negative");
        require(draft.maximumSupplyLevel() >= 0, "maximumSupplyLevel cannot be negative");
        validatePrices(draft);
        validateDynamicPricing(draft);
    }

    private void validatePrices(BankOfferDraft draft) {
        if (draft.buyEnabled()) {
            require(draft.baseBuyPrice() != null && draft.baseBuyPrice() >= 0L, "baseBuyPrice is required when buy is enabled");
        }
        if (draft.sellEnabled()) {
            require(draft.baseSellPrice() != null && draft.baseSellPrice() >= 0L, "baseSellPrice is required when sell is enabled");
        }
        require(draft.minimumBuyPrice() == null || draft.minimumBuyPrice() >= 0L, "minimumBuyPrice cannot be negative");
        require(draft.maximumSellPrice() == null || draft.maximumSellPrice() >= 0L, "maximumSellPrice cannot be negative");
        require(draft.targetQuantity() == null || draft.targetQuantity() >= 0L, "targetQuantity cannot be negative");
    }

    private void validateDynamicPricing(BankOfferDraft draft) {
        if (draft.pricingMode() != PricingMode.DYNAMIC) {
            return;
        }
        require(draft.quantityPerPriceLevel() != null && draft.quantityPerPriceLevel() > 0L, "quantityPerPriceLevel must be positive");
        require(draft.demandIncreaseBps() != null && draft.demandIncreaseBps() >= 0, "demandIncreaseBps cannot be negative");
        require(draft.supplyDecreaseBps() != null && draft.supplyDecreaseBps() >= 0, "supplyDecreaseBps cannot be negative");
    }

    private void validatePricingScope(BankOfferDraft draft) throws SQLException {
        CommercialBlockType blockType = blockTypeRepository.findType(draft.commercialBlockId()).orElse(null);
        require(blockType != null, "commercial block must be active");
        if (blockType != CommercialBlockType.BANK_COUNTER) {
            require(draft.pricingMode() == PricingMode.FIXED, "player shops only support fixed prices");
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
