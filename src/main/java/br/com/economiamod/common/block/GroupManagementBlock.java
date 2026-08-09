package br.com.economiamod.common.block;

import br.com.economiamod.common.group.GroupType;
import net.minecraft.world.level.block.Block;
import br.com.economiamod.common.menu.GroupManagementMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class GroupManagementBlock extends Block {
    private final GroupType groupType;

    public GroupManagementBlock(Properties properties, GroupType groupType) {
        super(properties);
        this.groupType = groupType;
    }

    public GroupType groupType() {
        return groupType;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new GroupManagementMenu(containerId, inventory, groupType, pos),
                    Component.translatable(groupType == GroupType.CLAN
                            ? "screen.economia.group.clan_title" : "screen.economia.group.private_property_title")
            ), data -> GroupManagementMenu.writeOpeningData(data, groupType, pos));
        }
        return InteractionResult.CONSUME;
    }
}
