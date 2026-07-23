package br.com.economiamod.server.persistence.migration;

public record VerifiedMigration(MigrationDefinition definition, String sql, String checksum) {
}

