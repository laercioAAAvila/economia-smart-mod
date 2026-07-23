package br.com.economiamod.client.screen;

import br.com.economiamod.common.menu.BuyShopMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BuyShopScreen extends PlayerShopScreen<BuyShopMenu> {
    public BuyShopScreen(BuyShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
