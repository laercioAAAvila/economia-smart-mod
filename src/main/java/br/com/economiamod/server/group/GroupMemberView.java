package br.com.economiamod.server.group;

import br.com.economiamod.common.group.GroupRole;
import java.util.UUID;

public record GroupMemberView(UUID playerUuid, GroupRole role, int permissionMask, long lastActiveMillis) {
}
