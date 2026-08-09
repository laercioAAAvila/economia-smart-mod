package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.EconomyMapScreen;
import br.com.economiamod.common.network.OpenSharedLocationPayload;
import net.minecraft.client.Minecraft;

public final class ClientSharedLocationHandler {
    private ClientSharedLocationHandler() {}
    public static void handle(OpenSharedLocationPayload payload) {
        Minecraft.getInstance().setScreen(new EconomyMapScreen(payload));
    }
}
