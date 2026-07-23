package br.com.economiamod.server.persistence;

import br.com.economiamod.server.account.SystemAccountInitializer;
import br.com.economiamod.server.gold.GoldReserveService;
import br.com.economiamod.server.persistence.migration.MigrationCatalog;
import br.com.economiamod.server.persistence.migration.MigrationCatalogVerifier;
import br.com.economiamod.server.persistence.migration.MigrationResourceLoader;
import br.com.economiamod.server.persistence.migration.SqlMigrationExecutor;
import br.com.economiamod.server.persistence.migration.VerifiedMigration;
import br.com.economiamod.server.session.BankSessionService;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class DatabaseResetService {
    public int reset() throws IOException, SQLException {
        DatabaseSettings settings = DatabaseSettings.fromConfig();
        List<VerifiedMigration> migrations = new MigrationCatalogVerifier(new MigrationResourceLoader()).verifyCatalog();

        EconomyDatabaseState.initializing(MigrationCatalog.all().size());
        EconomyDatabase.open(settings);
        dropEconomyObjects();
        new SqlMigrationExecutor(settings).apply(migrations);
        new SystemAccountInitializer().initialize();
        new GoldReserveService().initialize();
        BankSessionService.INSTANCE.clear();
        EconomyDatabaseState.available(migrations.size());
        return migrations.size();
    }

    private void dropEconomyObjects() throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    DROP TABLE IF EXISTS
                        economy_operations,
                        economy_audit_logs,
                        economy_daily_job_runs,
                        economy_daily_interest_accruals,
                        economy_gold_exchange_entries,
                        economy_gold_reserve_summary,
                        economy_inventory_slots,
                        economy_shop_offers,
                        economy_commercial_blocks,
                        economy_card_entries,
                        economy_ledger_entries,
                        economy_transactions,
                        economy_cards,
                        economy_accounts,
                        economy_schema_migrations
                    CASCADE
                    """);
            statement.execute("DROP SEQUENCE IF EXISTS economy_account_number_seq");
        }
    }
}
