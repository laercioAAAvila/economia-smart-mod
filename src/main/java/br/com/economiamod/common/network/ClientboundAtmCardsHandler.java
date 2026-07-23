package br.com.economiamod.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundAtmCardsHandler {
    private ClientboundAtmCardsHandler() {
    }

    public static void handle(AtmCardsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }
            try {
                Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientAtmCardsHandler");
                handler.getMethod("handle", AtmCardsPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to dispatch ATM cards payload", exception);
            }
        });
    }
}
