package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.EconomyMapScreen;
import br.com.economiamod.common.network.MapDataPayload;
import net.minecraft.client.Minecraft;

public final class ClientMapDataHandler {
    private static MapDataPayload latest = new MapDataPayload(java.util.List.of(), java.util.List.of(), false, false,
            br.com.economiamod.common.group.ChatChannel.GENERAL);

    private ClientMapDataHandler() {
    }

    public static void handle(MapDataPayload payload) {
        latest = payload;
        if (Minecraft.getInstance().screen instanceof EconomyMapScreen screen) {
            screen.applyMapData(payload);
        }
    }

    public static MapDataPayload latest() {
        return latest;
    }
}
