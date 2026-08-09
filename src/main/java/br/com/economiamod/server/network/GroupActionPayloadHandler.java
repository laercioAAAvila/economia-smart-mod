package br.com.economiamod.server.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.menu.GroupManagementMenu;
import br.com.economiamod.common.network.GroupActionPayload;
import br.com.economiamod.server.group.GroupBankingService;
import br.com.economiamod.server.group.GroupManagementStateService;
import br.com.economiamod.server.group.GroupOperationResult;
import br.com.economiamod.server.group.GroupRepository;
import br.com.economiamod.server.group.GroupService;
import br.com.economiamod.server.group.ClaimLimitUpgradeService;
import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.server.group.ServerActiveClockService;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GroupActionPayloadHandler {
    private static final GroupService GROUPS = new GroupService();
    private static final GroupRepository REPOSITORY = new GroupRepository();
    private static final GroupBankingService BANKING = new GroupBankingService();
    private static final GroupManagementStateService STATE = new GroupManagementStateService();
    private static final ClaimLimitUpgradeService UPGRADES = new ClaimLimitUpgradeService();

    private GroupActionPayloadHandler() {
    }

    public static void handle(GroupActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof GroupManagementMenu menu)) {
            return;
        }
        context.enqueueWork(() -> process(player, menu, payload));
    }

    private static void process(ServerPlayer player, GroupManagementMenu menu, GroupActionPayload payload) {
        try {
            if (payload.action() == br.com.economiamod.common.network.GroupAction.AUTHENTICATE) {
                if (!menu.authenticate(player)) {
                    player.displayClientMessage(Component.translatable("group.economia.invalid_card"), true);
                }
                sync(player, menu);
                return;
            }
            if (!menu.authenticated()) {
                return;
            }
            GroupOperationResult result = null;
            GroupMembership membership = REPOSITORY.membership(player.getUUID(), menu.groupType()).orElse(null);
            switch (payload.action()) {
                case REFRESH -> {
                }
                case CREATE -> result = GROUPS.create(player.getUUID(), menu.groupType(), payload.text(),
                        ServerActiveClockService.INSTANCE.currentMillis());
                case INVITE -> result = GROUPS.invite(player, payload.text(), menu.groupType());
                case ACCEPT_INVITE -> result = GROUPS.respondToInvite(player.getUUID(), payload.targetId(), true,
                        ServerActiveClockService.INSTANCE.currentMillis());
                case DECLINE_INVITE -> result = GROUPS.respondToInvite(player.getUUID(), payload.targetId(), false,
                        ServerActiveClockService.INSTANCE.currentMillis());
                case REMOVE_MEMBER -> result = GROUPS.removeMember(player.getUUID(), payload.targetId(), menu.groupType());
                case SET_PERMISSIONS -> result = GROUPS.updatePermissions(player.getUUID(), payload.targetId(), payload.intValue());
                case SET_VICE_LEADER -> result = GROUPS.appointViceLeader(player.getUUID(), payload.targetId());
                case LEAVE -> result = GROUPS.leave(player.getUUID(), menu.groupType());
                case RENAME -> result = GROUPS.rename(player.getUUID(), menu.groupType(), payload.text());
                case CLOSE -> result = GROUPS.close(player.getUUID(), membership == null ? null : membership.groupId(),
                        menu.groupType(), false);
                case SET_VISITOR_SHOPS -> result = GROUPS.updateVisitorShopPermissions(
                        player.getUUID(), menu.groupType(), payload.firstFlag(), payload.secondFlag());
                case DEPOSIT -> {
                    if (membership != null) {
                        BANKING.deposit(player.getUUID(), membership.groupId(), payload.firstFlag(), payload.amount(),
                                "group-deposit:" + payload.requestId());
                    }
                }
                case WITHDRAW -> {
                    if (membership != null) {
                        BANKING.withdraw(player.getUUID(), membership.groupId(), payload.firstFlag(), payload.amount(),
                                "group-withdraw:" + payload.requestId());
                    }
                }
                case SET_UPGRADE_PAYMENT -> menu.setUpgradePayment(player, true, payload.firstFlag());
                case CLOSE_UPGRADE_PAYMENT -> menu.setUpgradePayment(player, false, false);
                case BUY_UPGRADE -> {
                    if (membership != null && menu.upgradePaymentOpen()) {
                        result = UPGRADES.purchase(player, membership.groupId(),
                                DirectPaymentMethod.parse(payload.text()), menu.paymentCard(), menu.paymentCash(),
                                payload.intValue(), payload.amount(), payload.requestId());
                        if (result.success()) {
                            menu.setUpgradePayment(player, false, false);
                            player.displayClientMessage(Component.translatable("group.economia.upgrade_purchased"), true);
                        }
                    }
                }
                case AUTHENTICATE -> {
                }
            }
            if (result != null && !result.success()) {
                player.displayClientMessage(Component.translatable("group.economia.error." + result.code()), true);
            }
            sync(player, menu);
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao processar ação de grupo.", exception);
            player.displayClientMessage(Component.translatable("commands.economia.unavailable"), true);
        }
    }

    private static void sync(ServerPlayer player, GroupManagementMenu menu) throws SQLException {
        PacketDistributor.sendToPlayer(player, STATE.state(player, menu.groupType(), menu.authenticated()));
    }
}
