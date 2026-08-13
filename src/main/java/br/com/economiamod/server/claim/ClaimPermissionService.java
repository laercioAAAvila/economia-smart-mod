package br.com.economiamod.server.claim;

import br.com.economiamod.common.claim.ClaimRecord;
import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.group.TerritoryPermission;
import br.com.economiamod.server.group.GroupRepository;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import br.com.economiamod.server.persistence.EconomyDatabase;

public final class ClaimPermissionService {
    private final ClaimRepository claimRepository = new ClaimRepository();
    private final GroupRepository groupRepository = new GroupRepository();

    public Optional<ClaimRecord> claimAt(ServerLevel level, BlockPos pos) throws SQLException {
        return claimRepository.claimAt(level.dimension().location().toString(), pos.getX() >> 4, pos.getZ() >> 4);
    }

    public boolean can(UUID playerUuid, ClaimRecord claim, TerritoryPermission permission) throws SQLException {
        if (claim.groupType() == GroupType.PRIVATE_PROPERTY) {
            if (playerUuid.equals(claim.ownerPlayerUuid())) {
                return true;
            }
            return privatePropertyMemberCan(playerUuid, claim.territoryId(), permission);
        }
        return groupRepository.membership(playerUuid, claim.groupId()).map(member -> member.has(permission)).orElse(false);
    }

    public boolean canOverrideOwnerProtection(UUID playerUuid, ServerLevel level, BlockPos pos) throws SQLException {
        ClaimRecord claim = claimAt(level, pos).orElse(null);
        if (claim == null) {
            return false;
        }
        if (claim.groupType() == GroupType.PRIVATE_PROPERTY) {
            return playerUuid.equals(claim.ownerPlayerUuid());
        }
        GroupMembership member = groupRepository.membership(playerUuid, claim.groupId()).orElse(null);
        if (member == null) {
            return false;
        }
        return member.role() == GroupRole.LEADER
                || (claim.groupType() == GroupType.CLAN && member.role() == GroupRole.VICE_LEADER);
    }

    private boolean privatePropertyMemberCan(UUID playerUuid, UUID territoryId,
                                             TerritoryPermission permission) throws SQLException {
        if (territoryId == null) {
            return false;
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT permission_mask FROM economy_private_property_members
                      WHERE territory_id = ? AND player_uuid = ? LIMIT 1
                     """)) {
            statement.setObject(1, territoryId);
            statement.setObject(2, playerUuid);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() && permission.presentIn(resultSet.getInt("permission_mask"));
            }
        }
    }
}
