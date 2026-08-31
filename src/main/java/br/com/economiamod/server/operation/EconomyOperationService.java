package br.com.economiamod.server.operation;

import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.RequestFingerprint;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class EconomyOperationService {
    public OperationStartResult begin(String idempotencyKey, EconomyOperationType operationType,
                                      UUID playerUuid, String payload) throws SQLException {
        String fingerprint = RequestFingerprint.of(operationType, playerUuid, payload == null ? "" : payload);
        UUID newId = UUID.randomUUID();
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int inserted;
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO economy_operations(
                            id, idempotency_key, operation_type, player_uuid, state, payload,
                            request_fingerprint, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON CONFLICT (idempotency_key) DO NOTHING
                        """)) {
                    statement.setObject(1, newId);
                    statement.setString(2, idempotencyKey);
                    statement.setString(3, operationType.name());
                    statement.setObject(4, playerUuid);
                    statement.setString(5, EconomyOperationState.CREATED.name());
                    statement.setString(6, payload);
                    statement.setString(7, fingerprint);
                    inserted = statement.executeUpdate();
                }

                OperationRow row = find(connection, idempotencyKey);
                connection.commit();
                if (!row.operationType().equals(operationType.name())
                        || !equalsNullable(row.playerUuid(), playerUuid)
                        || row.requestFingerprint() == null
                        || !row.requestFingerprint().equals(fingerprint)) {
                    return new OperationStartResult(OperationStartType.CONFLICT, row.id(), row.state());
                }
                if (inserted == 1) {
                    return new OperationStartResult(OperationStartType.CREATED, row.id(), row.state());
                }
                if (row.state() == EconomyOperationState.COMPLETED) {
                    return new OperationStartResult(OperationStartType.DUPLICATE_COMPLETED, row.id(), row.state());
                }
                return new OperationStartResult(OperationStartType.IN_PROGRESS, row.id(), row.state());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public UUID createIfMissing(String idempotencyKey, EconomyOperationType operationType,
                                UUID playerUuid, String payload) throws SQLException {
        OperationStartResult start = begin(idempotencyKey, operationType, playerUuid, payload);
        if (start.type() == OperationStartType.CONFLICT) {
            throw new SQLException("Idempotency conflict for operation " + idempotencyKey);
        }
        return start.operationId();
    }

    public boolean mark(String idempotencyKey, EconomyOperationState state) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            return mark(connection, idempotencyKey, state);
        }
    }

    public boolean mark(Connection connection, String idempotencyKey, EconomyOperationState state) throws SQLException {
        Set<EconomyOperationState> allowedCurrent = allowedCurrentStates(state);
        if (allowedCurrent.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(allowedCurrent.size(), "?"));
        String sql = """
                UPDATE economy_operations
                   SET state = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       completed_at = CASE WHEN ? IN ('COMPLETED', 'ROLLED_BACK') THEN CURRENT_TIMESTAMP ELSE completed_at END
                 WHERE idempotency_key = ?
                   AND state IN (%s)
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.name());
            statement.setString(2, state.name());
            statement.setString(3, idempotencyKey);
            int index = 4;
            for (EconomyOperationState current : allowedCurrent) {
                statement.setString(index++, current.name());
            }
            return statement.executeUpdate() == 1;
        }
    }

    public boolean markReconciliationRequired(String idempotencyKey, String resultPayload) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            return markReconciliationRequired(connection, idempotencyKey, resultPayload);
        }
    }

    public boolean markReconciliationRequired(Connection connection, String idempotencyKey, String resultPayload) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_operations
                        SET state = 'RECONCILIATION_REQUIRED',
                            result_payload = ?,
                            updated_at = CURRENT_TIMESTAMP
                      WHERE idempotency_key = ?
                        AND state NOT IN ('COMPLETED', 'ROLLED_BACK')
                     """)) {
            statement.setString(1, resultPayload);
            statement.setString(2, idempotencyKey);
            return statement.executeUpdate() == 1;
        }
    }

    private OperationRow find(Connection connection, String idempotencyKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, operation_type, player_uuid, state, request_fingerprint
                  FROM economy_operations
                 WHERE idempotency_key = ?
                 FOR UPDATE
                """)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("operation was not created");
                }
                return new OperationRow(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("operation_type"),
                        resultSet.getObject("player_uuid", UUID.class),
                        EconomyOperationState.valueOf(resultSet.getString("state")),
                        resultSet.getString("request_fingerprint")
                );
            }
        }
    }

    private Set<EconomyOperationState> allowedCurrentStates(EconomyOperationState next) {
        return switch (next) {
            case ITEMS_RESERVED -> EnumSet.of(EconomyOperationState.CREATED);
            case SQL_COMMITTED -> EnumSet.of(EconomyOperationState.CREATED, EconomyOperationState.ITEMS_RESERVED);
            case ITEMS_DELIVERED -> EnumSet.of(EconomyOperationState.SQL_COMMITTED, EconomyOperationState.ITEMS_RESERVED);
            case COMPLETED -> EnumSet.of(EconomyOperationState.CREATED, EconomyOperationState.ITEMS_RESERVED,
                    EconomyOperationState.SQL_COMMITTED, EconomyOperationState.ITEMS_DELIVERED,
                    EconomyOperationState.RECONCILIATION_REQUIRED);
            case ROLLBACK_REQUIRED -> EnumSet.of(EconomyOperationState.CREATED, EconomyOperationState.ITEMS_RESERVED,
                    EconomyOperationState.SQL_COMMITTED);
            case RECONCILIATION_REQUIRED -> EnumSet.of(EconomyOperationState.ITEMS_RESERVED,
                    EconomyOperationState.SQL_COMMITTED, EconomyOperationState.ITEMS_DELIVERED,
                    EconomyOperationState.ROLLBACK_REQUIRED);
            case ROLLED_BACK -> EnumSet.of(EconomyOperationState.CREATED, EconomyOperationState.ITEMS_RESERVED,
                    EconomyOperationState.ROLLBACK_REQUIRED, EconomyOperationState.RECONCILIATION_REQUIRED);
            case CREATED -> EnumSet.noneOf(EconomyOperationState.class);
        };
    }

    private boolean equalsNullable(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private record OperationRow(UUID id, String operationType, UUID playerUuid,
                                EconomyOperationState state, String requestFingerprint) {
    }
}
