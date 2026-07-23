package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientboundAtmSessionStateHandler {
    private ClientboundAtmSessionStateHandler() {
    }

    public static void handle(AtmSessionStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        try {
            Class<?> handler = Class.forName("br.com.economiamod.client.network.ClientAtmSessionStateHandler");
            handler.getMethod("handle", AtmSessionStatePayload.class).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            EconomiaMod.LOGGER.warn("Falha ao aplicar estado de sessao do ATM no cliente.", exception);
        }
    }
}
