package br.com.economiamod.server.transaction;

import br.com.economiamod.common.credit.CreditLimitPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class AccountFinancialRepository {
    public Optional<AccountFinancialSnapshot> lockPlayerAccount(Connection connection, UUID accountId) throws SQLException {
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
                return Optional.of(new AccountFinancialSnapshot(
                        resultSet.getString("status"),
                        resultSet.getLong("balance"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                ));
            }
        }
    }

    public void updateBalance(Connection connection, UUID accountId, long balanceAfter) throws SQLException {
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
}
