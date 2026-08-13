package br.com.economiamod.server.claim;

import br.com.economiamod.common.group.GroupType;
import java.util.UUID;
import java.util.List;

public record ClaimAnchorMenuState(
        UUID anchorId,
        UUID territoryId,
        GroupType groupType,
        String dimension,
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
        long suggestedSalePrice,
        int chunkCount,
        int chunkLimit,
        long nextChunkPrice,
        boolean canBuyChunk,
        long currentTax,
        long totalTax,
        int taxCount,
        int taxPeriodDays,
        List<PrivatePropertyMemberView> privateMembers
) {
}
