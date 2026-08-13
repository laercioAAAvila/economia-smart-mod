package br.com.economiamod.server.event;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.server.claim.ClaimOperationResult;
import br.com.economiamod.server.claim.ClaimService;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import java.sql.SQLException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class ClaimBlockEvents {
    private static final ClaimService SERVICE = new ClaimService();

    private ClaimBlockEvents() {
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        GroupType type = anchorType(event.getPlacedBlock());
        if (type == null) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || !EconomyDatabaseState.isAvailable()) {
            event.setCanceled(true);
            return;
        }
        try {
            ClaimOperationResult result = SERVICE.placeAnchor(
                    player.getUUID(), type, level.dimension().location().toString(),
                    event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
            if (!result.success()) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("claim.economia.error." + result.code()), true);
            }
        } catch (SQLException exception) {
            event.setCanceled(true);
            EconomiaMod.LOGGER.warn("Falha ao registrar bloco de claim.", exception);
            player.displayClientMessage(Component.translatable("claim.economia.database_unavailable"), true);
        }
    }

    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (anchorType(event.getState()) == null) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || !EconomyDatabaseState.isAvailable()) {
            event.setCanceled(true);
            return;
        }
        try {
            boolean adminOverride = event.getPlayer().createCommandSourceStack().hasPermission(2);
            ClaimOperationResult result = SERVICE.removeAnchor(
                    event.getPlayer().getUUID(), level.dimension().location().toString(),
                    event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), adminOverride);
            if (!result.success()) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(Component.translatable("claim.economia.error." + result.code()), true);
            }
            if (adminOverride && result.success()) {
                event.getPlayer().displayClientMessage(
                        Component.translatable("claim.economia.admin_territory_removed"), true);
            }
        } catch (SQLException exception) {
            event.setCanceled(true);
            EconomiaMod.LOGGER.warn("Falha ao remover bloco de claim.", exception);
            event.getPlayer().displayClientMessage(Component.translatable("claim.economia.database_unavailable"), true);
        }
    }

    private static GroupType anchorType(BlockState state) {
        if (state.is(ModBlocks.CLAN_CLAIM_BLOCK.get())) {
            return GroupType.CLAN;
        }
        if (state.is(ModBlocks.PRIVATE_PROPERTY_CLAIM_BLOCK.get())) {
            return GroupType.PRIVATE_PROPERTY;
        }
        return null;
    }
}
