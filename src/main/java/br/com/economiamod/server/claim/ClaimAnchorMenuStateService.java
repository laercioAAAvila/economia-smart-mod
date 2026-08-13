package br.com.economiamod.server.claim;

import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupSummary;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.group.GroupRepository;
import br.com.economiamod.server.group.ServerActiveClockService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class ClaimAnchorMenuStateService {
    private final ClaimRepository repository = new ClaimRepository();
    private final GroupRepository groups = new GroupRepository();
    private final ClaimPriceService prices = new ClaimPriceService();
    private final ClaimInvoiceService invoices = new ClaimInvoiceService();

    public ClaimAnchorMenuState state(UUID playerUuid, UUID anchorId) throws SQLException {
        ClaimAnchorRecord anchor = repository.anchorById(anchorId).orElseThrow();
        boolean controller = anchor.groupType() == GroupType.PRIVATE_PROPERTY
                ? playerUuid.equals(anchor.active() ? owner(anchor.territoryId()) : anchor.placedByPlayerUuid())
                : groups.membership(playerUuid, anchor.groupId())
                .map(m -> m.role() == GroupRole.LEADER).orElse(false);
        int limit = anchor.groupType() == GroupType.CLAN
                ? EconomyServerConfig.CLAN_MAX_TERRITORIES.get()
                : EconomyServerConfig.PRIVATE_PROPERTY_MAX_TERRITORIES.get();
        int count = territoryCount(anchor.groupType(), anchor.groupId(), playerUuid);
        long landPrice = anchor.active() ? anchor.landPrice()
                : prices.landPrice(anchor.dimension(), anchor.blockX(), anchor.blockZ());
        long now = ServerActiveClockService.INSTANCE.currentMillis();
        long remainingMillis = Math.max(0L, anchor.anchorPaidUntilMillis() - now);
        long remainingDays = remainingMillis / ClaimInvoiceService.MINECRAFT_DAY_MILLIS
                + (remainingMillis % ClaimInvoiceService.MINECRAFT_DAY_MILLIS == 0L ? 0L : 1L);
        int days = (int) Math.min(Integer.MAX_VALUE, remainingDays);
        long anchorPrice = prices.anchorPrice(landPrice, anchor.groupType());
        long suggested = landPrice;
        if (anchor.anchorPaidUntilMillis() > now) {
            suggested = safeAdd(suggested, anchorPrice);
        }
        GroupSummary group = anchor.groupId() == null ? null : groups.group(anchor.groupId()).orElse(null);
        int chunkCount = group == null ? 0 : repository.claimCount(group.id());
        int chunkLimit = group == null ? 0 : group.claimLimit();
        long nextChunkPrice = prices.landPrice(anchor.dimension(), anchor.blockX(), anchor.blockZ());
        boolean canBuyChunk = anchor.active() && controller && group != null && chunkCount < chunkLimit;
        ClaimTaxSummary taxes = anchor.active() && controller
                ? invoices.taxSummary(playerUuid, anchor.territoryId()) : ClaimTaxSummary.empty();
        List<PrivatePropertyMemberView> privateMembers = anchor.active()
                && anchor.groupType() == GroupType.PRIVATE_PROPERTY && controller
                ? privateMembers(anchor.territoryId()) : List.of();
        return new ClaimAnchorMenuState(anchor.id(), anchor.territoryId(), anchor.groupType(), anchor.dimension(),
                anchor.blockX(), anchor.blockY(), anchor.blockZ(), landPrice, anchor.landDebt(), count, limit,
                anchor.active(), controller, controller && !anchor.active() && count < limit,
                anchorPrice, days, EconomyServerConfig.ANCHOR_DEFAULT_MINECRAFT_DAYS.get(),
                EconomyServerConfig.ANCHOR_MAX_MINECRAFT_DAYS.get(), suggested,
                chunkCount, chunkLimit, nextChunkPrice, canBuyChunk,
                taxes.currentAmount(), taxes.totalAmount(), taxes.invoiceCount(),
                EconomyServerConfig.ANCHOR_DEFAULT_MINECRAFT_DAYS.get(), privateMembers);
    }

    private List<PrivatePropertyMemberView> privateMembers(UUID territoryId) throws SQLException {
        List<PrivatePropertyMemberView> members = new ArrayList<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT player_uuid, COALESCE(player_name, '') player_name, permission_mask
                       FROM economy_private_property_members WHERE territory_id = ? ORDER BY created_at
                     """)) {
            statement.setObject(1, territoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID uuid = resultSet.getObject("player_uuid", UUID.class);
                    String name = resultSet.getString("player_name");
                    members.add(new PrivatePropertyMemberView(uuid,
                            name.isBlank() ? uuid.toString().substring(0, 8) : name,
                            resultSet.getInt("permission_mask")));
                }
            }
        }
        return members;
    }

    private UUID owner(UUID territoryId) {
        if (territoryId == null) {
            return null;
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT owner_player_uuid FROM economy_claim_territories WHERE id = ? AND server_uuid = ?")) {
            statement.setObject(1, territoryId);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject(1, UUID.class) : null;
            }
        } catch (SQLException exception) {
            return null;
        }
    }

    private int territoryCount(GroupType type, UUID groupId, UUID playerUuid) throws SQLException {
        String sql = type == GroupType.CLAN
                ? "SELECT COUNT(*) FROM economy_claim_territories WHERE server_uuid = ? AND claim_type = 'CLAN' AND group_id = ?"
                : "SELECT COUNT(*) FROM economy_claim_territories WHERE server_uuid = ? AND claim_type = 'PRIVATE_PROPERTY' AND owner_player_uuid = ?";
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, BankServerIdentityService.INSTANCE.current());
            statement.setObject(2, type == GroupType.CLAN ? groupId : playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private long safeAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
