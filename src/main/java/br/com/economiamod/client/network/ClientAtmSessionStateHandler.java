package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.AtmScreen;
import br.com.economiamod.common.network.AtmSessionStatePayload;
import net.minecraft.client.Minecraft;

public final class ClientAtmSessionStateHandler {
    private ClientAtmSessionStateHandler() {
    }

    public static void handle(AtmSessionStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AtmScreen atmScreen) {
            atmScreen.applySessionState(payload.loggedIn(), payload.username(), payload.accountNumber(), payload.showUsername());
        }
    }
}
