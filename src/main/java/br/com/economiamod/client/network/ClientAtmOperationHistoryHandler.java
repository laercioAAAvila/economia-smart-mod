package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.AtmOperationHistoryAwareScreen;
import br.com.economiamod.common.network.AtmOperationHistoryPayload;
import net.minecraft.client.Minecraft;

public final class ClientAtmOperationHistoryHandler {
    private ClientAtmOperationHistoryHandler() {
    }

    public static void handle(AtmOperationHistoryPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AtmOperationHistoryAwareScreen screen) {
            screen.applyOperationHistory(payload);
        }
    }
}
