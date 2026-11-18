package br.com.economiamod.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Only loaded after Curios is confirmed installed (see {@code GpsPresence}). */
public final class CuriosGpsBridge {
    private CuriosGpsBridge() {
    }

    /** True when {@code gps} is in a Curios slot that is currently active (a disabled slot does not count). */
    public static boolean hasGps(LivingEntity entity, Item gps) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.findCurios(stack -> stack.is(gps)).stream()
                        .anyMatch(result -> handler.isSlotActive(result.slotContext().identifier(),
                                result.slotContext().index())))
                .orElse(false);
    }

    /** Puts {@code stack} in the first "gps" slot, as a player would; false when the entity has no such slot. */
    public static boolean placeInGpsSlot(LivingEntity entity, ItemStack stack) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.getStacksHandler("gps"))
                .map(slots -> {
                    slots.getStacks().setStackInSlot(0, stack);
                    return true;
                })
                .orElse(false);
    }
}
