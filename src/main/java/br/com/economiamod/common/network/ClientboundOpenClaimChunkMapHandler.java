package br.com.economiamod.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundOpenClaimChunkMapHandler {
    private ClientboundOpenClaimChunkMapHandler() {
    }

    public static void handle(OpenClaimChunkMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }
            try {
                Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientClaimChunkMapHandler");
                handler.getMethod("handle", OpenClaimChunkMapPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to open claim chunk map", exception);
            }
        });
    }
}

