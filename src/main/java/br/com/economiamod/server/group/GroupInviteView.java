package br.com.economiamod.server.group;

import java.util.UUID;

public record GroupInviteView(UUID inviteId, UUID groupId, String groupName) {
}
