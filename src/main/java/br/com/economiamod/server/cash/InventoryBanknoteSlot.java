package br.com.economiamod.server.cash;

import net.minecraft.world.item.ItemStack;

public record InventoryBanknoteSlot(int slot, long value, ItemStack stack) {
}

