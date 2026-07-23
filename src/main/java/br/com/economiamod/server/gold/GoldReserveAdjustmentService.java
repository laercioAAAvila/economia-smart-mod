package br.com.economiamod.server.gold;

import br.com.economiamod.server.audit.AuditLogService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class GoldReserveAdjustmentService {
    private final AuditLogService auditLogService = new AuditLogService();

    public GoldReserveAdjustmentResult adjustGoldNuggetUnits(UUID adminPlayerUuid, long delta) throws SQLException {
        if (delta == 0L) {
            throw new IllegalArgumentException("delta cannot be zero");
        }
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long before = lockReserve(connection);
                long after = before + delta;
                if (after < 0L) {
                    connection.rollback();
                    return GoldReserveAdjustmentResult.insufficient(before);
                }
                updateReserve(connection, after);
                auditLogService.recordAdminChange(connection, adminPlayerUuid, "GOLD_RESERVE_ADJUSTMENT", "GOLD_RESERVE", null, Long.toString(before), Long.toString(after));
                connection.commit();
                return GoldReserveAdjustmentResult.completed(before, after);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private long lockReserve(Connection connection) throws SQLException {
        String sql = """
                SELECT gold_nugget_units
                  FROM economy_gold_reserve_summary
                 WHERE reserve_code = ?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, GoldReserveCode.OFFICIAL);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("gold_nugget_units");
            }
        }
    }

    private void updateReserve(Connection connection, long goldNuggetUnits) throws SQLException {
        String sql = """
                UPDATE economy_gold_reserve_summary
                   SET gold_nugget_units = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE reserve_code = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, goldNuggetUnits);
            statement.setString(2, GoldReserveCode.OFFICIAL);
            statement.executeUpdate();
        }
    }
}
