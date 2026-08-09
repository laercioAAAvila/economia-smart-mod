package br.com.economiamod.server.account;

import br.com.economiamod.common.account.AccountNameNormalizer;
import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.account.AccountType;
import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.security.PasswordHash;
import br.com.economiamod.server.security.PasswordService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class AccountService {
    private final PasswordService passwordService;

    public AccountService(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    public CreateAccountResult createPlayerAccount(UUID playerUuid, String username, char[] password) throws SQLException {
        String normalizedUsername = AccountNameNormalizer.normalize(username);
        PasswordHash passwordHash = passwordService.hash(password);

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (playerHasAccount(connection, playerUuid)) {
                    connection.rollback();
                    return CreateAccountResult.playerAlreadyHasAccount();
                }

                if (usernameExists(connection, normalizedUsername)) {
                    connection.rollback();
                    return CreateAccountResult.usernameAlreadyUsed();
                }

                insertPlayerAccount(connection, playerUuid, username.trim(), normalizedUsername, nextAccountNumber(connection), passwordHash);
                connection.commit();
                return CreateAccountResult.created();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public AuthenticateAccountResult authenticate(String username, char[] password) throws SQLException {
        String normalizedUsername = AccountNameNormalizer.normalize(username);
        String sql = """
                SELECT id,
                       username,
                       account_number,
                       password_hash,
                       password_salt,
                       password_algorithm,
                       status
                  FROM economy_accounts
                 WHERE account_type = 'PLAYER'
                   AND username_normalized = ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedUsername);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return AuthenticateAccountResult.notFound();
                }

                if (!AccountStatus.ACTIVE.name().equals(resultSet.getString("status"))) {
                    return AuthenticateAccountResult.inactiveAccount();
                }

                PasswordHash storedHash = new PasswordHash(
                        resultSet.getString("password_algorithm"),
                        resultSet.getString("password_salt"),
                        resultSet.getString("password_hash")
                );

                if (!passwordService.verify(password, storedHash)) {
                    return AuthenticateAccountResult.invalidPassword();
                }

                UUID accountId = resultSet.getObject("id", UUID.class);
                updateLastLogin(connection, accountId);
                return AuthenticateAccountResult.authenticated(accountId, resultSet.getString("username"), resultSet.getString("account_number"));
            }
        }
    }

    public ChangePasswordResult changePassword(UUID accountId, char[] currentPassword, char[] newPassword) throws SQLException {
        PasswordHash newPasswordHash = passwordService.hash(newPassword);
        String sql = """
                SELECT password_hash,
                       password_salt,
                       password_algorithm,
                       status
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                """;

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, accountId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return ChangePasswordResult.notFound();
                    }

                    if (!AccountStatus.ACTIVE.name().equals(resultSet.getString("status"))) {
                        connection.rollback();
                        return ChangePasswordResult.inactiveAccount();
                    }

                    PasswordHash storedHash = new PasswordHash(
                            resultSet.getString("password_algorithm"),
                            resultSet.getString("password_salt"),
                            resultSet.getString("password_hash")
                    );

                    if (!passwordService.verify(currentPassword, storedHash)) {
                        connection.rollback();
                        return ChangePasswordResult.invalidPassword();
                    }

                    updatePassword(connection, accountId, newPasswordHash);
                    connection.commit();
                    return ChangePasswordResult.changed();
                }
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public ChangePasswordResult recoverPassword(UUID playerUuid, String username, char[] newPassword) throws SQLException {
        String normalizedUsername = AccountNameNormalizer.normalize(username);
        PasswordHash newPasswordHash = passwordService.hash(newPassword);
        String sql = """
                SELECT id,
                       status
                  FROM economy_accounts
                 WHERE player_uuid = ?
                   AND username_normalized = ?
                   AND account_type = 'PLAYER'
                """;

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, playerUuid);
                statement.setString(2, normalizedUsername);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return ChangePasswordResult.notFound();
                    }

                    if (!AccountStatus.ACTIVE.name().equals(resultSet.getString("status"))) {
                        connection.rollback();
                        return ChangePasswordResult.inactiveAccount();
                    }

                    updatePassword(connection, resultSet.getObject("id", UUID.class), newPasswordHash);
                    connection.commit();
                    return ChangePasswordResult.changed();
                }
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public AccountPasswordVerificationResultType verifyPassword(UUID accountId, char[] password) throws SQLException {
        if (accountId == null || password == null || password.length == 0) {
            return AccountPasswordVerificationResultType.INVALID_PASSWORD;
        }

        String sql = """
                SELECT password_hash,
                       password_salt,
                       password_algorithm,
                       status
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return AccountPasswordVerificationResultType.NOT_FOUND;
                }
                if (!AccountStatus.ACTIVE.name().equals(resultSet.getString("status"))) {
                    return AccountPasswordVerificationResultType.INACTIVE_ACCOUNT;
                }

                PasswordHash storedHash = new PasswordHash(
                        resultSet.getString("password_algorithm"),
                        resultSet.getString("password_salt"),
                        resultSet.getString("password_hash")
                );
                return passwordService.verify(password, storedHash)
                        ? AccountPasswordVerificationResultType.VALID
                        : AccountPasswordVerificationResultType.INVALID_PASSWORD;
            }
        }
    }

    public AccountCreditLimitResultType updateConfiguredCreditLimit(UUID accountId, long limit) throws SQLException {
        if (accountId == null || limit < 0L) {
            return AccountCreditLimitResultType.INVALID_LIMIT;
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                AccountCreditState account = lockCreditState(connection, accountId);
                if (account == null || !AccountStatus.ACTIVE.name().equals(account.status())) {
                    connection.rollback();
                    return AccountCreditLimitResultType.INACTIVE_ACCOUNT;
                }

                long debt = br.com.economiamod.common.credit.CreditMath.debtTotal(account.principalOutstanding(), account.interestOutstanding());
                if (debt > 0L && limit > account.configuredCreditLimit()) {
                    connection.rollback();
                    return AccountCreditLimitResultType.DEBT_PRESENT;
                }

                if (limit < debt) {
                    connection.rollback();
                    return AccountCreditLimitResultType.LIMIT_BELOW_DEBT;
                }

                long reserved = reservedCardCredit(connection, accountId);
                if (limit < reserved) {
                    connection.rollback();
                    return AccountCreditLimitResultType.LIMIT_BELOW_RESERVED;
                }

                long eligibleLimit = CreditLimitPolicy.limitForBalance(account.balance());
                if (limit > eligibleLimit) {
                    connection.rollback();
                    return AccountCreditLimitResultType.LIMIT_ABOVE_ALLOWED;
                }

                updateConfiguredCreditLimit(connection, accountId, limit);
                connection.commit();
                return AccountCreditLimitResultType.UPDATED;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public AccountCreditLimitResultType requestCreditLimitByBalance(UUID accountId) throws SQLException {
        if (accountId == null) {
            return AccountCreditLimitResultType.INVALID_LIMIT;
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                AccountCreditState account = lockCreditState(connection, accountId);
                if (account == null || !AccountStatus.ACTIVE.name().equals(account.status())) {
                    connection.rollback();
                    return AccountCreditLimitResultType.INACTIVE_ACCOUNT;
                }

                long debt = br.com.economiamod.common.credit.CreditMath.debtTotal(account.principalOutstanding(), account.interestOutstanding());
                if (debt > 0L || hasLandDebt(connection, accountId)) {
                    connection.rollback();
                    return AccountCreditLimitResultType.DEBT_PRESENT;
                }

                long eligibleLimit = CreditLimitPolicy.limitForBalance(account.balance());
                if (eligibleLimit <= account.configuredCreditLimit()) {
                    connection.rollback();
                    return AccountCreditLimitResultType.NO_CREDIT_AVAILABLE;
                }

                long reserved = reservedCardCredit(connection, accountId);
                if (eligibleLimit < reserved) {
                    connection.rollback();
                    return AccountCreditLimitResultType.LIMIT_BELOW_RESERVED;
                }

                updateConfiguredCreditLimit(connection, accountId, eligibleLimit);
                connection.commit();
                return AccountCreditLimitResultType.UPDATED;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void updateLastLogin(Connection connection, UUID accountId) throws SQLException {
        String sql = "UPDATE economy_accounts SET last_login_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.executeUpdate();
        }
    }

    private void updatePassword(Connection connection, UUID accountId, PasswordHash passwordHash) throws SQLException {
        String sql = """
                UPDATE economy_accounts
                   SET password_hash = ?,
                       password_salt = ?,
                       password_algorithm = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash.hashBase64());
            statement.setString(2, passwordHash.saltBase64());
            statement.setString(3, passwordHash.algorithm());
            statement.setObject(4, accountId);
            statement.executeUpdate();
        }
    }

    private AccountCreditState lockCreditState(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT status,
                       balance,
                       configured_credit_limit,
                       credit_principal_outstanding,
                       credit_interest_outstanding
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new AccountCreditState(
                        resultSet.getString("status"),
                        resultSet.getLong("balance"),
                        resultSet.getLong("configured_credit_limit"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                );
            }
        }
    }

    private long reservedCardCredit(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN status = 'ACTIVE' THEN individual_credit_limit
                        WHEN status = 'DISABLED' AND (credit_principal_outstanding + credit_interest_outstanding) > 0
                            THEN credit_principal_outstanding + credit_interest_outstanding
                        ELSE 0
                    END
                ), 0) AS reserved
                  FROM economy_cards
                 WHERE account_id = ?
                   AND card_type IN ('CREDIT', 'DEBIT_CREDIT')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("reserved");
            }
        }
    }

    private void updateConfiguredCreditLimit(Connection connection, UUID accountId, long limit) throws SQLException {
        String sql = """
                UPDATE economy_accounts
                   SET configured_credit_limit = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, limit);
            statement.setObject(2, accountId);
            statement.executeUpdate();
        }
    }

    private boolean hasLandDebt(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                  FROM economy_accounts a
                  JOIN economy_claim_invoices i ON i.debtor_player_uuid = a.player_uuid
                 WHERE a.id = ? AND i.invoice_type = 'LAND' AND i.status = 'PENDING'
                 LIMIT 1
                """)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean playerHasAccount(Connection connection, UUID playerUuid) throws SQLException {
        String sql = "SELECT 1 FROM economy_accounts WHERE account_type = 'PLAYER' AND player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean usernameExists(Connection connection, String normalizedUsername) throws SQLException {
        String sql = "SELECT 1 FROM economy_accounts WHERE account_type = 'PLAYER' AND username_normalized = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedUsername);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String nextAccountNumber(Connection connection) throws SQLException {
        String sql = "SELECT nextval('economy_account_number_seq') AS value";
        for (int attempt = 0; attempt < 16; attempt++) {
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                String accountNumber = "%06d".formatted(resultSet.getInt("value"));
                if (!accountNumberExists(connection, accountNumber)) {
                    return accountNumber;
                }
            }
        }
        throw new SQLException("No account number available");
    }

    private boolean accountNumberExists(Connection connection, String accountNumber) throws SQLException {
        String sql = "SELECT 1 FROM economy_accounts WHERE account_type = 'PLAYER' AND account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertPlayerAccount(
            Connection connection,
            UUID playerUuid,
            String username,
            String normalizedUsername,
            String accountNumber,
            PasswordHash passwordHash
    ) throws SQLException {
        String sql = """
                INSERT INTO economy_accounts(
                    id,
                    player_uuid,
                    account_number,
                    username,
                    username_normalized,
                    password_hash,
                    password_salt,
                    password_algorithm,
                    account_type,
                    status,
                    balance,
                    configured_credit_limit,
                    credit_principal_outstanding,
                    credit_interest_outstanding,
                    created_at,
                    updated_at,
                    last_login_at,
                    version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 1)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, playerUuid);
            statement.setString(3, accountNumber);
            statement.setString(4, username);
            statement.setString(5, normalizedUsername);
            statement.setString(6, passwordHash.hashBase64());
            statement.setString(7, passwordHash.saltBase64());
            statement.setString(8, passwordHash.algorithm());
            statement.setString(9, AccountType.PLAYER.name());
            statement.setString(10, AccountStatus.ACTIVE.name());
            statement.executeUpdate();
        }
    }

    private record AccountCreditState(
            String status,
            long balance,
            long configuredCreditLimit,
            long principalOutstanding,
            long interestOutstanding
    ) {
    }
}
