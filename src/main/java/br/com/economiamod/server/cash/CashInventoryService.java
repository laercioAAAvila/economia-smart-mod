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
    /**
     * Upper bound on the exact-change search. Without it an impossible exact payment (for example an odd price paid
     * only with even notes) tried every combination of the notes held - hundreds of millions of steps on the server
     * thread for a large price. With the pruning below a payable amount is found in a handful of steps.
     */
    private static final int MAX_SELECTION_STEPS = 50_000;

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
        int size = denominations.size();
        int[] available = new int[size];
        for (int index = 0; index < size; index++) {
            long denomination = denominations.get(index);
            available[index] = slots.stream()
                    .filter(slot -> slot.value() == denomination)
                    .mapToInt(slot -> slot.stack().getCount())
                    .sum();
        }
        // Value and common divisor of the notes from each index on, to cut branches that can no longer pay exactly.
        long[] valueFrom = new long[size + 1];
        long[] gcdFrom = new long[size + 1];
        for (int index = size - 1; index >= 0; index--) {
            valueFrom[index] = Math.addExact(valueFrom[index + 1], Math.multiplyExact(denominations.get(index), available[index]));
            gcdFrom[index] = gcd(gcdFrom[index + 1], denominations.get(index));
        }
        if (valueFrom[0] < amount) {
            return List.of();
        }
        List<Integer> selectedCounts = new ArrayList<>();
        int[] steps = {0};
        if (!selectCounts(denominations, available, valueFrom, gcdFrom, 0, amount, selectedCounts, steps)) {
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

    private boolean selectCounts(List<Long> denominations, int[] available, long[] valueFrom, long[] gcdFrom,
                                 int index, long remaining, List<Integer> selectedCounts, int[] steps) {
        if (remaining == 0L) {
            while (selectedCounts.size() < denominations.size()) {
                selectedCounts.add(0);
            }
            return true;
        }
        if (index >= denominations.size()
                || remaining > valueFrom[index]
                || remaining % gcdFrom[index] != 0L
                || ++steps[0] > MAX_SELECTION_STEPS) {
            return false;
        }

        long denomination = denominations.get(index);
        int maxCount = Math.toIntExact(Math.min(available[index], remaining / denomination));
        // The notes after this one must still be able to cover what is left.
        long rest = valueFrom[index + 1];
        int minCount = (int) Math.max(0L, Math.min(maxCount, ceilDiv(remaining - rest, denomination)));

        for (int count = maxCount; count >= minCount; count--) {
            selectedCounts.add(count);
            long nextRemaining = remaining - Math.multiplyExact(denomination, count);
            if (selectCounts(denominations, available, valueFrom, gcdFrom, index + 1, nextRemaining, selectedCounts, steps)) {
                return true;
            }
            selectedCounts.remove(selectedCounts.size() - 1);
            if (steps[0] > MAX_SELECTION_STEPS) {
                return false;
            }
        }

        return false;
    }

    private static long ceilDiv(long value, long divisor) {
        return value <= 0L ? 0L : (value + divisor - 1L) / divisor;
    }

    private static long gcd(long a, long b) {
        while (b != 0L) {
            long next = a % b;
            a = b;
            b = next;
        }
        return Math.abs(a);
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
