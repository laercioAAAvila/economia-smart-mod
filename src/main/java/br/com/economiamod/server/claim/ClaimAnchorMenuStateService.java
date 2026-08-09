package br.com.economiamod.server.claim;

import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.group.GroupRepository;
import br.com.economiamod.server.group.ServerActiveClockService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class ClaimAnchorMenuStateService {
    private final ClaimRepository repository = new ClaimRepository();
    private final GroupRepository groups = new GroupRepository();
    private final ClaimPriceService prices = new ClaimPriceService();

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
        long anchorPrice = prices.anchorPrice(landPrice);
        long suggested = landPrice;
        if (anchor.anchorPaidUntilMillis() > now) {
            suggested = safeAdd(suggested, anchorPrice);
        }
        return new ClaimAnchorMenuState(anchor.id(), anchor.territoryId(), anchor.groupType(),
                anchor.blockX(), anchor.blockY(), anchor.blockZ(), landPrice, anchor.landDebt(), count, limit,
                anchor.active(), controller, controller && !anchor.active() && count < limit,
                anchorPrice, days, EconomyServerConfig.ANCHOR_DEFAULT_MINECRAFT_DAYS.get(),
                EconomyServerConfig.ANCHOR_MAX_MINECRAFT_DAYS.get(), suggested);
    }

    private UUID owner(UUID territoryId) {
        if (territoryId == null) {
            return null;
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT owner_player_uuid FROM economy_claim_territories WHERE id = ?")) {
            statement.setObject(1, territoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject(1, UUID.class) : null;
            }
        } catch (SQLException exception) {
            return null;
        }
    }

    private int territoryCount(GroupType type, UUID groupId, UUID playerUuid) throws SQLException {
        String sql = type == GroupType.CLAN
                ? "SELECT COUNT(*) FROM economy_claim_territories WHERE claim_type = 'CLAN' AND group_id = ?"
                : "SELECT COUNT(*) FROM economy_claim_territories WHERE claim_type = 'PRIVATE_PROPERTY' AND owner_player_uuid = ?";
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, type == GroupType.CLAN ? groupId : playerUuid);
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
