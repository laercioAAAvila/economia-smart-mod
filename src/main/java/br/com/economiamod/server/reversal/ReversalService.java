package br.com.economiamod.server.reversal;

import br.com.economiamod.server.transaction.EconomyTransactionStatus;
import br.com.economiamod.server.transaction.LedgerEntryType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ReversalService {
    private static final Set<LedgerEntryType> SUPPORTED = EnumSet.of(LedgerEntryType.DEBIT, LedgerEntryType.CREDIT, LedgerEntryType.ADJUSTMENT);

    private final ReversalRepository repository = new ReversalRepository();

    public ReversalResult reverse(UUID originalTransactionId, String idempotencyKey) throws SQLException {
        try (Connection connection = repository.openConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ReversalResult result = reverseInTransaction(connection, originalTransactionId, idempotencyKey);
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

    public ReversalResult reverseByIdempotencyKey(String originalIdempotencyKey, String reversalIdempotencyKey) throws SQLException {
        try (Connection connection = repository.openConnection()) {
            UUID transactionId = repository.transactionByIdempotencyKey(connection, originalIdempotencyKey).orElse(null);
            if (transactionId == null) {
                return ReversalResult.invalid(ReversalResultType.ORIGINAL_NOT_FOUND);
            }
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ReversalResult result = reverseInTransaction(connection, transactionId, reversalIdempotencyKey);
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

    private ReversalResult reverseInTransaction(Connection connection, UUID originalTransactionId, String idempotencyKey) throws SQLException {
        var duplicate = repository.completedByIdempotencyKey(connection, idempotencyKey);
        if (duplicate.isPresent()) {
            return ReversalResult.duplicate(duplicate.get());
        }

        ReversalTarget target = repository.lockTarget(connection, originalTransactionId).orElse(null);
        if (target == null) {
            return ReversalResult.invalid(ReversalResultType.ORIGINAL_NOT_FOUND);
        }
        if (!EconomyTransactionStatus.COMPLETED.name().equals(target.status())) {
            return ReversalResult.invalid(ReversalResultType.ORIGINAL_NOT_COMPLETED);
        }

        var impacts = repository.impacts(connection, originalTransactionId);
        if (impacts.isEmpty() || impacts.stream().anyMatch(impact -> !SUPPORTED.contains(impact.entryType()))) {
            return ReversalResult.invalid(ReversalResultType.UNSUPPORTED_TRANSACTION);
        }

        Map<UUID, Long> balances = lockBalances(connection, impacts);
        if (!canApply(impacts, balances)) {
            return ReversalResult.invalid(ReversalResultType.INSUFFICIENT_BALANCE);
        }

        UUID reversalTransactionId = UUID.randomUUID();
        repository.insertReversalTransaction(connection, reversalTransactionId, idempotencyKey, target);
        for (LedgerImpact impact : impacts) {
            long before = balances.get(impact.accountId());
            long after = before + impact.reversalDelta();
            balances.put(impact.accountId(), after);
            repository.updateBalance(connection, impact.accountId(), after);
            repository.insertLedger(connection, reversalTransactionId, impact.accountId(), impact.amount(), before, after);
        }
        repository.markReversed(connection, originalTransactionId);
        return ReversalResult.completed(reversalTransactionId);
    }

    private Map<UUID, Long> lockBalances(Connection connection, Iterable<LedgerImpact> impacts) throws SQLException {
        Map<UUID, Long> balances = new LinkedHashMap<>();
        for (LedgerImpact impact : impacts) {
            if (!balances.containsKey(impact.accountId())) {
                balances.put(impact.accountId(), repository.lockBalance(connection, impact.accountId()));
            }
        }
        return balances;
    }

    private boolean canApply(Iterable<LedgerImpact> impacts, Map<UUID, Long> startingBalances) {
        Map<UUID, Long> balances = new LinkedHashMap<>(startingBalances);
        for (LedgerImpact impact : impacts) {
            long after = balances.get(impact.accountId()) + impact.reversalDelta();
            if (after < 0L) {
                return false;
            }
            balances.put(impact.accountId(), after);
        }
        return true;
    }
}
