package br.com.economiamod.server.transaction;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.credit.CreditMath;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class AccountFinancialService {
    private final AccountFinancialRepository repository;
    private final PaymentAccountRepository paymentRepository = new PaymentAccountRepository();
    private final PaymentTransactionWriter paymentTransactionWriter = new PaymentTransactionWriter();
    private final TransactionIdempotencyService idempotencyService = new TransactionIdempotencyService();
    private final AccountTransactionWriter transactionWriter;

    public AccountFinancialService() {
        this(new AccountFinancialRepository(), new AccountTransactionWriter());
    }

    AccountFinancialService(AccountFinancialRepository repository, AccountTransactionWriter transactionWriter) {
        this.repository = repository;
        this.transactionWriter = transactionWriter;
    }

    public FinancialOperationResult deposit(UUID playerUuid, UUID accountId, long amount, String idempotencyKey) throws SQLException {
        return deposit(playerUuid, accountId, amount, idempotencyKey, TransactionOrigin.MINECRAFT);
    }

    public FinancialOperationResult deposit(UUID playerUuid, UUID accountId, long amount, String idempotencyKey,
                                            TransactionOrigin origin) throws SQLException {
        requirePositive(amount);
        return creditBalance(playerUuid, accountId, amount, IdempotencyKeys.requireValid(idempotencyKey),
                EconomyTransactionType.DEPOSIT, origin);
    }

    public FinancialOperationResult withdraw(UUID playerUuid, UUID accountId, long amount, String idempotencyKey) throws SQLException {
        return withdraw(playerUuid, accountId, amount, idempotencyKey, TransactionOrigin.MINECRAFT);
    }

    public FinancialOperationResult withdraw(UUID playerUuid, UUID accountId, long amount, String idempotencyKey,
                                             TransactionOrigin origin) throws SQLException {
        requirePositive(amount);
        String key = IdempotencyKeys.requireValid(idempotencyKey);
        String fingerprint = RequestFingerprint.of(EconomyTransactionType.WITHDRAW, playerUuid, accountId, amount);

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                IdempotencyCheck check = idempotencyService.check(connection, key, fingerprint);
                if (check == IdempotencyCheck.MATCH) {
                    Optional<FinancialOperationResult> duplicate = transactionWriter.findCompletedTransaction(connection, key);
                    connection.commit();
                    return duplicate.orElseGet(() -> FinancialOperationResult.duplicate(null, 0L));
                }
                if (check == IdempotencyCheck.CONFLICT) {
                    connection.rollback();
                    return FinancialOperationResult.idempotencyConflict();
                }

                AccountFinancialSnapshot account = repository.lockPlayerAccount(connection, accountId).orElse(null);
                if (!active(account)) {
                    connection.rollback();
                    return FinancialOperationResult.inactiveAccount();
                }

                long available = CreditMath.availableBalance(account.balance(), account.principalOutstanding(), account.interestOutstanding());
                if (available < amount) {
                    connection.rollback();
                    return FinancialOperationResult.insufficientBalance();
                }

                UUID transactionId = UUID.randomUUID();
                long balanceAfter = account.balance() - amount;
                repository.updateBalance(connection, accountId, balanceAfter);
                transactionWriter.insertTransaction(connection, transactionId, key, EconomyTransactionType.WITHDRAW,
                        amount, playerUuid, accountId, null, fingerprint, origin);
                transactionWriter.insertLedger(connection, transactionId, accountId, LedgerEntryType.DEBIT,
                        amount, account.balance(), balanceAfter);
                connection.commit();
                return FinancialOperationResult.completed(transactionId, balanceAfter);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public FinancialOperationResult transfer(UUID playerUuid, UUID sourceAccountId, UUID destinationAccountId,
                                             long amount, UUID cardId, String idempotencyKey) throws SQLException {
        return transfer(playerUuid, sourceAccountId, destinationAccountId, amount, cardId, idempotencyKey,
                TransactionOrigin.MINECRAFT);
    }

    public FinancialOperationResult transfer(UUID playerUuid, UUID sourceAccountId, UUID destinationAccountId,
                                             long amount, UUID cardId, String idempotencyKey,
                                             TransactionOrigin origin) throws SQLException {
        requirePositive(amount);
        String key = IdempotencyKeys.requireValid(idempotencyKey);
        EconomyTransactionType type = cardId == null ? EconomyTransactionType.TRANSFER : EconomyTransactionType.DEBIT_PURCHASE;
        String fingerprint = RequestFingerprint.of(type, playerUuid, sourceAccountId, destinationAccountId, amount, cardId);

        if (sourceAccountId.equals(destinationAccountId)) {
            return FinancialOperationResult.completed(null, 0L);
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                IdempotencyCheck check = paymentTransactionWriter.idempotencyCheck(connection, key, fingerprint);
                if (check == IdempotencyCheck.MATCH) {
                    connection.commit();
                    return FinancialOperationResult.duplicate(null, 0L);
                }
                if (check == IdempotencyCheck.CONFLICT) {
                    connection.rollback();
                    return FinancialOperationResult.idempotencyConflict();
                }

                paymentRepository.lockAccountsOrdered(connection, sourceAccountId, destinationAccountId);
                PaymentAccountSnapshot source = paymentRepository.findPaymentAccount(connection, sourceAccountId).orElse(null);
                PaymentAccountSnapshot destination = paymentRepository.findPaymentAccount(connection, destinationAccountId).orElse(null);
                if (!active(source) || !active(destination)) {
                    connection.rollback();
                    return FinancialOperationResult.inactiveAccount();
                }

                long available = CreditMath.availableBalance(source.balance(), source.principalOutstanding(), source.interestOutstanding());
                if (available < amount) {
                    connection.rollback();
                    return FinancialOperationResult.insufficientBalance();
                }

                UUID transactionId = UUID.randomUUID();
                long sourceAfter = source.balance() - amount;
                long destinationAfter = Math.addExact(destination.balance(), amount);
                paymentRepository.updateBalance(connection, sourceAccountId, sourceAfter);
                paymentRepository.updateBalance(connection, destinationAccountId, destinationAfter);
                if (cardId == null) {
                    paymentTransactionWriter.insertTransferTransaction(connection, transactionId, key, amount,
                            playerUuid, sourceAccountId, destinationAccountId, fingerprint, origin);
                } else {
                    paymentTransactionWriter.insertDebitTransaction(connection, transactionId, key, amount,
                            playerUuid, sourceAccountId, destinationAccountId, cardId, fingerprint, origin);
                }
                paymentTransactionWriter.insertLedger(connection, transactionId, sourceAccountId, LedgerEntryType.DEBIT,
                        amount, source.balance(), sourceAfter);
                paymentTransactionWriter.insertLedger(connection, transactionId, destinationAccountId, LedgerEntryType.CREDIT,
                        amount, destination.balance(), destinationAfter);
                connection.commit();
                return FinancialOperationResult.completed(transactionId, destinationAfter);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private FinancialOperationResult creditBalance(UUID playerUuid, UUID accountId, long amount,
                                                   String idempotencyKey, EconomyTransactionType transactionType,
                                                   TransactionOrigin origin) throws SQLException {
        String fingerprint = RequestFingerprint.of(transactionType, playerUuid, accountId, amount);
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                IdempotencyCheck check = idempotencyService.check(connection, idempotencyKey, fingerprint);
                if (check == IdempotencyCheck.MATCH) {
                    Optional<FinancialOperationResult> duplicate = transactionWriter.findCompletedTransaction(connection, idempotencyKey);
                    connection.commit();
                    return duplicate.orElseGet(() -> FinancialOperationResult.duplicate(null, 0L));
                }
                if (check == IdempotencyCheck.CONFLICT) {
                    connection.rollback();
                    return FinancialOperationResult.idempotencyConflict();
                }

                AccountFinancialSnapshot account = repository.lockPlayerAccount(connection, accountId).orElse(null);
                if (!active(account)) {
                    connection.rollback();
                    return FinancialOperationResult.inactiveAccount();
                }

                UUID transactionId = UUID.randomUUID();
                long balanceAfter = Math.addExact(account.balance(), amount);
                repository.updateBalance(connection, accountId, balanceAfter);
                transactionWriter.insertTransaction(connection, transactionId, idempotencyKey, transactionType,
                        amount, playerUuid, null, accountId, fingerprint, origin);
                transactionWriter.insertLedger(connection, transactionId, accountId, LedgerEntryType.CREDIT,
                        amount, account.balance(), balanceAfter);
                connection.commit();
                return FinancialOperationResult.completed(transactionId, balanceAfter);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private boolean active(AccountFinancialSnapshot account) {
        return account != null && AccountStatus.ACTIVE.name().equals(account.status());
    }

    private boolean active(PaymentAccountSnapshot account) {
        return account != null && AccountStatus.ACTIVE.name().equals(account.status());
    }

    private void requirePositive(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
