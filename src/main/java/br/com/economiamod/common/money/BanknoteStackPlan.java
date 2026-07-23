package br.com.economiamod.common.money;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record BanknoteStackPlan(long amount, List<ItemStack> stacks) {
}

