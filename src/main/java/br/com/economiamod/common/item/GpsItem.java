package br.com.economiamod.common.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * GPS: in the Curios "GPS" slot (or in a hand) it shows the Economia map as a minimap; using it opens the full map.
 */
public final class GpsItem extends Item {
    public GpsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            openMapOnClient();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.economia.gps.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.economia.gps.tooltip_zoom",
                Component.keybind("key.economia.minimap_zoom_in"),
                Component.keybind("key.economia.minimap_zoom_out")).withStyle(ChatFormatting.GRAY));
    }

    /** Client classes are reached by name, so a dedicated server never loads them. */
    private static void openMapOnClient() {
        try {
            Class.forName("br.com.economiamod.client.GpsClient").getMethod("openMap").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to open the Economia map", exception);
        }
    }
}
