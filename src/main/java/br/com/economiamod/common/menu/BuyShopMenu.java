package br.com.economiamod.common.menu;

import br.com.economiamod.registry.ModMenus;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

public final class BuyShopMenu extends PlayerShopMenu {
    public BuyShopMenu(int containerId, Inventory inventory) {
        super(ModMenus.BUY_SHOP.get(), containerId, inventory, ShopKind.BUY);
    }

    public BuyShopMenu(int containerId, Inventory inventory, UUID commercialBlockId, UUID ownerPlayerUuid, BlockPos accessPos, Block expectedBlock, boolean owner) {
        super(ModMenus.BUY_SHOP.get(), containerId, inventory, ShopKind.BUY, commercialBlockId, ownerPlayerUuid, accessPos, expectedBlock, owner);
    }

    public BuyShopMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        super(ModMenus.BUY_SHOP.get(), containerId, inventory, ShopKind.BUY, data);
    }
}
