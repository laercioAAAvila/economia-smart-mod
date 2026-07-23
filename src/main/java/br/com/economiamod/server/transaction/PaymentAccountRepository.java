package br.com.economiamod.server.transaction;

import br.com.economiamod.common.credit.CreditLimitPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PaymentAccountRepository {
    public void lockAccountsOrdered(Connection connection, UUID firstAccountId, UUID secondAccountId) throws SQLException {
        List<UUID> lockOrder = List.of(firstAccountId, secondAccountId).stream()
                .distinct()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        for (UUID accountId : lockOrder) {
            lockAccount(connection, accountId);
        }
    }

    public Optional<PaymentAccountSnapshot> findPaymentAccount(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT status,
                       balance,
                       configured_credit_limit,
                       credit_principal_outstanding,
                       credit_interest_outstanding
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type IN ('PLAYER', 'SYSTEM_TREASURY')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PaymentAccountSnapshot(
                        resultSet.getString("status"),
                        resultSet.getLong("balance"),
                        resultSet.getLong("configured_credit_limit"),
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

    public void increaseCreditPrincipal(Connection connection, UUID accountId, UUID cardId, long amount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE economy_accounts
                   SET credit_principal_outstanding = credit_principal_outstanding + ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """)) {
            statement.setLong(1, amount);
            statement.setObject(2, accountId);
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE economy_cards
                   SET credit_principal_outstanding = credit_principal_outstanding + ?,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """)) {
            statement.setLong(1, amount);
            statement.setObject(2, cardId);
            statement.executeUpdate();
        }
    }

    private void lockAccount(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM economy_accounts WHERE id = ? FOR UPDATE")) {
            statement.setObject(1, accountId);
            try (ResultSet ignored = statement.executeQuery()) {
            }
        }
    }
}
