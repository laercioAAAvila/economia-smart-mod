package br.com.economiamod.server.shop.sell;

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

public final class SellShopOfferService {
    private final ItemStackSnapshotMapper mapper = new ItemStackSnapshotMapper();
    private final BankOfferAdminService offerAdminService = new BankOfferAdminService();

    public UUID saveProductOffer(UUID commercialBlockId, int slotIndex, ItemStack productStack, HolderLookup.Provider registries, long sellPrice) throws SQLException {
        CommercialItemSnapshot item = mapper.fromStack(productStack, registries);
        if (item.isEmpty()) {
            throw new IllegalArgumentException("productStack cannot be empty");
        }
        return offerAdminService.saveOffer(new BankOfferDraft(
                commercialBlockId,
                slotIndex,
                item.itemId(),
                item.components(),
                item.dataVersion(),
                item.count(),
                null,
                sellPrice,
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
                false,
                true
        ));
    }
}
