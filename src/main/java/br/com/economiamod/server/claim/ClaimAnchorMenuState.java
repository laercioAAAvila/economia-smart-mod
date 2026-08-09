package br.com.economiamod.server.claim;

import br.com.economiamod.common.group.GroupType;
import java.util.UUID;

public record ClaimAnchorMenuState(
        UUID anchorId,
        UUID territoryId,
        GroupType groupType,
        int blockX,
        int blockY,
        int blockZ,
        long landPrice,
        long landDebt,
        int territoryCount,
        int territoryLimit,
        boolean active,
        boolean canManage,
        boolean canClaim,
        long anchorPrice,
        int anchorDaysRemaining,
        int defaultAnchorDays,
        int maxAnchorDays,
        long suggestedSalePrice
) {
}
