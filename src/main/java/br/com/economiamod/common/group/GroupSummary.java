package br.com.economiamod.common.group;

import java.util.UUID;

public record GroupSummary(
        UUID id,
        GroupType type,
        String name,
        UUID leaderPlayerUuid,
        UUID viceLeaderPlayerUuid,
        UUID accountId,
        UUID supportAccountId,
        int claimLimit,
        boolean visitorUseBuyShop,
        boolean visitorUseSellShop
) {
}
