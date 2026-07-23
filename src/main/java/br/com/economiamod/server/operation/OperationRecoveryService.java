package br.com.economiamod.server.operation;

import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.persistence.SqlParameterBinder;
import br.com.economiamod.server.reversal.ReversalResult;
import br.com.economiamod.server.reversal.ReversalResultType;
import br.com.economiamod.server.reversal.ReversalService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

public final class OperationRecoveryService {
    private final OperationRecoveryRepository recoveryRepository = new OperationRecoveryRepository();
    private final ReversalService reversalService = new ReversalService();

    public OperationRecoveryResult recoverStaleOperations(Instant updatedBefore) throws SQLException {
        int financiallyReversed = reverseSqlCommitted(updatedBefore);
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int rolledBack = mark(connection, updatedBefore, EconomyOperationState.ROLLED_BACK, EconomyOperationState.CREATED, EconomyOperationState.ITEMS_RESERVED);
                int completed = mark(connection, updatedBefore, EconomyOperationState.COMPLETED, EconomyOperationState.ITEMS_DELIVERED);
                int rollbackRequired = mark(connection, updatedBefore, EconomyOperationState.ROLLBACK_REQUIRED, EconomyOperationState.SQL_COMMITTED);
                connection.commit();
                return new OperationRecoveryResult(rolledBack, completed, rollbackRequired, financiallyReversed);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private int reverseSqlCommitted(Instant updatedBefore) throws SQLException {
        int reversed = 0;
        for (OperationRecoveryCandidate candidate : recoveryRepository.financialRollbackCandidatesBefore(updatedBefore)) {
            ReversalResult result = reversalService.reverseByIdempotencyKey(candidate.idempotencyKey(), "recovery:" + candidate.idempotencyKey());
            if (result.type() == ReversalResultType.COMPLETED || result.type() == ReversalResultType.DUPLICATE_COMPLETED) {
                recoveryRepository.mark(candidate.operationId(), EconomyOperationState.ROLLED_BACK);
                reversed++;
            }
        }
        return reversed;
    }

    private int mark(Connection connection, Instant updatedBefore, EconomyOperationState nextState, EconomyOperationState... currentStates) throws SQLException {
        String placeholders = "?,".repeat(currentStates.length);
        placeholders = placeholders.substring(0, placeholders.length() - 1);
        String sql = """
                UPDATE economy_operations
                   SET state = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       completed_at = CASE WHEN ? IN ('COMPLETED', 'ROLLED_BACK') THEN CURRENT_TIMESTAMP ELSE completed_at END
                 WHERE updated_at < ?
                   AND state IN (%s)
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nextState.name());
            statement.setString(2, nextState.name());
            SqlParameterBinder.setInstant(statement, 3, updatedBefore);
            for (int index = 0; index < currentStates.length; index++) {
                statement.setString(index + 4, currentStates[index].name());
            }
            return statement.executeUpdate();
        }
    }
}
