package br.com.economiamod.client.screen;

import br.com.economiamod.common.menu.SellShopMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SellShopScreen extends PlayerShopScreen<SellShopMenu> {
    public SellShopScreen(SellShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
