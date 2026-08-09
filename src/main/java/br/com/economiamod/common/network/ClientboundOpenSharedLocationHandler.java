package br.com.economiamod.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundOpenSharedLocationHandler {
    private ClientboundOpenSharedLocationHandler() {}
    public static void handle(OpenSharedLocationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            try {
                Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientSharedLocationHandler");
                handler.getMethod("handle", OpenSharedLocationPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to open shared location", exception);
            }
        });
    }
}
