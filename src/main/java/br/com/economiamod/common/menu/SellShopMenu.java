package br.com.economiamod.common.menu;

import br.com.economiamod.registry.ModMenus;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

public final class SellShopMenu extends PlayerShopMenu {
    public SellShopMenu(int containerId, Inventory inventory) {
        super(ModMenus.SELL_SHOP.get(), containerId, inventory, ShopKind.SELL);
    }

    public SellShopMenu(int containerId, Inventory inventory, UUID commercialBlockId, UUID ownerPlayerUuid, BlockPos accessPos, Block expectedBlock, boolean owner) {
        super(ModMenus.SELL_SHOP.get(), containerId, inventory, ShopKind.SELL, commercialBlockId, ownerPlayerUuid, accessPos, expectedBlock, owner);
    }

    public SellShopMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        super(ModMenus.SELL_SHOP.get(), containerId, inventory, ShopKind.SELL, data);
    }
}
