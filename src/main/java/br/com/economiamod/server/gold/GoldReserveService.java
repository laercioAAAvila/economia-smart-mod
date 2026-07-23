package br.com.economiamod.server.gold;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class GoldReserveService {
    private static final UUID OFFICIAL_RESERVE_ID = UUID.nameUUIDFromBytes("economia:gold_reserve:official".getBytes(StandardCharsets.UTF_8));

    public void initialize() throws SQLException {
        String sql = """
                INSERT INTO economy_gold_reserve_summary(
                    id, reserve_code, gold_nugget_units, currency_issued, currency_redeemed, updated_at, version
                )
                VALUES (?, ?, 0, 0, 0, CURRENT_TIMESTAMP, 1)
                ON CONFLICT (reserve_code) DO NOTHING
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, OFFICIAL_RESERVE_ID);
            statement.setString(2, GoldReserveCode.OFFICIAL);
            statement.executeUpdate();
        }
    }

    public Optional<GoldReserveSummary> summary() throws SQLException {
        String sql = """
                SELECT gold_nugget_units, currency_issued, currency_redeemed
                  FROM economy_gold_reserve_summary
                 WHERE reserve_code = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, GoldReserveCode.OFFICIAL);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new GoldReserveSummary(
                        resultSet.getLong("gold_nugget_units"),
                        resultSet.getLong("currency_issued"),
                        resultSet.getLong("currency_redeemed")
                ));
            }
        }
    }
}

