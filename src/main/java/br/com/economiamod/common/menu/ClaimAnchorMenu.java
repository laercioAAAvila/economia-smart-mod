package br.com.economiamod.common.menu;

import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.server.claim.ClaimAnchorMenuState;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class ClaimAnchorMenu extends AbstractContainerMenu {
    private final ClaimAnchorMenuState state;
    private final BlockPos accessPos;
    private final Block expectedBlock;

    public ClaimAnchorMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, readState(data));
    }

    public ClaimAnchorMenu(int containerId, Inventory inventory, ClaimAnchorMenuState state) {
        super(ModMenus.CLAIM_ANCHOR.get(), containerId);
        this.state = state;
        this.accessPos = new BlockPos(state.blockX(), state.blockY(), state.blockZ());
        this.expectedBlock = state.groupType() == GroupType.CLAN
                ? ModBlocks.CLAN_CLAIM_BLOCK.get() : ModBlocks.PRIVATE_PROPERTY_CLAIM_BLOCK.get();
    }

    public ClaimAnchorMenuState state() {
        return state;
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, ClaimAnchorMenuState state) {
        buffer.writeUUID(state.anchorId());
        buffer.writeBoolean(state.territoryId() != null);
        if (state.territoryId() != null) buffer.writeUUID(state.territoryId());
        buffer.writeVarInt(state.groupType().ordinal());
        buffer.writeBlockPos(new BlockPos(state.blockX(), state.blockY(), state.blockZ()));
        buffer.writeLong(state.landPrice());
        buffer.writeLong(state.landDebt());
        buffer.writeVarInt(state.territoryCount());
        buffer.writeVarInt(state.territoryLimit());
        buffer.writeBoolean(state.active());
        buffer.writeBoolean(state.canManage());
        buffer.writeBoolean(state.canClaim());
        buffer.writeLong(state.anchorPrice());
        buffer.writeVarInt(state.anchorDaysRemaining());
        buffer.writeVarInt(state.defaultAnchorDays());
        buffer.writeVarInt(state.maxAnchorDays());
        buffer.writeLong(state.suggestedSalePrice());
    }

    private static ClaimAnchorMenuState readState(FriendlyByteBuf buffer) {
        UUID anchorId = buffer.readUUID();
        UUID territoryId = buffer.readBoolean() ? buffer.readUUID() : null;
        GroupType type = GroupType.values()[buffer.readVarInt()];
        BlockPos pos = buffer.readBlockPos();
        return new ClaimAnchorMenuState(anchorId, territoryId, type, pos.getX(), pos.getY(), pos.getZ(),
                buffer.readLong(), buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readLong(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readLong());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return CommercialMenuAccess.stillValid(player, accessPos, expectedBlock);
    }
}
