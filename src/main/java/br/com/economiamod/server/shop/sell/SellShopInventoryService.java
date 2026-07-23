package br.com.economiamod.server.shop.sell;

import br.com.economiamod.server.commercial.inventory.CommercialInventoryRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventorySlot;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryType;
import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public final class SellShopInventoryService {
    private static final int SLOT_COUNT = 16;

    private final CommercialInventoryRepository repository = new CommercialInventoryRepository();

    public void ensureInventories(UUID commercialBlockId) throws SQLException {
        repository.ensureSlots(commercialBlockId, CommercialInventoryType.PRODUCT_STOCK, SLOT_COUNT);
        repository.ensureSlots(commercialBlockId, CommercialInventoryType.CASH_RESERVE, SLOT_COUNT);
    }

    public List<CommercialInventorySlot> stock(UUID commercialBlockId) throws SQLException {
        return repository.loadSlots(commercialBlockId, CommercialInventoryType.PRODUCT_STOCK);
    }

    public List<CommercialInventorySlot> cashReserve(UUID commercialBlockId) throws SQLException {
        return repository.loadSlots(commercialBlockId, CommercialInventoryType.CASH_RESERVE);
    }

    public boolean updateStockSlot(UUID slotId, CommercialItemSnapshot item, long expectedVersion) throws SQLException {
        return repository.updateSlot(slotId, item, expectedVersion);
    }

    public boolean updateCashSlot(UUID slotId, CommercialItemSnapshot item, long expectedVersion) throws SQLException {
        return repository.updateSlot(slotId, item, expectedVersion);
    }
}
