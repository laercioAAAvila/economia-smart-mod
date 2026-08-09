package br.com.economiamod.server.event;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.claim.ClaimRecord;
import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.server.claim.ClaimPermissionService;
import br.com.economiamod.server.group.GroupRepository;
import java.sql.SQLException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class GroupProtectedBlockEvents {
    private static final ClaimPermissionService CLAIMS = new ClaimPermissionService();
    private static final GroupRepository GROUPS = new GroupRepository();

    private GroupProtectedBlockEvents() {
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        ProtectedGroupBlock block = protectedBlock(event.getState());
        if (block == null || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!allowed(level, event.getPos(), event.getPlayer(), block)) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("group.economia.protected_block_denied"), true);
        }
    }

    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ProtectedGroupBlock block = protectedBlock(level.getBlockState(event.getPos()));
        if (block != null && !allowed(level, event.getPos(), player, block)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("group.economia.protected_block_denied"), true);
        }
    }

    private static boolean allowed(ServerLevel level, net.minecraft.core.BlockPos pos,
                                   net.minecraft.world.entity.player.Player player, ProtectedGroupBlock block) {
        try {
            ClaimRecord claim = CLAIMS.claimAt(level, pos).orElse(null);
            if (claim == null) {
                return true;
            }
            if (claim.groupType() != block.groupType()) {
                return false;
            }
            if (block.groupType() == GroupType.PRIVATE_PROPERTY) {
                return CLAIMS.canOverrideOwnerProtection(player.getUUID(), level, pos);
            }
            GroupMembership membership = GROUPS.membership(player.getUUID(), claim.groupId()).orElse(null);
            if (membership == null) {
                return false;
            }
            return membership.role() == GroupRole.LEADER || membership.role() == GroupRole.OWNER
                    || (block.groupType() == GroupType.CLAN && membership.role() == GroupRole.VICE_LEADER);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao validar bloco protegido de grupo.", exception);
            return false;
        }
    }

    private static ProtectedGroupBlock protectedBlock(BlockState state) {
        if (state.is(ModBlocks.CLAN_CHEST.get()) || state.is(ModBlocks.CLAN_MANAGEMENT_BLOCK.get())) {
            return new ProtectedGroupBlock(GroupType.CLAN);
        }
        if (state.is(ModBlocks.PRIVATE_PROPERTY_CHEST.get()) || state.is(ModBlocks.PRIVATE_PROPERTY_MANAGEMENT_BLOCK.get())) {
            return new ProtectedGroupBlock(GroupType.PRIVATE_PROPERTY);
        }
        return null;
    }

    private record ProtectedGroupBlock(GroupType groupType) {
    }
}
