package br.com.economiamod.server.persistence.migration;

import java.util.List;

public final class MigrationCatalog {
    private static final List<MigrationDefinition> MIGRATIONS = List.of(
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
            migration(21, "add_claim_territories_invoices_and_anchors")
    );

    private MigrationCatalog() {
    }

    public static List<MigrationDefinition> all() {
        return MIGRATIONS;
    }

    private static MigrationDefinition migration(int version, String description) {
        String fileName = "V%03d__%s.sql".formatted(version, description);
        return new MigrationDefinition(version, description, "/economia/migrations/" + fileName);
    }
}
