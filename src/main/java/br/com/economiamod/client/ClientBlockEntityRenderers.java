package br.com.economiamod.client;

import br.com.economiamod.registry.ModBlockEntities;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class ClientBlockEntityRenderers {
    private ClientBlockEntityRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.GROUP_CHEST.get(), ChestRenderer::new);
    }
}
