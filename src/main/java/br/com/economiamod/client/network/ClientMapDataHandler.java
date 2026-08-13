package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.EconomyMapScreen;
import br.com.economiamod.common.network.MapDataPayload;
import net.minecraft.client.Minecraft;

public final class ClientMapDataHandler {
    private ClientMapDataHandler() {
    }

    public static void handle(MapDataPayload payload) {
        if (Minecraft.getInstance().screen instanceof EconomyMapScreen screen) {
            screen.applyMapData(payload);
        }
    }

    public static MapDataPayload empty() {
        return new MapDataPayload(java.util.List.of(), java.util.List.of(), false, false,
                br.com.economiamod.common.group.ChatChannel.GENERAL);
    }
}
