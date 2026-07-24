package br.com.economiamod.common.menu;

import br.com.economiamod.common.money.MoneyStackCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

final class ShopMoneyContainerOps {
    private ShopMoneyContainerOps() {
    }

    static long moneyIn(Container container) {
        long total = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            var value = MoneyStackCalculator.banknoteValue(stack);
            if (value.isPresent()) {
                total += value.getAsLong() * stack.getCount();
            }
        }
        return total;
    }

    static boolean movePaymentToCashReserve(Container paymentContainer, Container cashContainer) {
        ItemStack payment = paymentContainer.getItem(0);
        if (!canAddToCashReserve(cashContainer, payment)) {
            return false;
        }
        addToCashReserve(cashContainer, payment.copy());
        paymentContainer.setItem(0, ItemStack.EMPTY);
        return true;
    }

    static boolean canAddToCashReserve(Container cashContainer, ItemStack payment) {
        for (int slot = 0; slot < cashContainer.getContainerSize(); slot++) {
            ItemStack target = cashContainer.getItem(slot);
            if (target.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(target, payment) && target.getCount() + payment.getCount() <= target.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    static boolean addToCashReserve(Container cashContainer, ItemStack payment) {
        for (int slot = 0; slot < cashContainer.getContainerSize(); slot++) {
            ItemStack target = cashContainer.getItem(slot);
            if (target.isEmpty()) {
                cashContainer.setItem(slot, payment.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(target, payment) && target.getCount() + payment.getCount() <= target.getMaxStackSize()) {
                target.grow(payment.getCount());
                return true;
            }
        }
        return false;
    }

    static boolean canRemoveCashReserve(Container cashContainer, long amount) {
        long remaining = amount;
        for (int slot = cashContainer.getContainerSize() - 1; slot >= 0; slot--) {
            ItemStack stack = cashContainer.getItem(slot);
            OptionalLong value = MoneyStackCalculator.banknoteValue(stack);
            if (value.isEmpty()) {
                continue;
            }
            int count = Math.toIntExact(Math.min(stack.getCount(), remaining / value.getAsLong()));
            if (count <= 0) {
                continue;
            }
            remaining -= value.getAsLong() * count;
            if (remaining == 0L) {
                return true;
            }
        }
        return remaining == 0L;
    }

    static List<ItemStack> removeCashReserveStacks(Container cashContainer, long amount) {
        if (!canRemoveCashReserve(cashContainer, amount)) {
            return List.of();
        }

        long remaining = amount;
        List<ItemStack> removed = new ArrayList<>();
        for (int slot = cashContainer.getContainerSize() - 1; slot >= 0; slot--) {
            ItemStack stack = cashContainer.getItem(slot);
            var value = MoneyStackCalculator.banknoteValue(stack);
            if (value.isEmpty()) {
                continue;
            }
            int count = Math.toIntExact(Math.min(stack.getCount(), remaining / value.getAsLong()));
            if (count <= 0) {
                continue;
            }
            ItemStack removedStack = stack.copy();
            removedStack.setCount(count);
            removed.add(removedStack);
            stack.shrink(count);
            remaining -= value.getAsLong() * count;
            if (remaining == 0L) {
                return List.copyOf(removed);
            }
        }

        return List.of();
    }

    static void givePayment(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }
}
