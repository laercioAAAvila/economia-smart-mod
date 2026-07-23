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
        shopMenu.setBuyReferenceFromClient(serverPlayer, payload.stack());
    }
}
