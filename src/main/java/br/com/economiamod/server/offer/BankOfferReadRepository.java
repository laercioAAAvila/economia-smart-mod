package br.com.economiamod.server.offer;

import br.com.economiamod.common.pricing.ComparisonMode;
import br.com.economiamod.common.pricing.PricingMode;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class BankOfferReadRepository {
    public Connection openConnection() throws SQLException {
        return EconomyDatabase.getConnection();
    }

    public Optional<BankOfferSnapshot> findById(UUID offerId) throws SQLException {
        try (Connection connection = openConnection()) {
            return lockById(connection, offerId, false);
        }
    }

    public Optional<BankOfferSnapshot> lockById(Connection connection, UUID offerId) throws SQLException {
        return lockById(connection, offerId, true);
    }

    public Optional<BankOfferSnapshot> lockByBlockSlot(Connection connection, UUID commercialBlockId, int slotIndex) throws SQLException {
        String sql = offerSelectSql("""
                 WHERE commercial_block_id = ?
                   AND slot_index = ?
                """) + " FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, commercialBlockId);
            statement.setInt(2, slotIndex);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
            }
        }
    }

    private Optional<BankOfferSnapshot> lockById(Connection connection, UUID offerId, boolean forUpdate) throws SQLException {
        String sql = offerSelectSql(" WHERE id = ?") + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, offerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
            }
        }
    }

    private String offerSelectSql(String whereClause) {
        return """
                SELECT id, commercial_block_id, slot_index, item_id, item_components, item_data_version,
                       quantity_per_operation, base_buy_price, base_sell_price, minimum_buy_price,
                       maximum_sell_price, target_quantity, purchased_quantity, comparison_mode,
                       pricing_mode, demand_level, supply_level, quantity_per_price_level,
                       demand_increase_bps, supply_decrease_bps, recovery_levels_per_idle_day,
                       maximum_demand_level, maximum_supply_level, last_player_purchase_date,
                       last_player_sale_date, is_buy_enabled, is_sell_enabled, version
                  FROM economy_shop_offers
                """ + whereClause;
    }

    public BankOfferSnapshot read(ResultSet resultSet) throws SQLException {
        return new BankOfferSnapshot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("commercial_block_id", UUID.class),
                resultSet.getInt("slot_index"),
                resultSet.getString("item_id"),
                resultSet.getString("item_components"),
                (Integer) resultSet.getObject("item_data_version"),
                resultSet.getInt("quantity_per_operation"),
                nullableLong(resultSet, "base_buy_price"),
                nullableLong(resultSet, "base_sell_price"),
                nullableLong(resultSet, "minimum_buy_price"),
                nullableLong(resultSet, "maximum_sell_price"),
                nullableLong(resultSet, "target_quantity"),
                resultSet.getLong("purchased_quantity"),
                ComparisonMode.valueOf(resultSet.getString("comparison_mode")),
                PricingMode.valueOf(resultSet.getString("pricing_mode")),
                resultSet.getInt("demand_level"),
                resultSet.getInt("supply_level"),
                nullableLong(resultSet, "quantity_per_price_level"),
                (Integer) resultSet.getObject("demand_increase_bps"),
                (Integer) resultSet.getObject("supply_decrease_bps"),
                resultSet.getInt("recovery_levels_per_idle_day"),
                resultSet.getInt("maximum_demand_level"),
                resultSet.getInt("maximum_supply_level"),
                resultSet.getObject("last_player_purchase_date", java.time.LocalDate.class),
                resultSet.getObject("last_player_sale_date", java.time.LocalDate.class),
                resultSet.getBoolean("is_buy_enabled"),
                resultSet.getBoolean("is_sell_enabled"),
                resultSet.getLong("version")
        );
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
