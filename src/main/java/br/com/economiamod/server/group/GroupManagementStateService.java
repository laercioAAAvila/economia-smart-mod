package br.com.economiamod.server.group;

import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupSummary;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.network.GroupStatePayload;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class GroupManagementStateService {
    private final GroupRepository repository = new GroupRepository();
    private final ClaimUpgradePricingService upgrades = new ClaimUpgradePricingService();

    public GroupStatePayload state(ServerPlayer player, GroupType type, boolean authenticated) throws SQLException {
        List<GroupStatePayload.InviteSummary> invites = new ArrayList<>();
        if (authenticated) {
            for (GroupInviteView invite : repository.pendingInvites(player.getUUID(), type)) {
                invites.add(new GroupStatePayload.InviteSummary(invite.inviteId(), invite.groupId(), invite.groupName()));
            }
        }
        GroupMembership membership = authenticated ? repository.membership(player.getUUID(), type).orElse(null) : null;
        if (membership == null) {
            return new GroupStatePayload(authenticated, type, false, null, "", GroupRole.MEMBER,
                    0L, 0L, 0, 0, 0, 0L, true, true,
                    false, false, List.of(), invites);
        }
        GroupSummary group = repository.group(membership.groupId()).orElse(null);
        if (group == null) {
            return new GroupStatePayload(authenticated, type, false, null, "", GroupRole.MEMBER,
                    0L, 0L, 0, 0, 0, 0L, true, true,
                    false, false, List.of(), invites);
        }
        List<GroupStatePayload.MemberSummary> members = new ArrayList<>();
        for (GroupMemberView member : repository.members(group.id())) {
            members.add(new GroupStatePayload.MemberSummary(member.playerUuid(), displayName(player, member.playerUuid()),
                    member.role(), member.permissionMask(), member.lastActiveMillis()));
        }
        ClaimUpgradeQuote quote = upgrades.quote(group.claimLimit());
        return new GroupStatePayload(true, type, true, group.id(), group.name(), membership.role(),
                balance(group.accountId()), balance(group.supportAccountId()), quote.currentLimit(),
                quote.maxLimit(), quote.percentageBasisPoints(), quote.price(), quote.maximumReached(),
                quote.configurationValid(),
                group.visitorUseBuyShop(), group.visitorUseSellShop(), members, invites);
    }

    private long balance(UUID accountId) throws SQLException {
        if (accountId == null) {
            return 0L;
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT balance FROM economy_accounts WHERE id = ? AND status = 'ACTIVE'")) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private String displayName(ServerPlayer viewer, UUID playerUuid) {
        if (viewer.getServer() != null) {
            ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(playerUuid);
            if (online != null) {
                return online.getGameProfile().getName();
            }
        }
        return playerUuid.toString().substring(0, 8);
    }
}
