package br.com.economiamod.server.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

public final class AuditLogService {
    public void recordAdminChange(Connection connection, UUID actorPlayerUuid, String action, String targetType, UUID targetId, String oldValue, String newValue) throws SQLException {
        String sql = """
                INSERT INTO economy_audit_logs(
                    id, actor_player_uuid, actor_type, action, target_type, target_id,
                    old_value, new_value, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, actorPlayerUuid);
            statement.setString(3, AuditActorType.ADMIN.name());
            statement.setString(4, action);
            statement.setString(5, targetType);
            setNullableUuid(statement, 6, targetId);
            statement.setString(7, oldValue);
            statement.setString(8, newValue);
            statement.executeUpdate();
        }
    }

    private void setNullableUuid(PreparedStatement statement, int index, UUID value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.OTHER);
        } else {
            statement.setObject(index, value);
        }
    }
}
