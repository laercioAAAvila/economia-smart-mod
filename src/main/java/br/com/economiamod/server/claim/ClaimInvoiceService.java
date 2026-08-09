package br.com.economiamod.server.claim;

import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.account.SystemAccountIds;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.group.GroupRepository;
import br.com.economiamod.server.group.GroupService;
import br.com.economiamod.server.group.ServerActiveClockService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.AccountFinancialService;
import br.com.economiamod.server.transaction.FinancialOperationResult;
import br.com.economiamod.server.transaction.FinancialOperationResultType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class ClaimInvoiceService {
    public static final long MINECRAFT_DAY_MILLIS = 1_200_000L;
    private final GroupRepository groupRepository = new GroupRepository();
    private final GroupService groupService = new GroupService();
    private final AccountQueryService accountQuery = new AccountQueryService();
    private final AccountFinancialService financialService = new AccountFinancialService();
    private final ClaimPriceService priceService = new ClaimPriceService();

    public ClaimInvoiceResult generateAnchorInvoice(UUID actorUuid, UUID territoryId, int days) throws SQLException {
        ClaimInvoiceRecord territory = territory(territoryId);
        if (territory == null || !controls(actorUuid, territory)) {
            return ClaimInvoiceResult.denied("owner_required");
        }
        if (days <= 0 || days > EconomyServerConfig.ANCHOR_MAX_MINECRAFT_DAYS.get()) {
            return ClaimInvoiceResult.denied("invalid_days");
        }
        long current = ServerActiveClockService.INSTANCE.currentMillis();
        long remainingMillis = Math.max(0L, territory.anchorPaidUntilMillis() - current);
        long remainingDays = remainingMillis / MINECRAFT_DAY_MILLIS
                + (remainingMillis % MINECRAFT_DAY_MILLIS == 0L ? 0L : 1L);
        long pendingDays = pendingAnchorDays(territoryId);
        if (remainingDays + pendingDays + days > EconomyServerConfig.ANCHOR_MAX_MINECRAFT_DAYS.get()) {
            return ClaimInvoiceResult.denied("anchor_day_limit");
        }
        long amount = priceService.anchorPrice(territory.landPrice());
        return insertInvoice(territoryId, "ANCHOR", actorUuid, actorUuid, null, null, null, amount, days);
    }

    public ClaimInvoiceResult generateSaleInvoice(UUID sellerUuid, UUID territoryId, UUID buyerUuid,
                                                  long amount) throws SQLException {
        ClaimInvoiceRecord territory = territory(territoryId);
        if (territory == null || buyerUuid == null || buyerUuid.equals(sellerUuid) || amount <= 0L
                || !controls(sellerUuid, territory)) {
            return ClaimInvoiceResult.denied("invalid_sale");
        }
        UUID sellerAccount = accountQuery.findActiveAccountIdByPlayer(sellerUuid).orElse(null);
        if (territory.claimType() == GroupType.CLAN) {
            sellerAccount = groupRepository.group(territory.groupId()).map(group -> group.accountId()).orElse(null);
        }
        if (sellerAccount == null || accountQuery.findActiveAccountIdByPlayer(buyerUuid).isEmpty()) {
            return ClaimInvoiceResult.denied("account_required");
        }
        UUID buyerGroup = null;
        if (territory.claimType() == GroupType.CLAN) {
            GroupMembership membership = groupRepository.membership(buyerUuid, GroupType.CLAN).orElse(null);
            if (membership == null || membership.role() != GroupRole.LEADER) {
                return ClaimInvoiceResult.denied("buyer_clan_leader_required");
            }
            buyerGroup = membership.groupId();
        }
        int limit = territory.claimType() == GroupType.CLAN
                ? EconomyServerConfig.CLAN_MAX_TERRITORIES.get()
                : EconomyServerConfig.PRIVATE_PROPERTY_MAX_TERRITORIES.get();
        if (territoryCount(territory.claimType(), buyerGroup, buyerUuid) >= limit) {
            return ClaimInvoiceResult.denied("buyer_territory_limit");
        }
        cancelPendingSales(territoryId);
        return insertInvoice(territoryId, "SALE", buyerUuid, sellerUuid, sellerUuid, sellerAccount,
                buyerGroup, amount, 0);
    }

    public ClaimInvoiceResult pay(UUID payerUuid, UUID payerAccountId, UUID invoiceId) throws SQLException {
        ClaimInvoiceRecord invoice = invoice(invoiceId, true);
        if (invoice == null || !"PENDING".equals(invoice.status()) || !payerUuid.equals(invoice.debtorPlayerUuid())) {
            return ClaimInvoiceResult.denied("invoice_invalid");
        }
        if ("SALE".equals(invoice.invoiceType())) {
            int limit = invoice.claimType() == GroupType.CLAN
                    ? EconomyServerConfig.CLAN_MAX_TERRITORIES.get()
                    : EconomyServerConfig.PRIVATE_PROPERTY_MAX_TERRITORIES.get();
            if (territoryCount(invoice.claimType(), invoice.buyerGroupId(), payerUuid) >= limit) {
                return ClaimInvoiceResult.denied("buyer_territory_limit");
            }
        }
        UUID destination = "SALE".equals(invoice.invoiceType()) ? invoice.sellerAccountId() : SystemAccountIds.TREASURY;
        if (invoice.amount() > 0L) {
            FinancialOperationResult financial = financialService.transfer(payerUuid, payerAccountId, destination,
                    invoice.amount(), null, "claim:invoice:" + invoice.id());
            if (financial.type() != FinancialOperationResultType.COMPLETED
                    && financial.type() != FinancialOperationResultType.DUPLICATE_COMPLETED) {
                return ClaimInvoiceResult.denied(financial.type().name().toLowerCase());
            }
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ClaimInvoiceRecord locked = invoice(connection, invoiceId, true);
                if (locked == null) {
                    connection.rollback();
                    return ClaimInvoiceResult.denied("invoice_invalid");
                }
                if ("PENDING".equals(locked.status())) {
                    applyPayment(connection, locked, payerUuid);
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE economy_claim_invoices SET status = 'PAID', paid_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                        statement.setObject(1, invoiceId);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                return ClaimInvoiceResult.success(invoiceId, locked.amount());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public long suggestedSalePrice(UUID territoryId) throws SQLException {
        ClaimInvoiceRecord territory = territory(territoryId);
        if (territory == null) {
            return 0L;
        }
        long anchor = territory.anchorPaidUntilMillis() > ServerActiveClockService.INSTANCE.currentMillis()
                ? priceService.anchorPrice(territory.landPrice()) : 0L;
        return safeAdd(territory.landPrice(), anchor);
    }

    public ClaimInvoiceRecord invoice(UUID invoiceId) throws SQLException {
        return invoice(invoiceId, false);
    }

    public List<ClaimInvoiceRecord> pendingForDebtor(UUID playerUuid, UUID territoryId) throws SQLException {
        List<ClaimInvoiceRecord> invoices = new ArrayList<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT i.id, i.territory_id, i.invoice_type, i.debtor_player_uuid,
                            i.seller_player_uuid, i.seller_account_id, i.buyer_group_id, i.amount,
                            i.minecraft_days, i.status, t.claim_type, t.group_id, t.owner_player_uuid,
                            t.land_debt, t.land_price, t.anchor_paid_until_millis
                       FROM economy_claim_invoices i
                       JOIN economy_claim_territories t ON t.id = i.territory_id
                      WHERE i.debtor_player_uuid = ? AND i.territory_id = ? AND i.status = 'PENDING'
                        AND i.invoice_type IN ('LAND', 'ANCHOR')
                      ORDER BY i.created_at
                     """)) {
            statement.setObject(1, playerUuid);
            statement.setObject(2, territoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    invoices.add(readInvoice(resultSet));
                }
            }
        }
        return invoices;
    }

    public List<ClaimInvoiceRecord> reissuePending(UUID controllerUuid, UUID territoryId) throws SQLException {
        ClaimInvoiceRecord territory = territory(territoryId);
        if (territory == null || !controls(controllerUuid, territory)) {
            return List.of();
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_claim_invoices SET debtor_player_uuid = ?
                      WHERE territory_id = ? AND status = 'PENDING'
                        AND invoice_type IN ('LAND', 'ANCHOR')
                     """)) {
            statement.setObject(1, controllerUuid);
            statement.setObject(2, territoryId);
            statement.executeUpdate();
        }
        return pendingForDebtor(controllerUuid, territoryId);
    }

    private void applyPayment(Connection connection, ClaimInvoiceRecord invoice, UUID payerUuid) throws SQLException {
        if ("LAND".equals(invoice.invoiceType())) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE economy_claim_territories SET land_debt = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                    """)) {
                statement.setObject(1, invoice.territoryId());
                statement.executeUpdate();
            }
            return;
        }
        if ("ANCHOR".equals(invoice.invoiceType())) {
            long now = ServerActiveClockService.INSTANCE.currentMillis();
            long base = Math.max(now, invoice.anchorPaidUntilMillis());
            long extension = safeMultiply(invoice.minecraftDays(), MINECRAFT_DAY_MILLIS);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE economy_claim_territories
                       SET anchor_paid_until_millis = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                    """)) {
                statement.setLong(1, safeAdd(base, extension));
                statement.setObject(2, invoice.territoryId());
                statement.executeUpdate();
            }
            return;
        }
        if ("SALE".equals(invoice.invoiceType())) {
            UUID targetGroup = invoice.buyerGroupId();
            if (invoice.claimType() == GroupType.PRIVATE_PROPERTY) {
                targetGroup = groupService.ensurePrivatePropertyPortfolio(
                        payerUuid, ServerActiveClockService.INSTANCE.currentMillis());
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE economy_claim_territories
                       SET group_id = ?, owner_player_uuid = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                    """)) {
                statement.setObject(1, targetGroup);
                statement.setObject(2, invoice.claimType() == GroupType.PRIVATE_PROPERTY ? payerUuid : null);
                statement.setObject(3, invoice.territoryId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE economy_claim_anchors SET group_id = ? WHERE territory_id = ?")) {
                statement.setObject(1, targetGroup);
                statement.setObject(2, invoice.territoryId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE economy_claims SET group_id = ? WHERE territory_id = ?")) {
                statement.setObject(1, targetGroup);
                statement.setObject(2, invoice.territoryId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE economy_claim_invoices SET debtor_player_uuid = ?
                     WHERE territory_id = ? AND invoice_type IN ('LAND', 'ANCHOR') AND status = 'PENDING'
                    """)) {
                statement.setObject(1, payerUuid);
                statement.setObject(2, invoice.territoryId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM economy_private_property_members WHERE territory_id = ?")) {
                statement.setObject(1, invoice.territoryId());
                statement.executeUpdate();
            }
        }
    }

    private boolean controls(UUID playerUuid, ClaimInvoiceRecord territory) throws SQLException {
        if (territory.claimType() == GroupType.PRIVATE_PROPERTY) {
            return playerUuid.equals(territory.ownerPlayerUuid());
        }
        return groupRepository.membership(playerUuid, territory.groupId())
                .map(m -> m.role() == GroupRole.LEADER).orElse(false);
    }

    private ClaimInvoiceResult insertInvoice(UUID territoryId, String type, UUID debtor, UUID issuer,
                                             UUID seller, UUID sellerAccount, UUID buyerGroup,
                                             long amount, int days) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO economy_claim_invoices(
                         id, territory_id, invoice_type, debtor_player_uuid, issuer_player_uuid,
                         seller_player_uuid, seller_account_id, buyer_group_id, amount, minecraft_days,
                         status, created_at
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)
                     """)) {
            statement.setObject(1, id);
            statement.setObject(2, territoryId);
            statement.setString(3, type);
            statement.setObject(4, debtor);
            statement.setObject(5, issuer);
            statement.setObject(6, seller);
            statement.setObject(7, sellerAccount);
            statement.setObject(8, buyerGroup);
            statement.setLong(9, amount);
            statement.setInt(10, days);
            statement.executeUpdate();
        }
        return ClaimInvoiceResult.success(id, amount);
    }

    private void cancelPendingSales(UUID territoryId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_claim_invoices SET status = 'CANCELLED'
                      WHERE territory_id = ? AND invoice_type = 'SALE' AND status = 'PENDING'
                     """)) {
            statement.setObject(1, territoryId);
            statement.executeUpdate();
        }
    }

    private long pendingAnchorDays(UUID territoryId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COALESCE(SUM(minecraft_days), 0)
                       FROM economy_claim_invoices
                      WHERE territory_id = ? AND invoice_type = 'ANCHOR' AND status = 'PENDING'
                     """)) {
            statement.setObject(1, territoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private int territoryCount(GroupType type, UUID groupId, UUID ownerUuid) throws SQLException {
        String sql = type == GroupType.CLAN
                ? "SELECT COUNT(*) FROM economy_claim_territories WHERE claim_type = 'CLAN' AND group_id = ?"
                : "SELECT COUNT(*) FROM economy_claim_territories WHERE claim_type = 'PRIVATE_PROPERTY' AND owner_player_uuid = ?";
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, type == GroupType.CLAN ? groupId : ownerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private ClaimInvoiceRecord territory(UUID territoryId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT t.id territory_id, t.claim_type, t.group_id, t.owner_player_uuid,
                            t.land_debt, t.land_price, t.anchor_paid_until_millis
                       FROM economy_claim_territories t WHERE t.id = ?
                     """)) {
            statement.setObject(1, territoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? readTerritory(resultSet) : null;
            }
        }
    }

    private ClaimInvoiceRecord invoice(UUID invoiceId, boolean lock) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            return invoice(connection, invoiceId, lock);
        }
    }

    private ClaimInvoiceRecord invoice(Connection connection, UUID invoiceId, boolean lock) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT i.id, i.territory_id, i.invoice_type, i.debtor_player_uuid,
                       i.seller_player_uuid, i.seller_account_id, i.buyer_group_id, i.amount,
                       i.minecraft_days, i.status, t.claim_type, t.group_id, t.owner_player_uuid,
                       t.land_debt, t.land_price, t.anchor_paid_until_millis
                  FROM economy_claim_invoices i
                  JOIN economy_claim_territories t ON t.id = i.territory_id
                 WHERE i.id = ?
                """ + (lock ? " FOR UPDATE" : ""))) {
            statement.setObject(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? readInvoice(resultSet) : null;
            }
        }
    }

    private ClaimInvoiceRecord readTerritory(ResultSet resultSet) throws SQLException {
        return new ClaimInvoiceRecord(null, resultSet.getObject("territory_id", UUID.class), null,
                null, null, null, null, 0L, 0, "", GroupType.valueOf(resultSet.getString("claim_type")),
                resultSet.getObject("group_id", UUID.class), resultSet.getObject("owner_player_uuid", UUID.class),
                resultSet.getLong("land_debt"), resultSet.getLong("land_price"),
                resultSet.getLong("anchor_paid_until_millis"));
    }

    private ClaimInvoiceRecord readInvoice(ResultSet resultSet) throws SQLException {
        return new ClaimInvoiceRecord(resultSet.getObject("id", UUID.class),
                resultSet.getObject("territory_id", UUID.class), resultSet.getString("invoice_type"),
                resultSet.getObject("debtor_player_uuid", UUID.class),
                resultSet.getObject("seller_player_uuid", UUID.class),
                resultSet.getObject("seller_account_id", UUID.class),
                resultSet.getObject("buyer_group_id", UUID.class), resultSet.getLong("amount"),
                resultSet.getInt("minecraft_days"), resultSet.getString("status"),
                GroupType.valueOf(resultSet.getString("claim_type")), resultSet.getObject("group_id", UUID.class),
                resultSet.getObject("owner_player_uuid", UUID.class), resultSet.getLong("land_debt"),
                resultSet.getLong("land_price"), resultSet.getLong("anchor_paid_until_millis"));
    }

    private long safeMultiply(long left, long right) {
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private long safeAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
