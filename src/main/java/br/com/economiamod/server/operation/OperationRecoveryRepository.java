package br.com.economiamod.server.operation;

import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.persistence.SqlParameterBinder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OperationRecoveryRepository {
    public List<OperationRecoveryCandidate> financialRollbackCandidatesBefore(Instant updatedBefore) throws SQLException {
        String sql = """
                SELECT id, idempotency_key, operation_type
                  FROM economy_operations
                 WHERE updated_at < ?
                   AND state IN (?, ?)
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            SqlParameterBinder.setInstant(statement, 1, updatedBefore);
            statement.setString(2, EconomyOperationState.SQL_COMMITTED.name());
            statement.setString(3, EconomyOperationState.ROLLBACK_REQUIRED.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<OperationRecoveryCandidate> candidates = new ArrayList<>();
                while (resultSet.next()) {
                    candidates.add(new OperationRecoveryCandidate(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("idempotency_key"),
                            EconomyOperationType.valueOf(resultSet.getString("operation_type"))
                    ));
                }
                return List.copyOf(candidates);
            }
        }
    }

    public void mark(UUID operationId, EconomyOperationState state) throws SQLException {
        String sql = """
                UPDATE economy_operations
                   SET state = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       completed_at = CASE WHEN ? IN ('COMPLETED', 'ROLLED_BACK') THEN CURRENT_TIMESTAMP ELSE completed_at END
                 WHERE id = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.name());
            statement.setString(2, state.name());
            statement.setObject(3, operationId);
            statement.executeUpdate();
        }
    }
}
