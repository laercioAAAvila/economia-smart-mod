package br.com.economiamod.client.screen;

import net.minecraft.client.gui.GuiGraphics;

public final class InventorySlotRenderer {
    private InventorySlotRenderer() {
    }

    public static void draw(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF111416);
        graphics.fill(x, y, x + 16, y + 16, 0xFF6D767B);
        graphics.fill(x + 1, y + 1, x + 16, y + 16, 0xFF343C40);
    }
}
