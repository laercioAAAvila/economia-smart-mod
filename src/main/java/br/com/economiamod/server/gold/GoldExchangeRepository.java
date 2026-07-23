package br.com.economiamod.server.gold;

import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class GoldExchangeRepository {
    public Connection openConnection() throws SQLException {
        return EconomyDatabase.getConnection();
    }

    public Optional<GoldAccountSnapshot> lockAccount(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT status, balance, credit_principal_outstanding, credit_interest_outstanding
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new GoldAccountSnapshot(
                        resultSet.getString("status"),
                        resultSet.getLong("balance"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                ));
            }
        }
    }

    public Optional<GoldAccountSnapshot> lockAnyAccount(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT status, balance, credit_principal_outstanding, credit_interest_outstanding
                  FROM economy_accounts
                 WHERE id = ?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new GoldAccountSnapshot(
                        resultSet.getString("status"),
                        resultSet.getLong("balance"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                ));
            }
        }
    }

    public GoldReserveSnapshot lockReserve(Connection connection) throws SQLException {
        String sql = """
                SELECT gold_nugget_units, currency_issued, currency_redeemed
                  FROM economy_gold_reserve_summary
                 WHERE reserve_code = ?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, GoldReserveCode.OFFICIAL);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new GoldReserveSnapshot(
                        resultSet.getLong("gold_nugget_units"),
                        resultSet.getLong("currency_issued"),
                        resultSet.getLong("currency_redeemed")
                );
            }
        }
    }

    public void updateAccountBalance(Connection connection, UUID accountId, long balanceAfter) throws SQLException {
        String sql = """
                UPDATE economy_accounts
                   SET balance = ?,
                       configured_credit_limit = LEAST(configured_credit_limit, ?),
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, balanceAfter);
            statement.setLong(2, CreditLimitPolicy.limitForBalance(balanceAfter));
            statement.setObject(3, accountId);
            statement.executeUpdate();
        }
    }

    public void mintReserve(Connection connection, long goldNuggetUnits, long moneyAmount) throws SQLException {
        updateReserve(connection, goldNuggetUnits, moneyAmount, 0L);
    }

    public void redeemReserve(Connection connection, long goldNuggetUnits, long moneyAmount) throws SQLException {
        updateReserve(connection, -goldNuggetUnits, 0L, moneyAmount);
    }

    private void updateReserve(Connection connection, long goldDelta, long issuedDelta, long redeemedDelta) throws SQLException {
        String sql = """
                UPDATE economy_gold_reserve_summary
                   SET gold_nugget_units = gold_nugget_units + ?,
                       currency_issued = currency_issued + ?,
                       currency_redeemed = currency_redeemed + ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE reserve_code = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, goldDelta);
            statement.setLong(2, issuedDelta);
            statement.setLong(3, redeemedDelta);
            statement.setString(4, GoldReserveCode.OFFICIAL);
            statement.executeUpdate();
        }
    }
}
