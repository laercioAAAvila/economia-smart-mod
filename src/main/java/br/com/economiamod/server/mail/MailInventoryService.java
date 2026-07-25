package br.com.economiamod.server.mail;

import br.com.economiamod.server.commercial.inventory.CommercialInventoryRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryRepository.SlotUpdate;
import br.com.economiamod.server.commercial.inventory.CommercialInventorySlot;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryType;
import br.com.economiamod.server.commercial.inventory.ItemStackSnapshotMapper;
import br.com.economiamod.server.player.ItemStackFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class MailInventoryService {
    public static final int RECEIVED_SLOTS = 18;

    private final CommercialInventoryRepository inventoryRepository = new CommercialInventoryRepository();
    private final ItemStackSnapshotMapper snapshotMapper = new ItemStackSnapshotMapper();
    private final ItemStackFactory itemStackFactory = new ItemStackFactory();

    public SimpleContainer loadReceived(UUID blockId, HolderLookup.Provider registries) throws SQLException {
        inventoryRepository.ensureSlots(blockId, CommercialInventoryType.MAIL_RECEIVED, RECEIVED_SLOTS);
        SimpleContainer container = new SimpleContainer(RECEIVED_SLOTS);
        for (CommercialInventorySlot slot : inventoryRepository.loadSlots(blockId, CommercialInventoryType.MAIL_RECEIVED)) {
            if (!slot.item().isEmpty() && slot.slotIndex() >= 0 && slot.slotIndex() < RECEIVED_SLOTS) {
                container.setItem(slot.slotIndex(), itemStackFactory.create(slot.item(), slot.item().count(), registries));
            }
        }
        return container;
    }

    public void saveReceived(UUID blockId, Container container, HolderLookup.Provider registries) throws SQLException {
        inventoryRepository.ensureSlots(blockId, CommercialInventoryType.MAIL_RECEIVED, RECEIVED_SLOTS);
        List<CommercialInventorySlot> slots = inventoryRepository.loadSlots(blockId, CommercialInventoryType.MAIL_RECEIVED);
        List<SlotUpdate> updates = new ArrayList<>();
        for (CommercialInventorySlot slot : slots) {
            if (slot.slotIndex() >= 0 && slot.slotIndex() < container.getContainerSize()) {
                updates.add(new SlotUpdate(slot.id(), snapshotMapper.fromStack(container.getItem(slot.slotIndex()), registries), slot.version()));
            }
        }
        inventoryRepository.updateSlots(updates);
    }

    public boolean insertShipment(UUID destinationBlockId, Container shipment, HolderLookup.Provider registries) throws SQLException {
        SimpleContainer received = loadReceived(destinationBlockId, registries);
        if (!canInsertAll(received, shipment)) {
            return false;
        }
        for (int slot = 0; slot < shipment.getContainerSize(); slot++) {
            ItemStack stack = shipment.getItem(slot);
            if (!stack.isEmpty()) {
                insert(received, stack.copy());
            }
        }
        saveReceived(destinationBlockId, received, registries);
        return true;
    }

    public boolean canInsertAll(Container received, Container shipment) {
        SimpleContainer simulation = new SimpleContainer(received.getContainerSize());
        for (int slot = 0; slot < received.getContainerSize(); slot++) {
            simulation.setItem(slot, received.getItem(slot).copy());
        }
        for (int slot = 0; slot < shipment.getContainerSize(); slot++) {
            ItemStack stack = shipment.getItem(slot);
            if (!stack.isEmpty() && !insert(simulation, stack.copy())) {
                return false;
            }
        }
        return true;
    }

    public void giveContainer(ServerPlayer player, Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack returned = stack.copy();
            container.setItem(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
        }
    }

    private boolean insert(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack target = container.getItem(slot);
            if (!target.isEmpty() && ItemStack.isSameItemSameComponents(target, stack)) {
                int accepted = Math.min(stack.getCount(), target.getMaxStackSize() - target.getCount());
                if (accepted > 0) {
                    target.grow(accepted);
                    stack.shrink(accepted);
                }
            }
            if (stack.isEmpty()) {
                return true;
            }
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, stack.copy());
                stack.setCount(0);
                return true;
            }
        }
        return stack.isEmpty();
    }
}
