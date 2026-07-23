package br.com.economiamod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    private static final String CATEGORY = "key.categories.economia";

    public static final KeyMapping SHOP_REFERENCE_FROM_JEI = new KeyMapping(
            "key.economia.shop_reference_from_jei",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SHOP_REFERENCE_FROM_JEI);
    }
}
