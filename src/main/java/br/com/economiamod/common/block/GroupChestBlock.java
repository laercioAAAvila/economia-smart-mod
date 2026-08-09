package br.com.economiamod.common.block;

import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GroupChestBlock extends ChestBlock {
    private final GroupType groupType;

    public GroupChestBlock(Properties properties, GroupType groupType) {
        super(properties, () -> ModBlockEntities.GROUP_CHEST.get());
        this.groupType = groupType;
    }

    public GroupType groupType() {
        return groupType;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GroupChestBlockEntity(pos, state);
    }
}
