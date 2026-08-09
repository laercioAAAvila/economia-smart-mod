package br.com.economiamod.server.claim;

import br.com.economiamod.common.group.GroupType;
import java.util.UUID;

public record ClaimInvoiceRecord(
        UUID id,
        UUID territoryId,
        String invoiceType,
        UUID debtorPlayerUuid,
        UUID sellerPlayerUuid,
        UUID sellerAccountId,
        UUID buyerGroupId,
        long amount,
        int minecraftDays,
        String status,
        GroupType claimType,
        UUID groupId,
        UUID ownerPlayerUuid,
        long landDebt,
        long landPrice,
        long anchorPaidUntilMillis
) {
}
