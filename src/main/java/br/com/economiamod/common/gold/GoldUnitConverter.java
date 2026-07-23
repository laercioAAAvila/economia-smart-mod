package br.com.economiamod.common.gold;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GoldUnitConverter {
    public static final long NUGGET_UNITS_PER_NUGGET = 1L;
    public static final long NUGGET_UNITS_PER_INGOT = 9L;
    public static final long NUGGET_UNITS_PER_BLOCK = 81L;

    private GoldUnitConverter() {
    }

    public static boolean isMonetaryGold(ItemStack stack) {
        return stack.is(Items.GOLD_NUGGET) || stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLD_BLOCK);
    }

    public static long nuggetUnits(ItemStack stack) {
        long unitValue;
        if (stack.is(Items.GOLD_NUGGET)) {
            unitValue = NUGGET_UNITS_PER_NUGGET;
        } else if (stack.is(Items.GOLD_INGOT)) {
            unitValue = NUGGET_UNITS_PER_INGOT;
        } else if (stack.is(Items.GOLD_BLOCK)) {
            unitValue = NUGGET_UNITS_PER_BLOCK;
        } else {
            throw new IllegalArgumentException("item is not monetary gold");
        }

        return Math.multiplyExact(unitValue, stack.getCount());
    }

    public static long moneyAmount(long nuggetUnits, long nuggetValue) {
        if (nuggetUnits < 0L) {
            throw new IllegalArgumentException("nuggetUnits cannot be negative");
        }
        if (nuggetValue <= 0L) {
            throw new IllegalArgumentException("nuggetValue must be positive");
        }
        return Math.multiplyExact(nuggetUnits, nuggetValue);
    }
}

