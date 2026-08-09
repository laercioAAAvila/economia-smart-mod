package br.com.economiamod.server.claim;

import br.com.economiamod.common.group.GroupType;
import java.util.UUID;

public record ClaimAnchorRecord(
        UUID id,
        UUID territoryId,
        UUID groupId,
        UUID placedByPlayerUuid,
        GroupType groupType,
        String dimension,
        int blockX,
        int blockY,
        int blockZ,
        int chunkX,
        int chunkZ,
        boolean active,
        long landPrice,
        long landDebt,
        long anchorPaidUntilMillis
) {
}
