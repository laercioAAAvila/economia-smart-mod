package br.com.economiamod.server.claim;

import java.util.UUID;

public record PrivatePropertyMemberView(UUID playerUuid, String playerName, int permissionMask) {
}
