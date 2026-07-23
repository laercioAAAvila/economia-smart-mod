package br.com.economiamod.server.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public final class SqlParameterBinder {
    private SqlParameterBinder() {
    }

    public static void setInstant(PreparedStatement statement, int index, Instant value) throws SQLException {
        statement.setTimestamp(index, Timestamp.from(value));
    }
}
