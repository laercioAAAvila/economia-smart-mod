package br.com.economiamod.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundMapDataHandler {
    private ClientboundMapDataHandler() {
    }

    public static void handle(MapDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }
            try {
                Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientMapDataHandler");
                handler.getMethod("handle", MapDataPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to dispatch map data payload", exception);
            }
        });
    }
}
