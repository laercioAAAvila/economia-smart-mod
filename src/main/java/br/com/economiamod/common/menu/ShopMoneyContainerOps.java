package br.com.economiamod.common.menu;

import br.com.economiamod.common.money.MoneyStackCalculator;
import br.com.economiamod.server.cash.CashInventoryService;
import java.util.List;
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
        for (int slot = 0; slot < cashContainer.getContainerSize(); slot++) {
            ItemStack target = cashContainer.getItem(slot);
            if (target.isEmpty()) {
                cashContainer.setItem(slot, payment.copy());
                paymentContainer.setItem(0, ItemStack.EMPTY);
                return true;
            }
            if (ItemStack.isSameItemSameComponents(target, payment) && target.getCount() + payment.getCount() <= target.getMaxStackSize()) {
                target.grow(payment.getCount());
                paymentContainer.setItem(0, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    static boolean removeCashReserve(Container cashContainer, long amount) {
        long remaining = amount;
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
            stack.shrink(count);
            remaining -= value.getAsLong() * count;
            if (remaining == 0L) {
                return true;
            }
        }
        return remaining == 0L;
    }

    static void givePayment(ServerPlayer player, long amount) {
        for (ItemStack stack : buildPaymentStacks(amount)) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static List<ItemStack> buildPaymentStacks(long amount) {
        return new CashInventoryService().buildWithdrawalPlan(amount).stacks();
    }
}
