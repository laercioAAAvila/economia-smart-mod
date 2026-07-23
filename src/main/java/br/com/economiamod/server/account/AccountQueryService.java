package br.com.economiamod.server.account;

import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.common.credit.CreditMath;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class AccountQueryService {
    public Optional<AccountIdentity> findActiveIdentity(UUID accountId) throws SQLException {
        String sql = """
                SELECT username,
                       account_number
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                   AND status = 'ACTIVE'
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(new AccountIdentity(resultSet.getString("username"), resultSet.getString("account_number")))
                        : Optional.empty();
            }
        }
    }

    public Optional<String> findActiveUsername(UUID accountId) throws SQLException {
        return findActiveIdentity(accountId).map(AccountIdentity::username);
    }

    public Optional<UUID> findActiveAccountIdByPlayer(UUID playerUuid) throws SQLException {
        String sql = """
                SELECT id
                  FROM economy_accounts
                 WHERE player_uuid = ?
                   AND account_type = 'PLAYER'
                   AND status = 'ACTIVE'
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerUuid);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getObject("id", UUID.class)) : Optional.empty();
            }
        }
    }

    public Optional<UUID> findActiveAccountIdByNumber(String accountNumber) throws SQLException {
        String sql = """
                SELECT id
                  FROM economy_accounts
                 WHERE account_number = ?
                   AND account_type = 'PLAYER'
                   AND status = 'ACTIVE'
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getObject("id", UUID.class)) : Optional.empty();
            }
        }
    }

    public Optional<AccountBalanceSummary> findBalanceSummary(UUID accountId) throws SQLException {
        String sql = """
                SELECT username,
                       account_number,
                       balance,
                       configured_credit_limit,
                       credit_principal_outstanding,
                       credit_interest_outstanding
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                   AND status = 'ACTIVE'
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                long balance = resultSet.getLong("balance");
                long configuredCreditLimit = resultSet.getLong("configured_credit_limit");
                long effectiveCreditLimit = CreditLimitPolicy.effectiveLimit(balance, configuredCreditLimit);
                long principal = resultSet.getLong("credit_principal_outstanding");
                long interest = resultSet.getLong("credit_interest_outstanding");
                long totalDebt = CreditMath.debtTotal(principal, interest);

                return Optional.of(new AccountBalanceSummary(
                        resultSet.getString("username"),
                        resultSet.getString("account_number"),
                        balance,
                        CreditMath.availableBalance(balance, principal, interest),
                        effectiveCreditLimit,
                        principal,
                        interest,
                        totalDebt,
                        CreditMath.globalCreditAvailable(effectiveCreditLimit, principal, interest)
                ));
            }
        }
    }
}
