package br.com.economiamod.server.event;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.block.BlockProtectionPolicy;
import br.com.economiamod.common.block.CommercialBlockEntity;
import br.com.economiamod.common.claim.ClaimRecord;
import br.com.economiamod.common.group.TerritoryPermission;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.server.claim.ClaimPermissionService;
import br.com.economiamod.server.commercial.CommercialBlockProtectionPolicy;
import br.com.economiamod.server.commercial.CommercialOwnerRepository;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import java.sql.SQLException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

public final class ClaimProtectionEvents {
    private static final ClaimPermissionService PERMISSIONS = new ClaimPermissionService();
    private static final CommercialOwnerRepository OWNERS = new CommercialOwnerRepository();

    private ClaimProtectionEvents() {
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        authorizeTerritoryAction(level, event.getPos(), player, TerritoryPermission.PLACE, event::setCanceled);
    }

    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (CommercialBlockProtectionPolicy.of(event.getState()) != BlockProtectionPolicy.COMMON) {
            return;
        }
        authorizeTerritoryAction(level, event.getPos(), event.getPlayer(), TerritoryPermission.DESTROY, event::setCanceled);
    }

    public static void onBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        try {
            ClaimRecord claim = PERMISSIONS.claimAt(level, event.getPos()).orElse(null);
            if (claim == null) {
                return;
            }
            BlockState state = level.getBlockState(event.getPos());
            BlockProtectionPolicy policy = CommercialBlockProtectionPolicy.of(state);
            if (policy == BlockProtectionPolicy.SYSTEM_PROTECTED) {
                return;
            }
            if (policy == BlockProtectionPolicy.OWNER_PROTECTED) {
                if (isCommercialOwner(level, event.getPos(), player)
                        || PERMISSIONS.can(player.getUUID(), claim, TerritoryPermission.USE)
                        || (state.is(ModBlocks.BUY_SHOP.get()) && PERMISSIONS.visitorCanUseShop(player.getUUID(), claim, true))
                        || (state.is(ModBlocks.SELL_SHOP.get()) && PERMISSIONS.visitorCanUseShop(player.getUUID(), claim, false))) {
                    return;
                }
                denyInteraction(event, player);
                return;
            }
            if (!PERMISSIONS.can(player.getUUID(), claim, TerritoryPermission.USE)) {
                denyInteraction(event, player);
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao validar uso dentro de claim.", exception);
            denyInteraction(event, player);
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!EconomyDatabaseState.isAvailable()) {
            event.getAffectedBlocks().clear();
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> {
            try {
                BlockState state = level.getBlockState(pos);
                return state.is(ModBlocks.CLAN_CLAIM_BLOCK.get())
                        || state.is(ModBlocks.PRIVATE_PROPERTY_CLAIM_BLOCK.get())
                        || PERMISSIONS.claimAt(level, pos).isPresent();
            } catch (SQLException exception) {
                EconomiaMod.LOGGER.warn("Falha ao proteger claim contra explosão.", exception);
                return true;
            }
        });
    }

    public static void onPistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!EconomyDatabaseState.isAvailable()) {
            event.setCanceled(true);
            return;
        }
        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) {
            return;
        }
        try {
            if (protectedPosition(level, event.getPos()) || protectedPosition(level, event.getFaceOffsetPos())) {
                event.setCanceled(true);
                return;
            }
            for (net.minecraft.core.BlockPos pos : resolver.getToPush()) {
                if (protectedPosition(level, pos) || protectedPosition(level, pos.relative(event.getDirection()))) {
                    event.setCanceled(true);
                    return;
                }
            }
            for (net.minecraft.core.BlockPos pos : resolver.getToDestroy()) {
                if (protectedPosition(level, pos)) {
                    event.setCanceled(true);
                    return;
                }
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao proteger claim contra pistão.", exception);
            event.setCanceled(true);
        }
    }

    private static boolean protectedPosition(ServerLevel level, net.minecraft.core.BlockPos pos) throws SQLException {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.CLAN_CLAIM_BLOCK.get())
                || state.is(ModBlocks.PRIVATE_PROPERTY_CLAIM_BLOCK.get())
                || PERMISSIONS.claimAt(level, pos).isPresent();
    }

    private static void authorizeTerritoryAction(ServerLevel level, net.minecraft.core.BlockPos pos,
                                                  net.minecraft.world.entity.player.Player player,
                                                  TerritoryPermission permission,
                                                  java.util.function.Consumer<Boolean> cancel) {
        if (!EconomyDatabaseState.isAvailable()) {
            cancel.accept(true);
            player.displayClientMessage(Component.translatable("claim.economia.database_unavailable"), true);
            return;
        }
        try {
            ClaimRecord claim = PERMISSIONS.claimAt(level, pos).orElse(null);
            if (claim != null && !PERMISSIONS.can(player.getUUID(), claim, permission)) {
                cancel.accept(true);
                player.displayClientMessage(Component.translatable("claim.economia.permission_denied"), true);
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao validar permissão territorial.", exception);
            cancel.accept(true);
            player.displayClientMessage(Component.translatable("claim.economia.database_unavailable"), true);
        }
    }

    private static boolean isCommercialOwner(ServerLevel level, net.minecraft.core.BlockPos pos, ServerPlayer player) throws SQLException {
        if (!(level.getBlockEntity(pos) instanceof CommercialBlockEntity blockEntity)) {
            return false;
        }
        return OWNERS.owner(blockEntity.commercialBlockId()).filter(player.getUUID()::equals).isPresent();
    }

    private static void denyInteraction(PlayerInteractEvent.RightClickBlock event, ServerPlayer player) {
        event.setCanceled(true);
        player.displayClientMessage(Component.translatable("claim.economia.permission_denied"), true);
    }
}
