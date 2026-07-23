package br.com.economiamod.server.commercial.inventory;

import java.util.UUID;

public record CommercialInventorySlot(
        UUID id,
        UUID commercialBlockId,
        CommercialInventoryType inventoryType,
        int slotIndex,
        CommercialItemSnapshot item,
        long version
) {
}

