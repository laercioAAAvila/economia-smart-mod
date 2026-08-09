package br.com.economiamod.common.block;

import br.com.economiamod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GroupChestBlockEntity extends ChestBlockEntity {
    public GroupChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROUP_CHEST.get(), pos, state);
    }
}
