package br.com.economiamod.server.network;

import br.com.economiamod.common.menu.PlayerShopMenu;
import br.com.economiamod.common.network.ShopReferencePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ShopReferencePayloadHandler {
    private ShopReferencePayloadHandler() {
    }

    public static void handle(ShopReferencePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.containerMenu instanceof PlayerShopMenu shopMenu)) {
            return;
        }
        // The server closes a menu whose block is gone or too far only on its tick; a packet arriving before that
        // must not act on a shop/mailbox that was just broken or left behind.
        if (!shopMenu.stillValid(serverPlayer)) {
            serverPlayer.closeContainer();
            return;
        }
        shopMenu.setBuyReferenceFromClient(serverPlayer, payload.stack());
    }
}
