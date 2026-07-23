package br.com.economiamod.server.network;

import br.com.economiamod.common.menu.PlayerShopMenu;
import br.com.economiamod.common.network.ShopActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ShopActionPayloadHandler {
    private ShopActionPayloadHandler() {
    }

    public static void handle(ShopActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.containerMenu instanceof PlayerShopMenu shopMenu)) {
            return;
        }

        switch (payload.action()) {
            case SAVE_CONFIG -> shopMenu.saveOwnerConfig(serverPlayer, payload.price(), payload.quantity(), payload.active());
            case TRADE -> shopMenu.trade(serverPlayer, payload.requestId(), payload.quantity(), payload.active());
        }
    }
}
