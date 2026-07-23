package br.com.economiamod.server.persistence.migration;

public record MigrationDefinition(int version, String description, String resourcePath) {
}

