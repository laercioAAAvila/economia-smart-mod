package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.GroupManagementScreen;
import br.com.economiamod.common.network.GroupStatePayload;
import net.minecraft.client.Minecraft;

public final class ClientGroupStateHandler {
    private ClientGroupStateHandler() {
    }

    public static void handle(GroupStatePayload payload) {
        if (Minecraft.getInstance().screen instanceof GroupManagementScreen screen) {
            screen.applyState(payload);
        }
    }
}
