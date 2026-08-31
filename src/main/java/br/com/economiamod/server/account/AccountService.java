package br.com.economiamod.server.account;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.account.AccountNameNormalizer;
import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.account.AccountType;
import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.DatabaseEngine;
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

    public CreateAccountResult createPlayerAccount(UUID playerUuid, String playerName,
                                                   String username, char[] password,
                                                   UUID requestId) throws SQLException {
        String normalizedUsername = AccountNameNormalizer.normalize(username);
        PasswordHash passwordHash = passwordService.hash(password);
        UUID serverUuid = BankServerIdentityService.INSTANCE.current();

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                lockPlayerAccountScope(connection, serverUuid, playerUuid);
                PendingAccount existing = pendingAccount(connection, serverUuid, playerUuid,
                        normalizedUsername, requestId);
                if (existing != null) {
                    if (!existing.active()) {
                        updatePendingPassword(connection, existing.accountId(), playerName, passwordHash, requestId);
                    }
                    connection.commit();
                    return CreateAccountResult.created(existing.accountId(), existing.openingFee(), existing.active());
                }
                if (playerAccountCount(connection, serverUuid, playerUuid)
                        >= EconomyServerConfig.BANK_MAX_ACCOUNTS_PER_PLAYER.get()) {
                    connection.rollback();
                    return CreateAccountResult.playerAlreadyHasAccount();
                }

                if (usernameExists(connection, serverUuid, normalizedUsername)) {
                    connection.rollback();
                    return CreateAccountResult.usernameAlreadyUsed();
                }

                UUID accountId = UUID.randomUUID();
                long openingFee = EconomyServerConfig.BANK_ACCOUNT_OPENING_FEE.get();
                insertPlayerAccount(connection, accountId, serverUuid, playerUuid, playerName,
                        username.trim(), normalizedUsername, nextAccountNumber(connection), passwordHash,
                        openingFee, requestId);
                connection.commit();
                return CreateAccountResult.created(accountId, openingFee, false);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public void activatePendingAccount(UUID accountId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_accounts
                        SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP, version = version + 1
                      WHERE id = ? AND server_uuid = ? AND account_type = 'PLAYER' AND status = 'PENDING'
                     """)) {
            statement.setObject(1, accountId);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
            statement.executeUpdate();
        }
    }

    public void deletePendingAccount(UUID accountId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM economy_accounts
                      WHERE id = ? AND server_uuid = ? AND account_type = 'PLAYER' AND status = 'PENDING'
                     """)) {
            statement.setObject(1, accountId);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
            statement.executeUpdate();
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
                   AND server_uuid = ?
                   AND username_normalized = ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, BankServerIdentityService.INSTANCE.current());
            statement.setString(2, normalizedUsername);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return AuthenticateAccountResult.notFound();
                }

                UUID accountId = resultSet.getObject("id", UUID.class);
                if (!AccountStatus.ACTIVE.name().equals(resultSet.getString("status"))) {
                    return AuthenticateAccountResult.inactiveAccount(accountId);
                }

                PasswordHash storedHash = new PasswordHash(
                        resultSet.getString("password_algorithm"),
                        resultSet.getString("password_salt"),
                        resultSet.getString("password_hash")
                );

                if (!passwordService.verify(password, storedHash)) {
                    return AuthenticateAccountResult.invalidPassword(accountId);
                }

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

    public ChangePasswordResult recoverPassword(UUID playerUuid, String playerName,
                                                String username, char[] newPassword) throws SQLException {
        String normalizedUsername = AccountNameNormalizer.normalize(username);
        PasswordHash newPasswordHash = passwordService.hash(newPassword);
        String sql = """
                SELECT id,
                       status
                  FROM economy_accounts
                 WHERE player_uuid = ?
                   AND server_uuid = ?
                   AND account_type = 'PLAYER'
                   AND username_normalized = ?
                """;

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, playerUuid);
                statement.setObject(2, BankServerIdentityService.INSTANCE.current());
                statement.setString(3, normalizedUsername);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        UUID existingAccountId = firstPlayerAccountId(connection, playerUuid);
                        if (existingAccountId != null) {
                            EconomiaMod.LOGGER.warn("Recuperação de senha recusada: usuário bancário não corresponde; accountUuid={}.", existingAccountId);
                            connection.rollback();
                            return ChangePasswordResult.usernameMismatch();
                        }
                        EconomiaMod.LOGGER.warn("Recuperação de senha recusada: nenhuma conta vinculada à identidade Minecraft atual neste servidor.");
                        connection.rollback();
                        return ChangePasswordResult.notFound();
                    }

                    UUID accountId = resultSet.getObject("id", UUID.class);
                    if (!AccountStatus.ACTIVE.name().equals(resultSet.getString("status"))) {
                        EconomiaMod.LOGGER.warn("Recuperação de senha recusada: conta inativa; accountUuid={}.", accountId);
                        connection.rollback();
                        return ChangePasswordResult.inactiveAccount();
                    }

                    updatePasswordAndPlayerName(connection, accountId, playerName, newPasswordHash);
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

    private UUID firstPlayerAccountId(Connection connection, UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id FROM economy_accounts
                 WHERE account_type = 'PLAYER' AND server_uuid = ? AND player_uuid = ?
                   AND status <> 'CLOSED'
                 ORDER BY created_at, id
                 LIMIT 1
                """)) {
            statement.setObject(1, BankServerIdentityService.INSTANCE.current());
            statement.setObject(2, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject("id", UUID.class) : null;
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

    private void updatePasswordAndPlayerName(Connection connection, UUID accountId, String playerName,
                                             PasswordHash passwordHash) throws SQLException {
        String sql = """
                UPDATE economy_accounts
                   SET minecraft_player_name = ?,
                       password_hash = ?,
                       password_salt = ?,
                       password_algorithm = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setString(2, passwordHash.hashBase64());
            statement.setString(3, passwordHash.saltBase64());
            statement.setString(4, passwordHash.algorithm());
            statement.setObject(5, accountId);
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

    private int playerAccountCount(Connection connection, UUID serverUuid, UUID playerUuid) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM economy_accounts
                 WHERE account_type = 'PLAYER' AND server_uuid = ? AND player_uuid = ? AND status <> 'CLOSED'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, serverUuid);
            statement.setObject(2, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private boolean usernameExists(Connection connection, UUID serverUuid, String normalizedUsername) throws SQLException {
        String sql = """
                SELECT 1 FROM economy_accounts
                 WHERE account_type = 'PLAYER' AND server_uuid = ? AND username_normalized = ? AND status <> 'CLOSED'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, serverUuid);
            statement.setString(2, normalizedUsername);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String nextAccountNumber(Connection connection) throws SQLException {
        if (EconomyDatabase.engine() == DatabaseEngine.SQLITE) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COALESCE(MAX(CAST(account_number AS INTEGER)), 0) + 1 AS value
                      FROM economy_accounts
                     WHERE account_type = 'PLAYER'
                       AND account_number IS NOT NULL
                    """);
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                int value = resultSet.getInt("value");
                if (value < 1 || value > 999999) {
                    throw new SQLException("No account number available");
                }
                return "%06d".formatted(value);
            }
        }

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
            UUID accountId,
            UUID serverUuid,
            UUID playerUuid,
            String playerName,
            String username,
            String normalizedUsername,
            String accountNumber,
            PasswordHash passwordHash,
            long openingFee,
            UUID requestId
    ) throws SQLException {
        String sql = """
                INSERT INTO economy_accounts(
                    id,
                    server_uuid,
                    player_uuid,
                    minecraft_player_name,
                    account_number,
                    username,
                    username_normalized,
                    password_hash,
                    password_salt,
                    password_algorithm,
                    account_type,
                    status,
                    opening_fee,
                    opening_request_id,
                    balance,
                    configured_credit_limit,
                    credit_principal_outstanding,
                    credit_interest_outstanding,
                    created_at,
                    updated_at,
                    last_login_at,
                    version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 1)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setObject(2, serverUuid);
            statement.setObject(3, playerUuid);
            statement.setString(4, playerName);
            statement.setString(5, accountNumber);
            statement.setString(6, username);
            statement.setString(7, normalizedUsername);
            statement.setString(8, passwordHash.hashBase64());
            statement.setString(9, passwordHash.saltBase64());
            statement.setString(10, passwordHash.algorithm());
            statement.setString(11, AccountType.PLAYER.name());
            statement.setString(12, "PENDING");
            statement.setLong(13, openingFee);
            statement.setObject(14, requestId);
            statement.executeUpdate();
        }
    }

    private void lockPlayerAccountScope(Connection connection, UUID serverUuid, UUID playerUuid) throws SQLException {
        if (EconomyDatabase.engine() == DatabaseEngine.SQLITE) {
            // SQLite transactions are opened in IMMEDIATE mode; the single writer lock
            // serializes account creation without requiring a database-specific advisory lock.
            return;
        }
        long lockKey = serverUuid.getMostSignificantBits() ^ serverUuid.getLeastSignificantBits()
                ^ playerUuid.getMostSignificantBits() ^ playerUuid.getLeastSignificantBits();
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, lockKey);
            statement.executeQuery().close();
        }
    }

    private PendingAccount pendingAccount(Connection connection, UUID serverUuid, UUID playerUuid,
                                          String normalizedUsername, UUID requestId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, opening_fee, status
                  FROM economy_accounts
                 WHERE account_type = 'PLAYER' AND server_uuid = ? AND player_uuid = ?
                   AND (opening_request_id = ? OR (username_normalized = ? AND status = 'PENDING'))
                 ORDER BY CASE WHEN opening_request_id = ? THEN 0 ELSE 1 END
                 LIMIT 1 FOR UPDATE
                """)) {
            statement.setObject(1, serverUuid);
            statement.setObject(2, playerUuid);
            statement.setObject(3, requestId);
            statement.setString(4, normalizedUsername);
            statement.setObject(5, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new PendingAccount(resultSet.getObject("id", UUID.class),
                        resultSet.getLong("opening_fee"), "ACTIVE".equals(resultSet.getString("status"))) : null;
            }
        }
    }

    private void updatePendingPassword(Connection connection, UUID accountId, String playerName,
                                       PasswordHash passwordHash, UUID requestId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE economy_accounts
                   SET minecraft_player_name = ?, password_hash = ?, password_salt = ?, password_algorithm = ?,
                       opening_request_id = ?, updated_at = CURRENT_TIMESTAMP, version = version + 1
                 WHERE id = ? AND status = 'PENDING'
                """)) {
            statement.setString(1, playerName);
            statement.setString(2, passwordHash.hashBase64());
            statement.setString(3, passwordHash.saltBase64());
            statement.setString(4, passwordHash.algorithm());
            statement.setObject(5, requestId);
            statement.setObject(6, accountId);
            statement.executeUpdate();
        }
    }

    private record PendingAccount(UUID accountId, long openingFee, boolean active) {
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
