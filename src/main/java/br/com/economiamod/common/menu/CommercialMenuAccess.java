package br.com.economiamod.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

public final class CommercialMenuAccess {
    private static final double MAX_DISTANCE_SQ = 64.0D;

    private CommercialMenuAccess() {
    }

    public static boolean stillValid(Player player, BlockPos accessPos, Block expectedBlock) {
        if (accessPos == null || expectedBlock == null) {
            return true;
        }
        if (!player.level().getBlockState(accessPos).is(expectedBlock)) {
            return false;
        }
        return player.distanceToSqr(
                accessPos.getX() + 0.5D,
                accessPos.getY() + 0.5D,
                accessPos.getZ() + 0.5D
        ) <= MAX_DISTANCE_SQ;
    }
}
