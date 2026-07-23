package br.com.economiamod.server.network;

import br.com.economiamod.common.menu.BankCounterMenu;
import br.com.economiamod.common.network.BankCounterActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BankCounterActionPayloadHandler {
    private BankCounterActionPayloadHandler() {
    }

    public static void handle(BankCounterActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu instanceof BankCounterMenu menu) {
            if (payload.action() == BankCounterActionPayload.ACTION_MINT) {
                menu.mintGold(serverPlayer, payload.requestId());
            } else if (payload.action() == BankCounterActionPayload.ACTION_REDEEM) {
                menu.redeemGold(serverPlayer, payload.amount(), payload.unit(), payload.requestId());
            } else if (payload.action() == BankCounterActionPayload.ACTION_REFRESH_PRICES) {
                menu.refreshGoldPricing(serverPlayer);
            }
        }
    }
}
