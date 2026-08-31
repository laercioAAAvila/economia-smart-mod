package br.com.economiamod.server.account;

import br.com.economiamod.common.account.AccountNameNormalizer;
import br.com.economiamod.server.audit.AuditLogService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.session.BankSessionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Administrative account close. Financial history is deliberately retained: deleting ledger rows,
 * transactions or cards would destroy the accounting trail and can hide/lose player assets.
 */
public final class AccountDeletionService {
    private final AuditLogService auditLogService = new AuditLogService();

    public AccountDeletionResult deletePlayerAccount(UUID adminPlayerUuid, String username) throws SQLException {
        String normalizedUsername = AccountNameNormalizer.normalize(username);

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                AccountDeletionTarget target = lockTargetAccount(connection, normalizedUsername);
                if (target == null) {
                    connection.rollback();
                    return AccountDeletionResult.notFound();
                }
                if ("CLOSED".equals(target.status())) {
                    connection.rollback();
                    return AccountDeletionResult.of(AccountDeletionResultType.ALREADY_CLOSED, target, 0);
                }
                if (target.hasFundsOrDebt()) {
                    connection.rollback();
                    return AccountDeletionResult.of(AccountDeletionResultType.HAS_BALANCE_OR_DEBT, target, 0);
                }

                int affectedRows = 0;
                affectedRows += detachCommercialBlocks(connection, target.accountId());
                affectedRows += disableCards(connection, target.accountId());
                affectedRows += closeAccount(connection, target.accountId());
                auditLogService.recordAdminChange(
                        connection,
                        adminPlayerUuid,
                        "ACCOUNT_CLOSE",
                        "ACCOUNT",
                        target.accountId(),
                        target.username() + " (" + target.accountNumber() + ")",
                        "CLOSED"
                );
                connection.commit();

                if (target.playerUuid() != null) {
                    BankSessionService.INSTANCE.logout(target.playerUuid(), target.accountId());
                }
                return AccountDeletionResult.of(AccountDeletionResultType.CLOSED, target, affectedRows);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private AccountDeletionTarget lockTargetAccount(Connection connection, String normalizedUsername) throws SQLException {
        String sql = """
                SELECT id,
                       player_uuid,
                       username,
                       account_number,
                       status,
                       balance,
                       credit_principal_outstanding,
                       credit_interest_outstanding
                  FROM economy_accounts
                 WHERE account_type = 'PLAYER'
                   AND server_uuid = ?
                   AND username_normalized = ?
                 FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, BankServerIdentityService.INSTANCE.current());
            statement.setString(2, normalizedUsername);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new AccountDeletionTarget(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("player_uuid", UUID.class),
                        resultSet.getString("username"),
                        resultSet.getString("account_number"),
                        resultSet.getString("status"),
                        resultSet.getLong("balance"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                );
            }
        }
    }

    private int detachCommercialBlocks(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                UPDATE economy_commercial_blocks
                   SET linked_account_id = CASE WHEN linked_account_id = ? THEN NULL ELSE linked_account_id END,
                       funding_card_id = CASE
                           WHEN funding_card_id IN (SELECT id FROM economy_cards WHERE account_id = ?) THEN NULL
                           ELSE funding_card_id
                       END,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE linked_account_id = ?
                    OR funding_card_id IN (SELECT id FROM economy_cards WHERE account_id = ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setObject(2, accountId);
            statement.setObject(3, accountId);
            statement.setObject(4, accountId);
            return statement.executeUpdate();
        }
    }

    private int disableCards(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE economy_cards
                   SET status = 'DISABLED', disabled_at = CURRENT_TIMESTAMP,
                       updated_at = CURRENT_TIMESTAMP, security_version = security_version + 1
                 WHERE account_id = ? AND status <> 'DISABLED'
                """)) {
            statement.setObject(1, accountId);
            return statement.executeUpdate();
        }
    }

    private int closeAccount(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE economy_accounts
                   SET status = 'CLOSED', updated_at = CURRENT_TIMESTAMP, version = version + 1
                 WHERE id = ? AND status <> 'CLOSED'
                """)) {
            statement.setObject(1, accountId);
            return statement.executeUpdate();
        }
    }
}
