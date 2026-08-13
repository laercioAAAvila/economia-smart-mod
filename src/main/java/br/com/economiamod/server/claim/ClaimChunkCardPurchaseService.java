package br.com.economiamod.server.claim;

import br.com.economiamod.server.transaction.MenuPaymentResult;
import br.com.economiamod.server.transaction.MenuPaymentService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ClaimChunkCardPurchaseService {
    private final ClaimRepository claims = new ClaimRepository();
    private final ClaimPriceService prices = new ClaimPriceService();
    private final ClaimService claimService = new ClaimService();
    private final MenuPaymentService payments = new MenuPaymentService();

    public ClaimOperationResult purchase(ServerPlayer player, UUID anchorId, String dimension,
                                         int chunkX, int chunkZ) throws SQLException {
        ItemStack card = ClaimPurchaseSessionService.INSTANCE.consume(player.getUUID(), anchorId);
        if (card.isEmpty()) {
            return ClaimOperationResult.denied("authentication_required");
        }
        ClaimAnchorRecord anchor = claims.anchorById(anchorId).orElse(null);
        if (anchor == null || !anchor.active() || anchor.territoryId() == null
                || !anchor.dimension().equals(dimension)) {
            return ClaimOperationResult.denied("owner_required");
        }
        ClaimOperationResult validation = claimService.validateChunkPurchase(
                player.getUUID(), anchorId, dimension, chunkX, chunkZ);
        if (!validation.success()) {
            return validation;
        }
        long amount = prices.landPrice(dimension, chunkX * 16 + 8, chunkZ * 16 + 8);
        String key = "claim-chunk-card:" + anchorId + ":" + dimension + ":" + chunkX + ":" + chunkZ;
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                MenuPaymentResult payment = payments.payDebit(connection, player, card, amount, key);
                if (!payment.success()) {
                    connection.rollback();
                    return ClaimOperationResult.denied("payment_" + payment.code());
                }
                ClaimOperationResult result = claimService.purchaseChunk(
                        connection, player.getUUID(), anchorId, dimension, chunkX, chunkZ);
                if (result.success()) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }
}
