package br.com.economiamod.server.persistence.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class MigrationResourceLoader {
    public String readSql(MigrationDefinition migration) throws IOException {
        InputStream stream = MigrationResourceLoader.class.getResourceAsStream(migration.resourcePath());
        if (stream == null) {
            throw new IOException("Migration resource not found: " + migration.resourcePath());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}

