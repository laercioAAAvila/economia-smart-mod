package br.com.economiamod.server.operation;

import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.persistence.SqlParameterBinder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

public final class OperationRecoveryService {
    public OperationRecoveryResult recoverStaleOperations(Instant updatedBefore) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int rolledBack = mark(connection, updatedBefore, EconomyOperationState.ROLLED_BACK,
                        EconomyOperationState.CREATED);
                int completed = mark(connection, updatedBefore, EconomyOperationState.COMPLETED,
                        EconomyOperationState.ITEMS_DELIVERED);
                int reconciliation = mark(connection, updatedBefore, EconomyOperationState.RECONCILIATION_REQUIRED,
                        EconomyOperationState.ITEMS_RESERVED,
                        EconomyOperationState.SQL_COMMITTED,
                        EconomyOperationState.ROLLBACK_REQUIRED);
                connection.commit();
                // financiallyReversed is intentionally zero: automatically reversing a transaction
                // after a crash can duplicate physical money/items if delivery already happened.
                return new OperationRecoveryResult(rolledBack, completed, reconciliation, 0);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private int mark(Connection connection, Instant updatedBefore, EconomyOperationState nextState,
                     EconomyOperationState... currentStates) throws SQLException {
        String placeholders = String.join(",", java.util.Collections.nCopies(currentStates.length, "?"));
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
