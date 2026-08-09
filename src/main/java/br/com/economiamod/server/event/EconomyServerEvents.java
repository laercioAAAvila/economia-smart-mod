package br.com.economiamod.server.event;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.server.account.SystemAccountInitializer;
import br.com.economiamod.server.gold.GoldReserveService;
import br.com.economiamod.server.group.GroupRepository;
import br.com.economiamod.server.group.ServerActiveClockService;
import br.com.economiamod.server.group.ClanLeadershipService;
import br.com.economiamod.server.group.GroupChatService;
import br.com.economiamod.server.claim.ClaimAnchorChunkLoaderService;
import net.neoforged.neoforge.event.ServerChatEvent;
import br.com.economiamod.server.operation.OperationRecoveryResult;
import br.com.economiamod.server.operation.OperationRecoveryService;
import br.com.economiamod.server.persistence.DatabaseSettings;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import br.com.economiamod.server.persistence.migration.MigrationCatalog;
import br.com.economiamod.server.persistence.migration.MigrationCatalogVerifier;
import br.com.economiamod.server.persistence.migration.MigrationResourceLoader;
import br.com.economiamod.server.persistence.migration.SqlMigrationExecutor;
import br.com.economiamod.server.persistence.migration.VerifiedMigration;
import br.com.economiamod.server.session.BankSessionService;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class EconomyServerEvents {
    private static int socialMaintenanceTicks;
    private static int claimAnchorTicks;
    private EconomyServerEvents() {
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        int knownMigrations = MigrationCatalog.all().size();
        EconomyDatabaseState.initializing(knownMigrations);

        try {
            List<VerifiedMigration> verifiedMigrations = new MigrationCatalogVerifier(new MigrationResourceLoader()).verifyCatalog();
            DatabaseSettings databaseSettings = DatabaseSettings.fromConfig();
            EconomyDatabase.open(databaseSettings);
            new SqlMigrationExecutor(databaseSettings).apply(verifiedMigrations);
            new SystemAccountInitializer().initialize();
            new GoldReserveService().initialize();
            ServerActiveClockService.INSTANCE.start();
            new ClanLeadershipService().process(ServerActiveClockService.INSTANCE.currentMillis());
            ClaimAnchorChunkLoaderService.INSTANCE.refresh(event.getServer());
            recoverOperations();
            EconomyDatabaseState.available(verifiedMigrations.size());
            EconomiaMod.LOGGER.info("Economia Mod conectou ao SQL e aplicou/verificou {} migracoes.", verifiedMigrations.size());
        } catch (IOException exception) {
            EconomyDatabaseState.unavailable("Falha ao carregar migracoes SQL: " + exception.getMessage(), knownMigrations);
            EconomiaMod.LOGGER.error("Falha ao verificar migracoes SQL do Economia Mod.", exception);
        } catch (SQLException | RuntimeException exception) {
            EconomyDatabaseState.unavailable("Persistencia SQL indisponivel: " + exception.getMessage(), knownMigrations);
            EconomiaMod.LOGGER.warn(
                    "Persistencia SQL indisponivel. Operacoes financeiras permanecerao bloqueadas: {}",
                    exception.getMessage(),
                    exception
            );
        }
    }

    private static void recoverOperations() throws SQLException {
        OperationRecoveryResult recovery = new OperationRecoveryService().recoverStaleOperations(Instant.now());
        if (recovery.totalTouched() > 0) {
            EconomiaMod.LOGGER.warn(
                    "Recuperacao de operacoes: {} revertidas, {} concluidas, {} exigem rollback.",
                    recovery.rolledBack() + recovery.financiallyReversed(),
                    recovery.completed(),
                    recovery.rollbackRequired()
            );
        }
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        ClaimAnchorChunkLoaderService.INSTANCE.stop(event.getServer());
        ServerActiveClockService.INSTANCE.stop();
        EconomyDatabase.close();
        BankSessionService.INSTANCE.clear();
        EconomyDatabaseState.unavailable("Servidor parado. Persistencia SQL fechada.", MigrationCatalog.all().size());
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BankSessionService.INSTANCE.logout(event.getEntity().getUUID());
        GroupChatService.INSTANCE.logout(event.getEntity().getUUID());
    }

    public static void onServerChat(ServerChatEvent event) {
        GroupChatService.INSTANCE.onChat(event);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerActiveClockService.INSTANCE.tick();
        if (++claimAnchorTicks >= 200 && EconomyDatabaseState.isAvailable()) {
            claimAnchorTicks = 0;
            try {
                ClaimAnchorChunkLoaderService.INSTANCE.refresh(event.getServer());
            } catch (SQLException exception) {
                EconomiaMod.LOGGER.warn("Falha ao atualizar ancoras de claim.", exception);
            }
        }
        if (++socialMaintenanceTicks < 6000 || !EconomyDatabaseState.isAvailable()) {
            return;
        }
        socialMaintenanceTicks = 0;
        try {
            new ClanLeadershipService().process(ServerActiveClockService.INSTANCE.currentMillis());
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha na manutenção de liderança dos clãs.", exception);
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!EconomyDatabaseState.isAvailable()) {
            return;
        }
        try {
            new GroupRepository().updateLastActivity(
                    event.getEntity().getUUID(),
                    ServerActiveClockService.INSTANCE.currentMillis()
            );
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao registrar atividade do membro.", exception);
        }
    }
}
