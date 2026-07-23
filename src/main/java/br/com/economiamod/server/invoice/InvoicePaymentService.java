package br.com.economiamod.server.invoice;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.LedgerEntryType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InvoicePaymentService {
    private final InvoicePaymentRepository repository;
    private final InvoicePaymentWriter writer;

    public InvoicePaymentService() {
        this(new InvoicePaymentRepository(), new InvoicePaymentWriter());
    }

    InvoicePaymentService(InvoicePaymentRepository repository, InvoicePaymentWriter writer) {
        this.repository = repository;
        this.writer = writer;
    }

    public InvoicePaymentResult payAccount(UUID playerUuid, UUID accountId, long requestedAmount, String idempotencyKey) throws SQLException {
        if (requestedAmount <= 0L) {
            throw new IllegalArgumentException("requestedAmount must be positive");
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                InvoicePaymentResult result = payInsideTransaction(connection, playerUuid, accountId, requestedAmount, idempotencyKey);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private InvoicePaymentResult payInsideTransaction(Connection connection, UUID playerUuid, UUID accountId, long requestedAmount, String idempotencyKey) throws SQLException {
        Optional<InvoicePaymentResult> duplicate = repository.completedTransaction(connection, idempotencyKey);
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        AccountDebtSnapshot account = repository.lockAccount(connection, accountId).orElse(null);
        if (account == null || !AccountStatus.ACTIVE.name().equals(account.status())) {
            return InvoicePaymentResult.inactiveAccount();
        }

        long totalDebt = account.principalOutstanding() + account.interestOutstanding();
        if (totalDebt <= 0L) {
            return InvoicePaymentResult.noDebt();
        }

        long payAmount = Math.min(requestedAmount, totalDebt);
        if (account.balance() < payAmount) {
            return InvoicePaymentResult.insufficientBalance();
        }

        repository.lockCards(connection, accountId);
        List<InvoiceEntryPayment> entries = repository.entriesToPay(connection, accountId, payAmount);
        if (entries.isEmpty()) {
            return InvoicePaymentResult.noDebt();
        }

        UUID transactionId = UUID.randomUUID();
        InvoicePaymentTotals totals = InvoicePaymentTotals.from(entries);
        long balanceAfter = account.balance() - payAmount;

        writer.reduceEntriesAndCards(connection, entries);
        writer.reduceAccountDebtAndBalance(connection, accountId, balanceAfter, totals.principalPaid(), totals.interestPaid());
        writer.insertTransaction(connection, transactionId, idempotencyKey, payAmount, playerUuid, accountId);
        writer.insertLedger(connection, transactionId, accountId, LedgerEntryType.CREDIT_DEBT_PAYMENT, payAmount, account.balance(), balanceAfter);
        writer.insertPaymentEntries(connection, transactionId, entries);
        return InvoicePaymentResult.completed(payAmount, balanceAfter);
    }
}

