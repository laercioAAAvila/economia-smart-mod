package br.com.economiamod.client.network;

import br.com.economiamod.client.screen.EconomyMapScreen;
import br.com.economiamod.common.network.MapDataPayload;
import net.minecraft.client.Minecraft;

public final class ClientMapDataHandler {
    private ClientMapDataHandler() {
    }

    /** Last claims and saved places from the server, also drawn by the GPS minimap. */
    private static MapDataPayload latest;

    public static void handle(MapDataPayload payload) {
        latest = payload;
        if (Minecraft.getInstance().screen instanceof EconomyMapScreen screen) {
            screen.applyMapData(payload);
        }
    }

    public static MapDataPayload latest() {
        return latest != null ? latest : empty();
    }

    /** Forget the last server's data when leaving it. */
    public static void clear() {
        latest = null;
    }

    public static MapDataPayload empty() {
        return new MapDataPayload(java.util.List.of(), java.util.List.of(), false, false,
                br.com.economiamod.common.group.ChatChannel.GENERAL);
    }
}
