package br.com.economiamod.server.persistence.migration;

import br.com.economiamod.server.persistence.DatabaseEngine;
import java.util.ArrayList;
import java.util.List;

public final class MigrationCatalog {
    private static final List<MigrationDefinition> POSTGRESQL_MIGRATIONS = buildPostgresql();
    private static final List<MigrationDefinition> SQLITE_MIGRATIONS = List.of(
            sqliteMigration(0, "create_schema_migrations"),
            sqliteMigration(29, "baseline_v029")
    );

    private MigrationCatalog() {
    }

    public static List<MigrationDefinition> all() {
        return all(DatabaseEngine.POSTGRESQL);
    }

    public static List<MigrationDefinition> all(DatabaseEngine engine) {
        return engine == DatabaseEngine.SQLITE ? SQLITE_MIGRATIONS : POSTGRESQL_MIGRATIONS;
    }

    private static List<MigrationDefinition> buildPostgresql() {
        List<MigrationDefinition> migrations = new ArrayList<>(List.of(
                migration(0, "create_schema_migrations"),
                migration(1, "create_accounts"),
                migration(2, "create_cards"),
                migration(3, "create_transactions_and_ledger"),
                migration(4, "create_card_entries_and_interest"),
                migration(5, "create_commercial_blocks"),
                migration(6, "create_shop_offers"),
                migration(7, "create_inventory_slots"),
                migration(8, "create_gold_reserve"),
                migration(9, "create_daily_stats_and_jobs"),
                migration(10, "create_audit_logs"),
                migration(11, "create_operations"),
                migration(12, "allow_gold_exchange_without_block"),
                migration(13, "add_account_numbers"),
                migration(14, "add_performance_indexes"),
                migration(15, "align_account_number_sequence"),
                migration(16, "add_operation_recovery_index"),
                migration(17, "add_debit_daily_limits"),
                migration(18, "add_card_creation_numbers"),
                migration(19, "add_mail_blocks"),
                migration(20, "create_groups_claims_and_locations"),
                migration(21, "add_claim_territories_invoices_and_anchors"),
                migration(22, "add_claim_payments_and_upgrades"),
                migration(23, "add_minecraft_player_name"),
                migration(24, "scope_player_accounts_by_server"),
                migration(25, "private_property_permissions"),
                migration(26, "claim_invoice_bundle_cleanup"),
                migration(27, "scope_world_data_by_server"),
                migration(28, "scope_commercial_blocks_by_server"),
                migration(29, "transaction_security_and_web_origin")
        ));
        return List.copyOf(migrations);
    }

    private static MigrationDefinition migration(int version, String description) {
        String fileName = "V%03d__%s.sql".formatted(version, description);
        return new MigrationDefinition(version, description, "/economia/migrations/" + fileName);
    }

    private static MigrationDefinition sqliteMigration(int version, String description) {
        String fileName = "V%03d__%s.sql".formatted(version, description);
        return new MigrationDefinition(version, description, "/economia/migrations/sqlite/" + fileName);
    }
}
