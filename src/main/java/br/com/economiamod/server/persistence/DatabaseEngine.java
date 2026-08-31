package br.com.economiamod.server.persistence;

import java.util.Locale;

public enum DatabaseEngine {
    POSTGRESQL,
    SQLITE;

    public static DatabaseEngine parse(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "postgres", "postgresql", "pgsql" -> POSTGRESQL;
            case "sqlite", "sqlite3", "sql" -> SQLITE;
            default -> throw new IllegalArgumentException("Unsupported database.type: " + value + ". Use postgresql/pgsql or sqlite/sql.");
        };
    }
}
