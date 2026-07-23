package br.com.economiamod.server.operation;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class EconomyOperationService {
    public UUID createIfMissing(String idempotencyKey, EconomyOperationType operationType, UUID playerUuid, String payload) throws SQLException {
        UUID operationId = UUID.randomUUID();
        String sql = """
                INSERT INTO economy_operations(
                    id,
                    idempotency_key,
                    operation_type,
                    player_uuid,
                    state,
                    payload,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (idempotency_key) DO NOTHING
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, operationId);
            statement.setString(2, idempotencyKey);
            statement.setString(3, operationType.name());
            statement.setObject(4, playerUuid);
            statement.setString(5, EconomyOperationState.CREATED.name());
            statement.setString(6, payload);
            statement.executeUpdate();
            return findOperationId(connection, idempotencyKey);
        }
    }

    public void mark(String idempotencyKey, EconomyOperationState state) throws SQLException {
        String sql = """
                UPDATE economy_operations
                   SET state = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       completed_at = CASE WHEN ? IN ('COMPLETED', 'ROLLED_BACK') THEN CURRENT_TIMESTAMP ELSE completed_at END
                 WHERE idempotency_key = ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.name());
            statement.setString(2, state.name());
            statement.setString(3, idempotencyKey);
            statement.executeUpdate();
        }
    }

    private UUID findOperationId(Connection connection, String idempotencyKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM economy_operations WHERE idempotency_key = ?")) {
            statement.setString(1, idempotencyKey);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getObject("id", UUID.class);
                }
                throw new SQLException("operation was not created");
            }
        }
    }
}

