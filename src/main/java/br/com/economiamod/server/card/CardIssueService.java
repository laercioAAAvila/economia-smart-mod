package br.com.economiamod.server.card;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.card.CardStatus;
import br.com.economiamod.common.card.CardType;
import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.common.credit.CreditMath;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.AccountTransactionWriter;
import br.com.economiamod.server.transaction.EconomyTransactionStatus;
import br.com.economiamod.server.transaction.EconomyTransactionType;
import br.com.economiamod.server.transaction.IdempotencyCheck;
import br.com.economiamod.server.transaction.IdempotencyKeys;
import br.com.economiamod.server.transaction.LedgerEntryType;
import br.com.economiamod.server.transaction.RequestFingerprint;
import br.com.economiamod.server.transaction.TransactionIdempotencyService;
import br.com.economiamod.server.transaction.TransactionOrigin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class CardIssueService {
    private static final int DEBIT_LIMIT = 3;
    private static final int CREDIT_LIMIT = 3;
    private static final int DEBIT_CREDIT_LIMIT = 1;
    private static final int INITIAL_SECURITY_VERSION = 1;
    private static final DateTimeFormatter CARD_NAME_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final TransactionIdempotencyService idempotencyService = new TransactionIdempotencyService();
    private final AccountTransactionWriter transactionWriter = new AccountTransactionWriter();

    public CardIssueResult issue(CardIssueRequest request) throws SQLException {
        validateRequest(request);
        UUID playerUuid = request.playerUuid() == null ? new UUID(0L, 0L) : request.playerUuid();
        String key = IdempotencyKeys.requireValid("card-issue:" + request.accountId() + ":" + request.requestId());
        String fingerprint = RequestFingerprint.of(EconomyTransactionType.CARD_ISSUE, playerUuid,
                request.accountId(), request.cardType(), request.customName(), request.individualCreditLimit());

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                IdempotencyCheck idempotency = idempotencyService.check(connection, key, fingerprint);
                if (idempotency == IdempotencyCheck.MATCH) {
                    CardIssueResult duplicate = completedIssue(connection, key);
                    connection.commit();
                    return duplicate == null ? CardIssueResult.idempotencyConflict() : duplicate;
                }
                if (idempotency == IdempotencyCheck.CONFLICT) {
                    connection.rollback();
                    return CardIssueResult.idempotencyConflict();
                }

                AccountCreditState account = lockAccount(connection, request.accountId());
                if (account == null || !AccountStatus.ACTIVE.name().equals(account.status())) {
                    connection.rollback();
                    return CardIssueResult.inactiveAccount();
                }

                if (activeCardsOfType(connection, request.accountId(), request.cardType()) >= maxActiveCards(request.cardType())) {
                    connection.rollback();
                    return CardIssueResult.cardLimitReached();
                }

                long effectiveCreditLimit = CreditLimitPolicy.effectiveLimit(account.balance(), account.configuredCreditLimit());
                if (request.cardType().hasCredit() && !hasCreditReservation(connection, request, effectiveCreditLimit)) {
                    connection.rollback();
                    return CardIssueResult.creditLimitUnavailable();
                }

                long issueFee = EconomyServerConfig.BANK_CARD_ISSUE_FEE.get();
                if (issueFee > 0L && CreditMath.availableBalance(account.balance(), account.principalOutstanding(), account.interestOutstanding()) < issueFee) {
                    connection.rollback();
                    return CardIssueResult.insufficientBalance();
                }

                UUID cardId = UUID.randomUUID();
                int creationNumber = nextCardCreationNumber(connection, request.accountId());
                String cardName = generatedCardName(request.customName(), creationNumber);
                insertCard(connection, request, cardId, creationNumber, cardName);
                long balanceAfter = account.balance();
                if (issueFee > 0L) {
                    balanceAfter = account.balance() - issueFee;
                    updateAccountBalance(connection, request.accountId(), balanceAfter);
                }
                UUID transactionId = UUID.randomUUID();
                insertIssueTransaction(connection, transactionId, key, fingerprint, playerUuid, request.accountId(), cardId, issueFee);
                if (issueFee > 0L) {
                    transactionWriter.insertLedger(connection, transactionId, request.accountId(), LedgerEntryType.DEBIT,
                            issueFee, account.balance(), balanceAfter);
                }
                connection.commit();
                return CardIssueResult.issued(cardId, request.cardType(), INITIAL_SECURITY_VERSION, account.accountNumber(), cardName, request.individualCreditLimit());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void validateRequest(CardIssueRequest request) {
        if (request.accountId() == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (request.cardType() == null) {
            throw new IllegalArgumentException("cardType is required");
        }
        if (!request.cardType().hasCredit() && request.individualCreditLimit() != 0L) {
            throw new IllegalArgumentException("debit cards cannot have credit limit");
        }
        if (request.cardType().hasCredit() && request.individualCreditLimit() < 0L) {
            throw new IllegalArgumentException("individualCreditLimit cannot be negative");
        }
        if (request.customName() != null && request.customName().length() > 32) {
            throw new IllegalArgumentException("customName cannot exceed 32 characters");
        }
        if (request.requestId() == null) {
            throw new IllegalArgumentException("requestId is required");
        }
    }

    private AccountCreditState lockAccount(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT status,
                       account_number,
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
                        resultSet.getString("account_number"),
                        resultSet.getLong("balance"),
                        resultSet.getLong("configured_credit_limit"),
                        resultSet.getLong("credit_principal_outstanding"),
                        resultSet.getLong("credit_interest_outstanding")
                );
            }
        }
    }

    private int activeCardsOfType(Connection connection, UUID accountId, CardType cardType) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total
                  FROM economy_cards
                 WHERE account_id = ?
                   AND card_type = ?
                   AND status = 'ACTIVE'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setString(2, cardType.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt("total");
            }
        }
    }

    private boolean hasCreditReservation(Connection connection, CardIssueRequest request, long configuredCreditLimit) throws SQLException {
        if (request.individualCreditLimit() > configuredCreditLimit) {
            return false;
        }

        long reserved = reservedCredit(connection, request.accountId());
        return reserved + request.individualCreditLimit() <= configuredCreditLimit;
    }

    private long reservedCredit(Connection connection, UUID accountId) throws SQLException {
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

    private int nextCardCreationNumber(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(card_creation_number), 0) + 1 AS next_number
                  FROM economy_cards
                 WHERE account_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt("next_number");
            }
        }
    }

    private String generatedCardName(String customName, int creationNumber) {
        String normalized = normalizedCustomName(customName);
        if (normalized != null) {
            return normalized;
        }
        LocalDate today = LocalDate.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
        return creationNumber + "-" + CARD_NAME_DATE.format(today);
    }

    private void insertCard(Connection connection, CardIssueRequest request, UUID cardId, int creationNumber, String cardName) throws SQLException {
        String sql = """
                INSERT INTO economy_cards(
                    id,
                    account_id,
                    card_type,
                    custom_name,
                    card_creation_number,
                    status,
                    individual_credit_limit,
                    credit_principal_outstanding,
                    credit_interest_outstanding,
                    interest_rounding_remainder,
                    security_version,
                    created_at,
                    updated_at,
                    disabled_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            statement.setObject(2, request.accountId());
            statement.setString(3, request.cardType().name());
            statement.setString(4, cardName);
            statement.setInt(5, creationNumber);
            statement.setString(6, CardStatus.ACTIVE.name());
            statement.setLong(7, request.individualCreditLimit());
            statement.setInt(8, INITIAL_SECURITY_VERSION);
            statement.executeUpdate();
        }
    }

    private void updateAccountBalance(Connection connection, UUID accountId, long balanceAfter) throws SQLException {
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

    private void insertIssueTransaction(Connection connection, UUID transactionId, String key, String fingerprint,
                                        UUID playerUuid, UUID accountId, UUID cardId, long issueFee) throws SQLException {
        String sql = """
                INSERT INTO economy_transactions(
                    id, idempotency_key, transaction_type, status, amount, initiator_player_uuid,
                    source_account_id, card_id, request_fingerprint, origin, created_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transactionId);
            statement.setString(2, key);
            statement.setString(3, EconomyTransactionType.CARD_ISSUE.name());
            statement.setString(4, EconomyTransactionStatus.COMPLETED.name());
            statement.setLong(5, issueFee);
            statement.setObject(6, playerUuid);
            statement.setObject(7, accountId);
            statement.setObject(8, cardId);
            statement.setString(9, fingerprint);
            statement.setString(10, TransactionOrigin.MINECRAFT.name());
            statement.executeUpdate();
        }
    }

    private CardIssueResult completedIssue(Connection connection, String key) throws SQLException {
        String sql = """
                SELECT c.id, c.card_type, c.security_version, c.custom_name, c.individual_credit_limit,
                       a.account_number
                  FROM economy_transactions t
                  JOIN economy_cards c ON c.id = t.card_id
                  JOIN economy_accounts a ON a.id = c.account_id
                 WHERE t.idempotency_key = ? AND t.status = 'COMPLETED' AND t.transaction_type = 'CARD_ISSUE'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return CardIssueResult.duplicateIssued(
                        resultSet.getObject("id", UUID.class),
                        CardType.valueOf(resultSet.getString("card_type")),
                        resultSet.getInt("security_version"),
                        resultSet.getString("account_number"),
                        resultSet.getString("custom_name"),
                        resultSet.getLong("individual_credit_limit")
                );
            }
        }
    }

    private int maxActiveCards(CardType cardType) {
        return switch (cardType) {
            case DEBIT -> DEBIT_LIMIT;
            case CREDIT -> CREDIT_LIMIT;
            case DEBIT_CREDIT -> DEBIT_CREDIT_LIMIT;
        };
    }

    private String normalizedCustomName(String customName) {
        if (customName == null || customName.isBlank()) {
            return null;
        }
        return customName.trim();
    }

    private record AccountCreditState(
            String status,
            String accountNumber,
            long balance,
            long configuredCreditLimit,
            long principalOutstanding,
            long interestOutstanding
    ) {
    }
}
