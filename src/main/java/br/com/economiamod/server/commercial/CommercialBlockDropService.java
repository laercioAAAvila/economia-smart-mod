package br.com.economiamod.server.commercial;

import br.com.economiamod.server.commercial.inventory.CommercialInventoryRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventorySlot;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryType;
import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;
import br.com.economiamod.server.player.ItemStackFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class CommercialBlockDropService {
    private final CommercialInventoryRepository inventoryRepository = new CommercialInventoryRepository();
    private final ItemStackFactory itemStackFactory = new ItemStackFactory();

    public void dropStoredItems(ServerLevel level, BlockPos pos, UUID commercialBlockId) throws SQLException {
        List<ItemStack> stacks = new ArrayList<>();
        for (CommercialInventoryType type : CommercialInventoryType.values()) {
            for (CommercialInventorySlot slot : inventoryRepository.loadSlots(commercialBlockId, type)) {
                if (slot.item().isEmpty()) {
                    continue;
                }
                stacks.add(itemStackFactory.create(slot.item(), slot.item().count(), level.registryAccess()));
                if (!inventoryRepository.updateSlot(slot.id(), CommercialItemSnapshot.empty(), slot.version())) {
                    throw new SQLException("Falha ao limpar slot comercial " + slot.id());
                }
            }
        }

        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                pop(level, pos, stack);
            }
        }
    }

    private void pop(ServerLevel level, BlockPos pos, ItemStack stack) {
        net.minecraft.world.level.block.Block.popResource(level, pos, stack);
    }
}
