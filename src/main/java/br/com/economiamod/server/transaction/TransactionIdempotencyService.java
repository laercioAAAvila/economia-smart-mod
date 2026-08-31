package br.com.economiamod.server.transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class TransactionIdempotencyService {
    public IdempotencyCheck check(Connection connection, String idempotencyKey, String expectedFingerprint) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT request_fingerprint
                  FROM economy_transactions
                 WHERE idempotency_key = ?
                 LIMIT 1
                """)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return IdempotencyCheck.ABSENT;
                }
                String stored = resultSet.getString("request_fingerprint");
                return stored != null && stored.equals(expectedFingerprint)
                        ? IdempotencyCheck.MATCH
                        : IdempotencyCheck.CONFLICT;
            }
        }
    }
}
