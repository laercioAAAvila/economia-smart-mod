package br.com.economiamod.common.group;

import java.util.UUID;

public record GroupMembership(
        UUID groupId,
        GroupType groupType,
        UUID playerUuid,
        GroupRole role,
        int permissionMask,
        long lastActiveMillis
) {
    public boolean has(TerritoryPermission permission) {
        return role == GroupRole.OWNER || role != GroupRole.MEMBER || permission.presentIn(permissionMask);
    }
}
