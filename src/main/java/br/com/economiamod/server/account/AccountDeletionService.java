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

                int affectedRows = purgeAccountData(connection, target);
                auditLogService.recordAdminChange(
                        connection,
                        adminPlayerUuid,
                        "ACCOUNT_DELETE",
                        "ACCOUNT",
                        target.accountId(),
                        target.username() + " (" + target.accountNumber() + ")",
                        "DELETED"
                );
                connection.commit();

                if (target.playerUuid() != null) {
                    BankSessionService.INSTANCE.logout(target.playerUuid(), target.accountId());
                }
                return AccountDeletionResult.deleted(target, affectedRows);
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
                       account_number
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
                        resultSet.getString("account_number")
                );
            }
        }
    }

    private int purgeAccountData(Connection connection, AccountDeletionTarget target) throws SQLException {
        int affectedRows = 0;
        affectedRows += detachCommercialBlocks(connection, target.accountId());
        affectedRows += deletePreviousAuditLogs(connection, target);
        affectedRows += deleteGoldExchangeEntries(connection, target);
        affectedRows += deleteCardEntries(connection, target.accountId());
        affectedRows += deleteInterestAccruals(connection, target.accountId());
        affectedRows += deleteLedgerEntries(connection, target.accountId());
        affectedRows += detachTransactions(connection, target.accountId());
        affectedRows += deleteCards(connection, target.accountId());
        affectedRows += deleteAccount(connection, target.accountId());
        return affectedRows;
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

    private int deletePreviousAuditLogs(Connection connection, AccountDeletionTarget target) throws SQLException {
        String sql = """
                DELETE FROM economy_audit_logs
                 WHERE target_type = 'ACCOUNT'
                   AND target_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, target.accountId());
            return statement.executeUpdate();
        }
    }

    private int deleteGoldExchangeEntries(Connection connection, AccountDeletionTarget target) throws SQLException {
        return deleteGoldExchangeEntriesByAccountTransactions(connection, target.accountId());
    }

    private int deleteGoldExchangeEntriesByAccountTransactions(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                DELETE FROM economy_gold_exchange_entries
                 WHERE transaction_id IN (
                        SELECT id
                          FROM economy_transactions
                         WHERE source_account_id = ?
                            OR destination_account_id = ?
                            OR card_id IN (SELECT id FROM economy_cards WHERE account_id = ?)
                    )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setObject(2, accountId);
            statement.setObject(3, accountId);
            return statement.executeUpdate();
        }
    }

    private int deleteCardEntries(Connection connection, UUID accountId) throws SQLException {
        String sql = "DELETE FROM economy_card_entries WHERE card_id IN (SELECT id FROM economy_cards WHERE account_id = ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            return statement.executeUpdate();
        }
    }

    private int deleteInterestAccruals(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                DELETE FROM economy_interest_accruals
                 WHERE account_id = ?
                    OR card_id IN (SELECT id FROM economy_cards WHERE account_id = ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setObject(2, accountId);
            return statement.executeUpdate();
        }
    }

    private int deleteLedgerEntries(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM economy_ledger_entries WHERE account_id = ?")) {
            statement.setObject(1, accountId);
            return statement.executeUpdate();
        }
    }

    private int detachTransactions(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                UPDATE economy_transactions
                   SET source_account_id = CASE WHEN source_account_id = ? THEN NULL ELSE source_account_id END,
                       destination_account_id = CASE WHEN destination_account_id = ? THEN NULL ELSE destination_account_id END,
                       card_id = CASE
                           WHEN card_id IN (SELECT id FROM economy_cards WHERE account_id = ?) THEN NULL
                           ELSE card_id
                       END
                 WHERE source_account_id = ?
                    OR destination_account_id = ?
                    OR card_id IN (SELECT id FROM economy_cards WHERE account_id = ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setObject(2, accountId);
            statement.setObject(3, accountId);
            statement.setObject(4, accountId);
            statement.setObject(5, accountId);
            statement.setObject(6, accountId);
            return statement.executeUpdate();
        }
    }

    private int deleteCards(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM economy_cards WHERE account_id = ?")) {
            statement.setObject(1, accountId);
            return statement.executeUpdate();
        }
    }

    private int deleteAccount(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM economy_accounts WHERE id = ?")) {
            statement.setObject(1, accountId);
            return statement.executeUpdate();
        }
    }
}
