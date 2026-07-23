package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.AtmCardsAwareScreen;
import br.com.economiamod.common.network.AtmCardsPayload;
import net.minecraft.client.Minecraft;

public final class ClientAtmCardsHandler {
    private ClientAtmCardsHandler() {
    }

    public static void handle(AtmCardsPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AtmCardsAwareScreen screen) {
            screen.applyAtmCards(payload);
        }
    }
}
