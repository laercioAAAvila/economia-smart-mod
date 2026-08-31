package br.com.economiamod.server.persistence.migration;

import br.com.economiamod.server.persistence.DatabaseSettings;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public final class SqlMigrationExecutor {
    private final DatabaseSettings settings;

    public SqlMigrationExecutor(DatabaseSettings settings) {
        this.settings = settings;
    }

    public int apply(List<VerifiedMigration> migrations) throws SQLException {
        int applied = 0;
        for (VerifiedMigration migration : migrations) {
            applyOne(migration);
            applied++;
        }
        return applied;
    }

    private void applyOne(VerifiedMigration migration) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (migration.definition().version() == 0) {
                    executeSql(connection, migration.sql());
                    insertMigrationIfMissing(connection, migration);
                    connection.commit();
                    connection.setAutoCommit(previousAutoCommit);
                    return;
                }

                Optional<String> appliedChecksum = findAppliedChecksum(connection, migration.definition().version());
                if (appliedChecksum.isPresent()) {
                    validateChecksum(migration, appliedChecksum.get());
                    connection.commit();
                    connection.setAutoCommit(previousAutoCommit);
                    return;
                }

                executeSql(connection, migration.sql());
                insertMigration(connection, migration);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void executeSql(Connection connection, String sql) throws SQLException {
        for (String statementSql : SqlStatementSplitter.split(sql)) {
            String trimmed = statementSql.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(queryTimeoutSeconds());
                statement.execute(trimmed);
            }
        }
    }

    private Optional<String> findAppliedChecksum(Connection connection, int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT checksum FROM economy_schema_migrations WHERE version = ?")) {
            statement.setQueryTimeout(queryTimeoutSeconds());
            statement.setInt(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getString("checksum"));
                }
                return Optional.empty();
            }
        }
    }

    private void insertMigrationIfMissing(Connection connection, VerifiedMigration migration) throws SQLException {
        Optional<String> appliedChecksum = findAppliedChecksum(connection, migration.definition().version());
        if (appliedChecksum.isPresent()) {
            validateChecksum(migration, appliedChecksum.get());
            return;
        }
        insertMigration(connection, migration);
    }

    private void insertMigration(Connection connection, VerifiedMigration migration) throws SQLException {
        String sql = """
                INSERT INTO economy_schema_migrations(version, description, checksum, applied_at, execution_time_ms)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(queryTimeoutSeconds());
            statement.setInt(1, migration.definition().version());
            statement.setString(2, migration.definition().description());
            statement.setString(3, migration.checksum());
            statement.setLong(4, 0L);
            statement.executeUpdate();
        }
    }

    private void validateChecksum(VerifiedMigration migration, String appliedChecksum) {
        if (!migration.checksum().equals(appliedChecksum)) {
            throw new IllegalStateException("Migration checksum mismatch for version " + migration.definition().version());
        }
    }

    private int queryTimeoutSeconds() {
        return Math.max(1, Math.toIntExact(settings.queryTimeoutMs() / 1000L));
    }
}

