package br.com.economiamod.server.cash;

import br.com.economiamod.common.money.BanknoteStackPlan;
import br.com.economiamod.common.money.MoneyStackCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CashInventoryService {
    private static final int BANKNOTE_MAX_STACK_SIZE = 64;

    public long totalBanknotes(ServerPlayer player) {
        long total = 0L;
        for (InventoryBanknoteSlot slot : banknoteSlots(player)) {
            total = Math.addExact(total, Math.multiplyExact(slot.value(), slot.stack().getCount()));
        }
        return total;
    }

    public List<InventoryBanknoteSlot> banknoteSlots(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<InventoryBanknoteSlot> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            OptionalLong value = MoneyStackCalculator.banknoteValue(stack);
            if (value.isPresent()) {
                slots.add(new InventoryBanknoteSlot(slot, value.getAsLong(), stack.copy()));
            }
        }
        return slots;
    }

    public BanknoteStackPlan buildWithdrawalPlan(long amount) {
        Map<Item, Integer> banknotes = MoneyStackCalculator.buildBanknotes(amount);
        return buildWithdrawalPlan(amount, banknotes);
    }

    public BanknoteStackPlan buildWithdrawalPlan(long amount, long banknoteValue) {
        if (amount <= 0L || banknoteValue <= 0L || amount % banknoteValue != 0L) {
            throw new IllegalArgumentException("amount is not compatible with banknote value");
        }
        Item item = MoneyStackCalculator.banknoteItem(banknoteValue)
                .orElseThrow(() -> new IllegalArgumentException("unsupported banknote value"));
        return buildWithdrawalPlan(amount, Map.of(item, Math.toIntExact(amount / banknoteValue)));
    }

    private BanknoteStackPlan buildWithdrawalPlan(long amount, Map<Item, Integer> banknotes) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : banknotes.entrySet()) {
            int remaining = entry.getValue();
            while (remaining > 0) {
                int count = Math.min(remaining, BANKNOTE_MAX_STACK_SIZE);
                stacks.add(new ItemStack(entry.getKey(), count));
                remaining -= count;
            }
        }
        return new BanknoteStackPlan(amount, List.copyOf(stacks));
    }

    public boolean canInsert(ServerPlayer player, BanknoteStackPlan plan) {
        List<InventorySlotCapacity> capacities = inventoryCapacities(player);
        for (ItemStack stack : plan.stacks()) {
            int remaining = stack.getCount();
            for (InventorySlotCapacity capacity : capacities) {
                if (!capacity.accepts(stack.getItem())) {
                    continue;
                }

                int accepted = Math.min(remaining, capacity.available());
                capacity.consume(stack.getItem(), accepted);
                remaining -= accepted;
                if (remaining == 0) {
                    break;
                }
            }

            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    public void insert(ServerPlayer player, BanknoteStackPlan plan) {
        for (ItemStack stack : plan.stacks()) {
            ItemStack remaining = stack.copy();
            boolean inserted = player.getInventory().add(remaining);
            if (!inserted && !remaining.isEmpty()) {
                player.drop(remaining, false);
            }
        }
    }

    public boolean canRemoveExactValue(ServerPlayer player, long amount) {
        return amount == 0L || !selectBanknotes(player, amount).isEmpty();
    }

    public boolean removeExactValue(ServerPlayer player, long amount) {
        List<InventoryBanknoteSlot> selected = selectBanknotes(player, amount);
        if (selected.isEmpty() && amount > 0L) {
            return false;
        }

        long remaining = amount;
        Inventory inventory = player.getInventory();
        for (InventoryBanknoteSlot selectedSlot : selected) {
            ItemStack liveStack = inventory.items.get(selectedSlot.slot());
            int removeCount = Math.toIntExact(Math.min(selectedSlot.stack().getCount(), remaining / selectedSlot.value()));
            if (removeCount <= 0) {
                continue;
            }

            liveStack.shrink(removeCount);
            remaining -= Math.multiplyExact(selectedSlot.value(), removeCount);
            if (remaining == 0L) {
                return true;
            }
        }

        return remaining == 0L;
    }

    private List<InventoryBanknoteSlot> selectBanknotes(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            return List.of();
        }

        List<InventoryBanknoteSlot> slots = banknoteSlots(player).stream()
                .sorted(Comparator.comparingLong(InventoryBanknoteSlot::value).reversed())
                .toList();

        List<Long> denominations = slots.stream()
                .map(InventoryBanknoteSlot::value)
                .distinct()
                .toList();
        List<Integer> selectedCounts = new ArrayList<>();
        if (!selectCounts(slots, denominations, 0, amount, selectedCounts)) {
            return List.of();
        }

        List<InventoryBanknoteSlot> selected = new ArrayList<>();
        for (int index = 0; index < denominations.size(); index++) {
            long denomination = denominations.get(index);
            int remainingCount = selectedCounts.get(index);
            if (remainingCount <= 0) {
                continue;
            }

            for (InventoryBanknoteSlot slot : slots) {
                if (slot.value() != denomination || remainingCount <= 0) {
                    continue;
                }

                int count = Math.min(slot.stack().getCount(), remainingCount);
                selected.add(new InventoryBanknoteSlot(slot.slot(), slot.value(), new ItemStack(slot.stack().getItem(), count)));
                remainingCount -= count;
            }
        }

        return selected;
    }

    private boolean selectCounts(List<InventoryBanknoteSlot> slots, List<Long> denominations, int index, long remaining, List<Integer> selectedCounts) {
        if (remaining == 0L) {
            while (selectedCounts.size() < denominations.size()) {
                selectedCounts.add(0);
            }
            return true;
        }
        if (index >= denominations.size()) {
            return false;
        }

        long denomination = denominations.get(index);
        int availableCount = slots.stream()
                .filter(slot -> slot.value() == denomination)
                .mapToInt(slot -> slot.stack().getCount())
                .sum();
        int maxCount = Math.toIntExact(Math.min(availableCount, remaining / denomination));

        for (int count = maxCount; count >= 0; count--) {
            selectedCounts.add(count);
            long nextRemaining = remaining - Math.multiplyExact(denomination, count);
            if (selectCounts(slots, denominations, index + 1, nextRemaining, selectedCounts)) {
                return true;
            }
            selectedCounts.remove(selectedCounts.size() - 1);
        }

        return false;
    }

    private List<InventorySlotCapacity> inventoryCapacities(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<InventorySlotCapacity> capacities = new ArrayList<>();
        for (ItemStack stack : inventory.items) {
            if (stack.isEmpty()) {
                capacities.add(InventorySlotCapacity.empty());
            } else if (MoneyStackCalculator.isBanknote(stack)) {
                capacities.add(InventorySlotCapacity.existing(stack.getItem(), stack.getMaxStackSize() - stack.getCount()));
            }
        }
        return capacities;
    }

    private static final class InventorySlotCapacity {
        private Item item;
        private int available;

        private InventorySlotCapacity(Item item, int available) {
            this.item = item;
            this.available = available;
        }

        static InventorySlotCapacity empty() {
            return new InventorySlotCapacity(null, 64);
        }

        static InventorySlotCapacity existing(Item item, int available) {
            return new InventorySlotCapacity(item, Math.max(0, available));
        }

        boolean accepts(Item candidate) {
            return item == null || item == candidate;
        }

        int available() {
            return available;
        }

        void consume(Item candidate, int count) {
            if (count <= 0) {
                return;
            }
            if (item == null) {
                item = candidate;
            }
            available = Math.max(0, available - count);
        }
    }
}
