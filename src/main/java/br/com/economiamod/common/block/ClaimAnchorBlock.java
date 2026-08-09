package br.com.economiamod.common.block;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.server.claim.ClaimAnchorRecord;
import br.com.economiamod.server.claim.ClaimRepository;
import br.com.economiamod.server.claim.ClaimAnchorMenuState;
import br.com.economiamod.server.claim.ClaimAnchorMenuStateService;
import br.com.economiamod.common.menu.ClaimAnchorMenu;
import java.sql.SQLException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.SimpleMenuProvider;

public final class ClaimAnchorBlock extends Block {
    private static final ClaimRepository REPOSITORY = new ClaimRepository();
    private static final ClaimAnchorMenuStateService MENU_STATE = new ClaimAnchorMenuStateService();

    public ClaimAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        try {
            ClaimAnchorRecord anchor = REPOSITORY.anchorAt(
                    serverLevel.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ()).orElse(null);
            if (anchor == null) {
                serverPlayer.displayClientMessage(Component.translatable("claim.economia.anchor_missing"), true);
                return InteractionResult.CONSUME;
            }
            ClaimAnchorMenuState menuState = MENU_STATE.state(serverPlayer.getUUID(), anchor.id());
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new ClaimAnchorMenu(containerId, inventory, menuState),
                    Component.translatable(anchor.groupType() == br.com.economiamod.common.group.GroupType.CLAN
                            ? "screen.economia.claim.clan_title" : "screen.economia.claim.private_title")
            ), data -> ClaimAnchorMenu.writeOpeningData(data, menuState));
            return InteractionResult.CONSUME;
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao ativar bloco de claim.", exception);
            serverPlayer.displayClientMessage(Component.translatable("claim.economia.database_unavailable"), true);
            return InteractionResult.CONSUME;
        }
    }
}
