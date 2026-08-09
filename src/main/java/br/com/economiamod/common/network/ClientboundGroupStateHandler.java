package br.com.economiamod.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundGroupStateHandler {
    private ClientboundGroupStateHandler() {
    }

    public static void handle(GroupStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }
            try {
                Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientGroupStateHandler");
                handler.getMethod("handle", GroupStatePayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to dispatch group state", exception);
            }
        });
    }
}
