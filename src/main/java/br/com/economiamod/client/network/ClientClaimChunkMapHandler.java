package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.EconomyMapScreen;
import br.com.economiamod.common.network.OpenClaimChunkMapPayload;
import net.minecraft.client.Minecraft;

public final class ClientClaimChunkMapHandler {
    private ClientClaimChunkMapHandler() {
    }

    public static void handle(OpenClaimChunkMapPayload payload) {
        Minecraft.getInstance().setScreen(new EconomyMapScreen(payload));
    }
}

