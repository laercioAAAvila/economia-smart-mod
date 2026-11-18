package br.com.economiamod.common.gps;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/** Where a player carries the GPS: the Curios "gps" slot (when Curios is installed) or a hand. */
public final class GpsPresence {
    public static final String CURIOS_MOD_ID = "curios";
    /** Curios slot identifier, also the name of the item tag {@code curios:gps}. */
    public static final String CURIOS_SLOT = "gps";

    private static volatile Boolean curiosAvailable;

    private GpsPresence() {
    }

    public static boolean inHand(Player player) {
        return player.getMainHandItem().is(ModItems.GPS.get()) || player.getOffhandItem().is(ModItems.GPS.get());
    }

    /** True when the GPS sits in an active Curios slot. Always false without Curios. */
    public static boolean inCuriosSlot(LivingEntity entity) {
        if (!curiosAvailable()) {
            return false;
        }
        try {
            return br.com.economiamod.compat.curios.CuriosGpsBridge.hasGps(entity, ModItems.GPS.get());
        } catch (LinkageError error) {
            // A Curios build with a different API: keep the GPS working in the hand instead of crashing.
            curiosAvailable = false;
            EconomiaMod.LOGGER.warn("Curios API incompatível; o GPS só funcionará na mão.", error);
            return false;
        }
    }

    public static boolean curiosAvailable() {
        Boolean available = curiosAvailable;
        if (available == null) {
            ModList mods = ModList.get();
            available = mods != null && mods.isLoaded(CURIOS_MOD_ID);
            curiosAvailable = available;
        }
        return available;
    }
}
