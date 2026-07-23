package br.com.economiamod.server.shop.buy;

import br.com.economiamod.common.pricing.ComparisonMode;
import br.com.economiamod.common.pricing.PricingMode;
import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;
import br.com.economiamod.server.commercial.inventory.ItemStackSnapshotMapper;
import br.com.economiamod.server.offer.BankOfferAdminService;
import br.com.economiamod.server.offer.BankOfferDraft;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

public final class BuyShopReferenceService {
    private final ItemStackSnapshotMapper mapper = new ItemStackSnapshotMapper();
    private final BankOfferAdminService offerAdminService = new BankOfferAdminService();

    public UUID saveFixedReference(UUID commercialBlockId, int slotIndex, ItemStack referenceStack, HolderLookup.Provider registries, long baseBuyPrice) throws SQLException {
        CommercialItemSnapshot item = requireItem(referenceStack, registries);
        return offerAdminService.saveOffer(new BankOfferDraft(
                commercialBlockId,
                slotIndex,
                item.itemId(),
                item.components(),
                item.dataVersion(),
                item.count(),
                baseBuyPrice,
                null,
                null,
                null,
                null,
                ComparisonMode.FULL_COMPONENTS,
                PricingMode.FIXED,
                null,
                null,
                null,
                0,
                0,
                0,
                true,
                false
        ));
    }

    public UUID saveDynamicReference(UUID commercialBlockId, int slotIndex, ItemStack referenceStack, HolderLookup.Provider registries, long baseBuyPrice, long minimumBuyPrice) throws SQLException {
        CommercialItemSnapshot item = requireItem(referenceStack, registries);
        return offerAdminService.saveOffer(new BankOfferDraft(
                commercialBlockId,
                slotIndex,
                item.itemId(),
                item.components(),
                item.dataVersion(),
                item.count(),
                baseBuyPrice,
                null,
                minimumBuyPrice,
                null,
                null,
                ComparisonMode.FULL_COMPONENTS,
                PricingMode.FIXED,
                null,
                null,
                null,
                1,
                0,
                0,
                true,
                false
        ));
    }

    private CommercialItemSnapshot requireItem(ItemStack referenceStack, HolderLookup.Provider registries) {
        CommercialItemSnapshot item = mapper.fromStack(referenceStack, registries);
        if (item.isEmpty()) {
            throw new IllegalArgumentException("referenceStack cannot be empty");
        }
        return item;
    }
}
