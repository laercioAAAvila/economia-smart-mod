package br.com.economiamod.server.claim;

import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.MenuPaymentResult;
import br.com.economiamod.server.transaction.MenuPaymentService;
import br.com.economiamod.server.transaction.PaymentTransactionWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class ClaimDirectPaymentService {
    private final ClaimAnchorMenuStateService states = new ClaimAnchorMenuStateService();
    private final ClaimRepository claims = new ClaimRepository();
    private final ClaimService claimService = new ClaimService();
    private final MenuPaymentService payments = new MenuPaymentService();
    private final PaymentTransactionWriter transactions = new PaymentTransactionWriter();

    public ClaimOperationResult payAndActivate(ServerPlayer player, UUID anchorId,
                                               DirectPaymentMethod requestedMethod, ItemStack card,
                                               Container cash, long expectedAmount, UUID requestId) throws SQLException {
        ClaimAnchorMenuState state = states.state(player.getUUID(), anchorId);
        if (state.active()) {
            complete(anchorId);
            return ClaimOperationResult.success(state.territoryId());
        }
        if (!state.canClaim() || state.landPrice() <= 0L) {
            return ClaimOperationResult.denied("claim_limit");
        }
        if (expectedAmount != state.landPrice()) {
            return ClaimOperationResult.denied("payment_price_changed");
        }
        PaymentRecord record = prepare(player.getUUID(), anchorId, state.landPrice(), requestedMethod,
                requestId == null ? UUID.randomUUID() : requestId);
        if (record == null) {
            return ClaimOperationResult.denied("payment_in_progress");
        }
        if (!record.paid()) {
            MenuPaymentResult payment = payments.pay(player, record.method(), card, cash, record.amount(),
                    "Claim", paymentKey(record.id()));
            if (!payment.success()) {
                if (!ambiguousPayment(payment)) {
                    deleteUnpaid(record.id());
                }
                return ClaimOperationResult.denied("payment_" + payment.code());
            }
            markPaid(record.id());
        }

        ClaimOperationResult activation = claimService.activatePaidAnchor(player.getUUID(), anchorId);
        if (!activation.success()) {
            ClaimAnchorRecord anchor = claims.anchorById(anchorId).orElse(null);
            if (anchor == null || !anchor.active()) {
                return activation;
            }
        }
        complete(anchorId);
        return activation.success() ? activation : ClaimOperationResult.success(anchorId);
    }

    private PaymentRecord prepare(UUID playerUuid, UUID anchorId, long currentAmount,
                                  DirectPaymentMethod requestedMethod, UUID requestId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                PaymentRecord existing = find(connection, anchorId, true);
                if (existing != null) {
                    if (!existing.payerUuid().equals(playerUuid)) {
                        connection.rollback();
                        return null;
                    }
                    boolean charged = existing.paid()
                            || transactions.completedTransactionExists(connection, paymentKey(existing.id()));
                    if (charged) {
                        if (!existing.paid()) {
                            updatePaid(connection, existing.id());
                        }
                        connection.commit();
                        return new PaymentRecord(existing.id(), existing.anchorId(), existing.payerUuid(),
                                existing.amount(), existing.method(), true);
                    }
                    if (existing.amount() != currentAmount || existing.method() != requestedMethod) {
                        delete(connection, existing.id());
                    } else {
                        connection.commit();
                        return existing;
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO economy_claim_direct_payments(
                            id, anchor_id, payer_player_uuid, amount, payment_method, status, created_at
                        ) VALUES (?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)
                        """)) {
                    statement.setObject(1, requestId);
                    statement.setObject(2, anchorId);
                    statement.setObject(3, playerUuid);
                    statement.setLong(4, currentAmount);
                    statement.setString(5, requestedMethod.name());
                    statement.executeUpdate();
                }
                connection.commit();
                return new PaymentRecord(requestId, anchorId, playerUuid, currentAmount, requestedMethod, false);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void markPaid(UUID id) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            updatePaid(connection, id);
        }
    }

    private void updatePaid(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE economy_claim_direct_payments
                   SET status = 'PAID', paid_at = COALESCE(paid_at, CURRENT_TIMESTAMP)
                 WHERE id = ? AND status = 'PENDING'
                """)) {
            statement.setObject(1, id);
            statement.executeUpdate();
        }
    }

    private void complete(UUID anchorId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_claim_direct_payments
                        SET status = 'COMPLETED', completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP)
                      WHERE anchor_id = ? AND status <> 'COMPLETED'
                     """)) {
            statement.setObject(1, anchorId);
            statement.executeUpdate();
        }
    }

    private void deleteUnpaid(UUID id) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            delete(connection, id);
        }
    }

    private void delete(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM economy_claim_direct_payments WHERE id = ? AND status = 'PENDING'")) {
            statement.setObject(1, id);
            statement.executeUpdate();
        }
    }

    private PaymentRecord find(Connection connection, UUID anchorId, boolean lock) throws SQLException {
        String sql = """
                SELECT id, anchor_id, payer_player_uuid, amount, payment_method, status
                  FROM economy_claim_direct_payments WHERE anchor_id = ?
                """ + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, anchorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new PaymentRecord(resultSet.getObject("id", UUID.class),
                        resultSet.getObject("anchor_id", UUID.class),
                        resultSet.getObject("payer_player_uuid", UUID.class),
                        resultSet.getLong("amount"),
                        DirectPaymentMethod.valueOf(resultSet.getString("payment_method")),
                        !"PENDING".equals(resultSet.getString("status")));
            }
        }
    }

    private String paymentKey(UUID id) {
        return "claim-direct:" + id;
    }

    private boolean ambiguousPayment(MenuPaymentResult payment) {
        return "reconciliation_required".equals(payment.code())
                || "idempotency_conflict".equals(payment.code());
    }

    private record PaymentRecord(UUID id, UUID anchorId, UUID payerUuid, long amount,
                                 DirectPaymentMethod method, boolean paid) {
    }
}
