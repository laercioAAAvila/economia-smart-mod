package br.com.economiamod.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundAtmOperationHistoryHandler {
    private ClientboundAtmOperationHistoryHandler() {
    }

    public static void handle(AtmOperationHistoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }
            try {
                Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientAtmOperationHistoryHandler");
                handler.getMethod("handle", AtmOperationHistoryPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to dispatch ATM operation history payload", exception);
            }
        });
    }
}
