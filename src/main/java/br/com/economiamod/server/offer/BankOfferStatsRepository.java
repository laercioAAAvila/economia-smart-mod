package br.com.economiamod.server.offer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public final class BankOfferStatsRepository {
    public void recordPlayerPurchase(Connection connection, UUID offerId, long quantity, long moneyReceived, int demandLevel, LocalDate businessDate) throws SQLException {
        updateOfferPurchase(connection, offerId, quantity, demandLevel, businessDate);
        upsertStats(connection, offerId, businessDate, quantity, 0L, moneyReceived, 0L, demandLevel, 0);
    }

    public void recordPlayerSale(Connection connection, UUID offerId, long quantity, long moneyPaid, int supplyLevel, LocalDate businessDate) throws SQLException {
        updateOfferSale(connection, offerId, quantity, supplyLevel, businessDate);
        upsertStats(connection, offerId, businessDate, 0L, quantity, 0L, moneyPaid, 0, supplyLevel);
    }

    private void updateOfferPurchase(Connection connection, UUID offerId, long quantity, int demandLevel, LocalDate businessDate) throws SQLException {
        String sql = """
                UPDATE economy_shop_offers
                   SET purchased_quantity = purchased_quantity + ?,
                       demand_level = ?,
                       last_player_purchase_date = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, quantity);
            statement.setInt(2, demandLevel);
            statement.setObject(3, businessDate);
            statement.setObject(4, offerId);
            statement.executeUpdate();
        }
    }

    private void updateOfferSale(Connection connection, UUID offerId, long quantity, int supplyLevel, LocalDate businessDate) throws SQLException {
        String sql = """
                UPDATE economy_shop_offers
                   SET purchased_quantity = purchased_quantity + ?,
                       supply_level = ?,
                       last_player_sale_date = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, quantity);
            statement.setInt(2, supplyLevel);
            statement.setObject(3, businessDate);
            statement.setObject(4, offerId);
            statement.executeUpdate();
        }
    }

    private void upsertStats(Connection connection, UUID offerId, LocalDate businessDate, long bought, long sold, long received, long paid, int demand, int supply) throws SQLException {
        String sql = """
                INSERT INTO economy_offer_daily_stats(
                    id, offer_id, business_date, quantity_bought_from_bank, quantity_sold_to_bank,
                    money_received_by_bank, money_paid_by_bank, highest_demand_level,
                    highest_supply_level, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (offer_id, business_date)
                DO UPDATE SET quantity_bought_from_bank = economy_offer_daily_stats.quantity_bought_from_bank + EXCLUDED.quantity_bought_from_bank,
                              quantity_sold_to_bank = economy_offer_daily_stats.quantity_sold_to_bank + EXCLUDED.quantity_sold_to_bank,
                              money_received_by_bank = economy_offer_daily_stats.money_received_by_bank + EXCLUDED.money_received_by_bank,
                              money_paid_by_bank = economy_offer_daily_stats.money_paid_by_bank + EXCLUDED.money_paid_by_bank,
                              highest_demand_level = GREATEST(economy_offer_daily_stats.highest_demand_level, EXCLUDED.highest_demand_level),
                              highest_supply_level = GREATEST(economy_offer_daily_stats.highest_supply_level, EXCLUDED.highest_supply_level),
                              updated_at = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, offerId);
            statement.setObject(3, businessDate);
            statement.setLong(4, bought);
            statement.setLong(5, sold);
            statement.setLong(6, received);
            statement.setLong(7, paid);
            statement.setInt(8, demand);
            statement.setInt(9, supply);
            statement.executeUpdate();
        }
    }
}
