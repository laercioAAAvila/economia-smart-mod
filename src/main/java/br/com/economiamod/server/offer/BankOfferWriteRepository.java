package br.com.economiamod.server.offer;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

public final class BankOfferWriteRepository {
    public UUID saveAdminOffer(BankOfferDraft draft) throws SQLException {
        UUID offerId = UUID.randomUUID();
        String sql = """
                INSERT INTO economy_shop_offers(
                    id, commercial_block_id, slot_index, item_id, item_components, item_data_version,
                    quantity_per_operation, base_buy_price, base_sell_price, minimum_buy_price,
                    maximum_sell_price, target_quantity, purchased_quantity, comparison_mode, pricing_mode,
                    demand_level, supply_level, quantity_per_price_level, demand_increase_bps,
                    supply_decrease_bps, recovery_levels_per_idle_day, maximum_demand_level,
                    maximum_supply_level, is_buy_enabled, is_sell_enabled, created_at, updated_at, version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 0, 0, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)
                ON CONFLICT (commercial_block_id, slot_index)
                DO UPDATE SET item_id = EXCLUDED.item_id,
                              item_components = EXCLUDED.item_components,
                              item_data_version = EXCLUDED.item_data_version,
                              quantity_per_operation = EXCLUDED.quantity_per_operation,
                              base_buy_price = EXCLUDED.base_buy_price,
                              base_sell_price = EXCLUDED.base_sell_price,
                              minimum_buy_price = EXCLUDED.minimum_buy_price,
                              maximum_sell_price = EXCLUDED.maximum_sell_price,
                              target_quantity = EXCLUDED.target_quantity,
                              purchased_quantity = CASE
                                  WHEN economy_shop_offers.item_id IS DISTINCT FROM EXCLUDED.item_id
                                    OR COALESCE(economy_shop_offers.item_components, '') IS DISTINCT FROM COALESCE(EXCLUDED.item_components, '')
                                  THEN 0
                                  WHEN EXCLUDED.target_quantity IS NULL
                                  THEN economy_shop_offers.purchased_quantity
                                  ELSE LEAST(economy_shop_offers.purchased_quantity, EXCLUDED.target_quantity)
                              END,
                              comparison_mode = EXCLUDED.comparison_mode,
                              pricing_mode = EXCLUDED.pricing_mode,
                              quantity_per_price_level = EXCLUDED.quantity_per_price_level,
                              demand_increase_bps = EXCLUDED.demand_increase_bps,
                              supply_decrease_bps = EXCLUDED.supply_decrease_bps,
                              recovery_levels_per_idle_day = EXCLUDED.recovery_levels_per_idle_day,
                              maximum_demand_level = EXCLUDED.maximum_demand_level,
                              maximum_supply_level = EXCLUDED.maximum_supply_level,
                              is_buy_enabled = EXCLUDED.is_buy_enabled,
                              is_sell_enabled = EXCLUDED.is_sell_enabled,
                              updated_at = CURRENT_TIMESTAMP,
                              version = economy_shop_offers.version + 1
                RETURNING id
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindDraft(statement, offerId, draft);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getObject("id", UUID.class);
            }
        }
    }

    public void updateLevels(Connection connection, UUID offerId, int demandLevel, int supplyLevel) throws SQLException {
        String sql = """
                UPDATE economy_shop_offers
                   SET demand_level = ?,
                       supply_level = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, demandLevel);
            statement.setInt(2, supplyLevel);
            statement.setObject(3, offerId);
            statement.executeUpdate();
        }
    }

    public void resetPricing(UUID offerId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            updateLevels(connection, offerId, 0, 0);
        }
    }

    public void setEnabledByBlockSlot(UUID commercialBlockId, int slotIndex, boolean buyEnabled, boolean sellEnabled) throws SQLException {
        String sql = """
                UPDATE economy_shop_offers
                   SET is_buy_enabled = ?,
                       is_sell_enabled = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE commercial_block_id = ?
                   AND slot_index = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, buyEnabled);
            statement.setBoolean(2, sellEnabled);
            statement.setObject(3, commercialBlockId);
            statement.setInt(4, slotIndex);
            statement.executeUpdate();
        }
    }

    public void incrementPurchasedByBlockSlot(UUID commercialBlockId, int slotIndex, long quantity) throws SQLException {
        String sql = """
                UPDATE economy_shop_offers
                   SET purchased_quantity = purchased_quantity + ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE commercial_block_id = ?
                   AND slot_index = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, quantity);
            statement.setObject(2, commercialBlockId);
            statement.setInt(3, slotIndex);
            statement.executeUpdate();
        }
    }

    private void bindDraft(PreparedStatement statement, UUID offerId, BankOfferDraft draft) throws SQLException {
        statement.setObject(1, offerId);
        statement.setObject(2, draft.commercialBlockId());
        statement.setInt(3, draft.slotIndex());
        statement.setString(4, draft.itemId());
        statement.setString(5, draft.itemComponents());
        setNullableInteger(statement, 6, draft.itemDataVersion());
        statement.setInt(7, draft.quantityPerOperation());
        setNullableLong(statement, 8, draft.baseBuyPrice());
        setNullableLong(statement, 9, draft.baseSellPrice());
        setNullableLong(statement, 10, draft.minimumBuyPrice());
        setNullableLong(statement, 11, draft.maximumSellPrice());
        setNullableLong(statement, 12, draft.targetQuantity());
        statement.setString(13, draft.comparisonMode().name());
        statement.setString(14, draft.pricingMode().name());
        setNullableLong(statement, 15, draft.quantityPerPriceLevel());
        setNullableInteger(statement, 16, draft.demandIncreaseBps());
        setNullableInteger(statement, 17, draft.supplyDecreaseBps());
        statement.setInt(18, draft.recoveryLevelsPerIdleDay());
        statement.setInt(19, draft.maximumDemandLevel());
        statement.setInt(20, draft.maximumSupplyLevel());
        statement.setBoolean(21, draft.buyEnabled());
        statement.setBoolean(22, draft.sellEnabled());
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }
}
