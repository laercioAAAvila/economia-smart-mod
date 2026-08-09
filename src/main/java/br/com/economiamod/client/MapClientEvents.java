package br.com.economiamod.client;

import br.com.economiamod.client.screen.EconomyMapScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class MapClientEvents {
    private MapClientEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen == null && ModKeyMappings.OPEN_MAP.consumeClick()) {
            minecraft.setScreen(new EconomyMapScreen());
        }
    }
}
