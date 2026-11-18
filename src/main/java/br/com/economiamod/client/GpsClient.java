package br.com.economiamod.client;

import br.com.economiamod.client.screen.EconomyMapScreen;
import net.minecraft.client.Minecraft;

/** Client side of the GPS item, reached by name from {@code GpsItem}. */
public final class GpsClient {
    private GpsClient() {
    }

    public static void openMap() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen == null) {
            minecraft.setScreen(new EconomyMapScreen());
        }
    }
}
