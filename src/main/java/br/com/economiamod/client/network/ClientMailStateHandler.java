package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.MailStateAwareScreen;
import br.com.economiamod.common.network.MailStatePayload;
import net.minecraft.client.Minecraft;

public final class ClientMailStateHandler {
    private ClientMailStateHandler() {
    }

    public static void handle(MailStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof MailStateAwareScreen screen) {
            screen.applyMailState(payload);
        }
    }
}
