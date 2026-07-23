package br.com.economiamod.server.shop.sell;

import br.com.economiamod.server.commercial.inventory.CommercialInventoryMutationRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventorySlot;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryType;
import br.com.economiamod.server.commercial.inventory.CommercialItemMatcher;
import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;
import br.com.economiamod.server.offer.BankOfferSnapshot;
import br.com.economiamod.server.offer.OfferItemSnapshotMapper;
import java.sql.Connection;
import java.sql.SQLException;

public final class SellShopStockService {
    private final CommercialInventoryMutationRepository inventoryRepository = new CommercialInventoryMutationRepository();
    private final CommercialItemMatcher matcher = new CommercialItemMatcher();
    private final OfferItemSnapshotMapper offerItemMapper = new OfferItemSnapshotMapper();

    public boolean hasStock(Connection connection, BankOfferSnapshot offer) throws SQLException {
        CommercialInventorySlot slot = inventoryRepository.lockSlot(
                connection,
                offer.commercialBlockId(),
                CommercialInventoryType.PRODUCT_STOCK,
                stockSlotIndex(offer)
        ).orElse(null);
        return slot != null
                && matcher.matches(slot.item(), offerItemMapper.item(offer), offer.comparisonMode())
                && slot.item().count() >= offer.quantityPerOperation();
    }

    public boolean removeStock(Connection connection, BankOfferSnapshot offer) throws SQLException {
        CommercialInventorySlot slot = inventoryRepository.lockSlot(
                connection,
                offer.commercialBlockId(),
                CommercialInventoryType.PRODUCT_STOCK,
                stockSlotIndex(offer)
        ).orElse(null);
        if (slot == null || !matcher.matches(slot.item(), offerItemMapper.item(offer), offer.comparisonMode())) {
            return false;
        }
        return inventoryRepository.removeFromSlot(connection, slot, offer.quantityPerOperation());
    }

    public CommercialItemSnapshot item(BankOfferSnapshot offer) {
        return offerItemMapper.item(offer);
    }

    private int stockSlotIndex(BankOfferSnapshot offer) {
        return offer.slotIndex() + 1;
    }
}
