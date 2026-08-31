package br.com.economiamod.server.persistence.migration;

import br.com.economiamod.server.persistence.DatabaseEngine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MigrationCatalogVerifier {
    private final MigrationResourceLoader loader;

    public MigrationCatalogVerifier(MigrationResourceLoader loader) {
        this.loader = loader;
    }

    public List<VerifiedMigration> verifyCatalog() throws IOException {
        return verifyCatalog(DatabaseEngine.POSTGRESQL);
    }

    public List<VerifiedMigration> verifyCatalog(DatabaseEngine engine) throws IOException {
        List<VerifiedMigration> verified = new ArrayList<>();
        for (MigrationDefinition migration : MigrationCatalog.all(engine)) {
            String sql = loader.readSql(migration);
            if (sql.isBlank()) {
                throw new IOException("Migration is empty: " + migration.resourcePath());
            }
            verified.add(new VerifiedMigration(migration, sql, MigrationChecksum.sha256(sql)));
        }
        return verified;
    }
}
