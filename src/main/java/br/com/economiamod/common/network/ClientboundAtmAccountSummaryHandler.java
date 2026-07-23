package br.com.economiamod.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundAtmAccountSummaryHandler {
    private ClientboundAtmAccountSummaryHandler() {
    }

    public static void handle(AtmAccountSummaryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }
            try {
                Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientAtmAccountSummaryHandler");
                handler.getMethod("handle", AtmAccountSummaryPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to dispatch ATM account summary payload", exception);
            }
        });
    }
}
