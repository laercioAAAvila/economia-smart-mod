package br.com.economiamod.common.money;

import br.com.economiamod.registry.ModItems;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MoneyStackCalculator {
    private MoneyStackCalculator() {
    }

    public static OptionalLong banknoteValue(ItemStack stack) {
        if (stack.is(ModItems.BANKNOTE_1.get())) {
            return OptionalLong.of(1L);
        }
        if (stack.is(ModItems.BANKNOTE_2.get())) {
            return OptionalLong.of(2L);
        }
        if (stack.is(ModItems.BANKNOTE_5.get())) {
            return OptionalLong.of(5L);
        }
        if (stack.is(ModItems.BANKNOTE_10.get())) {
            return OptionalLong.of(10L);
        }
        if (stack.is(ModItems.BANKNOTE_20.get())) {
            return OptionalLong.of(20L);
        }
        if (stack.is(ModItems.BANKNOTE_50.get())) {
            return OptionalLong.of(50L);
        }
        if (stack.is(ModItems.BANKNOTE_100.get())) {
            return OptionalLong.of(100L);
        }
        if (stack.is(ModItems.BANKNOTE_200.get())) {
            return OptionalLong.of(200L);
        }
        return OptionalLong.empty();
    }

    public static boolean isBanknote(ItemStack stack) {
        return banknoteValue(stack).isPresent();
    }

    public static long totalValue(Iterable<ItemStack> stacks) {
        long total = 0L;
        for (ItemStack stack : stacks) {
            OptionalLong value = banknoteValue(stack);
            if (value.isPresent()) {
                total = Math.addExact(total, Math.multiplyExact(value.getAsLong(), stack.getCount()));
            }
        }
        return total;
    }

    public static Map<Item, Integer> buildBanknotes(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("amount must be positive");
        }

        Map<Item, Integer> result = new LinkedHashMap<>();
        long remaining = amount;

        for (MoneyDenomination denomination : MoneyDenomination.DESCENDING) {
            int count = Math.toIntExact(remaining / denomination.value());
            if (count > 0) {
                result.put(itemFor(denomination), count);
                remaining %= denomination.value();
            }
        }

        return result;
    }

    private static Item itemFor(MoneyDenomination denomination) {
        return switch (denomination) {
            case NOTE_1 -> ModItems.BANKNOTE_1.get();
            case NOTE_2 -> ModItems.BANKNOTE_2.get();
            case NOTE_5 -> ModItems.BANKNOTE_5.get();
            case NOTE_10 -> ModItems.BANKNOTE_10.get();
            case NOTE_20 -> ModItems.BANKNOTE_20.get();
            case NOTE_50 -> ModItems.BANKNOTE_50.get();
            case NOTE_100 -> ModItems.BANKNOTE_100.get();
            case NOTE_200 -> ModItems.BANKNOTE_200.get();
        };
    }
}

