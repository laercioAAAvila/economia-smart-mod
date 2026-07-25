package br.com.economiamod.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundMailStateHandler {
    private ClientboundMailStateHandler() {
    }

    public static void handle(MailStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }
            try {
                Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientMailStateHandler");
                handler.getMethod("handle", MailStatePayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to dispatch mail state payload", exception);
            }
        });
    }
}
