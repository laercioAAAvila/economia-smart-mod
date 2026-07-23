package br.com.economiamod.server.player;

import br.com.economiamod.common.pricing.ComparisonMode;
import br.com.economiamod.server.commercial.inventory.CommercialItemMatcher;
import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;
import br.com.economiamod.server.commercial.inventory.ItemStackSnapshotMapper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PlayerItemInventoryService {
    private final CommercialItemMatcher matcher = new CommercialItemMatcher();
    private final ItemStackSnapshotMapper mapper = new ItemStackSnapshotMapper();
    private final ItemStackFactory stackFactory = new ItemStackFactory();

    public boolean canInsert(ServerPlayer player, CommercialItemSnapshot item, int count) {
        ItemStack candidate = stackFactory.create(item, count, player.registryAccess());
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                remaining -= candidate.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(stack, candidate)) {
                remaining -= Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    public void insert(ServerPlayer player, CommercialItemSnapshot item, int count) {
        ItemStack stack = stackFactory.create(item, count, player.registryAccess());
        boolean added = player.getInventory().add(stack);
        if (!added && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    public boolean hasMatching(ServerPlayer player, CommercialItemSnapshot expected, ComparisonMode mode, int count) {
        return matchingCount(player, expected, mode) >= count;
    }

    public boolean removeMatching(ServerPlayer player, CommercialItemSnapshot expected, ComparisonMode mode, int count) {
        if (!hasMatching(player, expected, mode, count)) {
            return false;
        }
        int remaining = count;
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (!matcher.matches(mapper.fromStack(stack, player.registryAccess()), expected, mode)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
            if (remaining == 0) {
                return true;
            }
        }
        return false;
    }

    private int matchingCount(ServerPlayer player, CommercialItemSnapshot expected, ComparisonMode mode) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matcher.matches(mapper.fromStack(stack, player.registryAccess()), expected, mode)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
