package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.AccountSummaryAwareScreen;
import br.com.economiamod.common.network.AtmAccountSummaryPayload;
import net.minecraft.client.Minecraft;

public final class ClientAtmAccountSummaryHandler {
    private ClientAtmAccountSummaryHandler() {
    }

    public static void handle(AtmAccountSummaryPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AccountSummaryAwareScreen screen) {
            screen.applyAccountSummary(payload);
        }
    }
}
