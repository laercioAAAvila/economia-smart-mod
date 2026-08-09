package br.com.economiamod.common.claim;

import br.com.economiamod.common.group.GroupType;
import java.util.UUID;

public record ClaimRecord(
        UUID id,
        UUID territoryId,
        UUID groupId,
        UUID ownerPlayerUuid,
        GroupType groupType,
        String dimension,
        int chunkX,
        int chunkZ
) {
}
