package br.com.economiamod.server.player;

import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemStackFactory {
    public ItemStack create(CommercialItemSnapshot snapshot, int count, HolderLookup.Provider registries) {
        ItemStack parsed = parseStoredStack(snapshot, registries);
        if (!parsed.isEmpty()) {
            parsed.setCount(count);
            return parsed;
        }

        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(snapshot.itemId()));
        return new ItemStack(item, count);
    }

    private ItemStack parseStoredStack(CommercialItemSnapshot snapshot, HolderLookup.Provider registries) {
        if (registries == null || snapshot.components() == null || snapshot.components().isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            CompoundTag tag = TagParser.parseTag(snapshot.components());
            return ItemStack.parseOptional(registries, tag);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }
}
