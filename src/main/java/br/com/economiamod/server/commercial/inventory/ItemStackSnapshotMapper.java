package br.com.economiamod.server.commercial.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class ItemStackSnapshotMapper {
    public CommercialItemSnapshot fromStack(ItemStack stack) {
        return fromStack(stack, null);
    }

    public CommercialItemSnapshot fromStack(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return CommercialItemSnapshot.empty();
        }

        String components = registries == null ? null : stack.saveOptional(registries).toString();
        return new CommercialItemSnapshot(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(),
                components,
                null
        );
    }
}
