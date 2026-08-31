package br.com.economiamod.server.event;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.server.account.SystemAccountInitializer;
import br.com.economiamod.server.account.AccountPlayerIdentityService;
import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.gold.GoldReserveService;
import br.com.economiamod.server.group.GroupRepository;
import br.com.economiamod.server.group.ServerActiveClockService;
import br.com.economiamod.server.group.ClanLeadershipService;
import br.com.economiamod.server.group.GroupChatService;
import br.com.economiamod.server.group.ClaimLimitUpgradeService;
import br.com.economiamod.server.claim.ClaimAnchorChunkLoaderService;
import br.com.economiamod.server.claim.ClaimPurchaseSessionService;
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
import br.com.economiamod.server.web.WebEconomyApi;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class EconomyServerEvents {
    private static int socialMaintenanceTicks;
    private static int claimAnchorTicks;
    private EconomyServerEvents() {
    }

    public static void onServerStarting(ServerStartingEvent event) {
        int knownMigrations = 0;
        DatabaseSettings previewSettings = null;
        DatabaseSettings databaseSettings = null;
        String initializationStage = "leitura_da_configuracao";

        try {
            previewSettings = DatabaseSettings.fromConfig(event.getServer());
            knownMigrations = MigrationCatalog.all(previewSettings.engine()).size();
            EconomyDatabaseState.initializing(knownMigrations);
            initializationStage = "carregamento_das_migracoes";
            List<VerifiedMigration> verifiedMigrations = new MigrationCatalogVerifier(new MigrationResourceLoader()).verifyCatalog(previewSettings.engine());
            initializationStage = "leitura_da_configuracao";
            databaseSettings = previewSettings;
            initializationStage = "conexao_com_o_banco";
            EconomyDatabase.open(databaseSettings);
            initializationStage = "aplicacao_das_migracoes";
            new SqlMigrationExecutor(databaseSettings).apply(verifiedMigrations);
            initializationStage = "identidade_do_servidor_bancario";
            BankServerIdentityService.INSTANCE.initialize(event.getServer());
            initializationStage = "inicializacao_das_contas_do_sistema";
            new SystemAccountInitializer().initialize();
            initializationStage = "inicializacao_da_reserva_de_ouro";
            new GoldReserveService().initialize();
            initializationStage = "inicializacao_dos_claims";
            new ClaimLimitUpgradeService().normalizeAll();
            ServerActiveClockService.INSTANCE.start();
            new ClanLeadershipService().process(ServerActiveClockService.INSTANCE.currentMillis());
            ClaimAnchorChunkLoaderService.INSTANCE.refresh(event.getServer());
            recoverOperations();
            EconomyDatabaseState.available(verifiedMigrations.size());
            try {
                WebEconomyApi.INSTANCE.startIfEnabled();
            } catch (IOException | RuntimeException webException) {
                EconomiaMod.LOGGER.error("Web API nao foi iniciada. O banco do jogo continua disponivel.", webException);
            }
            EconomiaMod.LOGGER.info("Economia Mod conectou ao banco {} e aplicou/verificou {} migracoes.", databaseSettings.engine(), verifiedMigrations.size());
        } catch (IOException exception) {
            EconomyDatabaseState.unavailable("Falha ao carregar migracoes SQL: " + exception.getMessage(), knownMigrations);
            EconomiaMod.LOGGER.error(
                    "Falha ao iniciar persistencia SQL. etapa={}, tipoErro={}, detalhe={}",
                    initializationStage,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
        } catch (SQLException exception) {
            EconomyDatabaseState.unavailable("Persistencia SQL indisponivel: " + exception.getMessage(), knownMigrations);
            EconomiaMod.LOGGER.warn(
                    "Persistencia SQL indisponivel. etapa={}, destino={}, sqlState={}, codigoSql={}, detalhe={}. "
                            + "Operacoes financeiras permanecerao bloqueadas.",
                    initializationStage,
                    safeDatabaseTarget(databaseSettings),
                    exception.getSQLState(),
                    exception.getErrorCode(),
                    exception.getMessage(),
                    exception
            );
        } catch (RuntimeException exception) {
            EconomyDatabaseState.unavailable("Persistencia SQL indisponivel: " + exception.getMessage(), knownMigrations);
            EconomiaMod.LOGGER.warn(
                    "Persistencia SQL indisponivel. etapa={}, destino={}, tipoErro={}, detalhe={}. "
                            + "Operacoes financeiras permanecerao bloqueadas.",
                    initializationStage,
                    safeDatabaseTarget(databaseSettings),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
        }
    }

    private static String safeDatabaseTarget(DatabaseSettings settings) {
        if (settings == null) {
            return "nao_configurado";
        }
        return settings.safeTarget();
    }

    private static void recoverOperations() throws SQLException {
        OperationRecoveryResult recovery = new OperationRecoveryService().recoverStaleOperations(Instant.now());
        if (recovery.totalTouched() > 0) {
            EconomiaMod.LOGGER.warn(
                    "Recuperacao de operacoes: {} revertidas, {} concluidas, {} exigem reconciliacao manual.",
                    recovery.rolledBack() + recovery.financiallyReversed(),
                    recovery.completed(),
                    recovery.rollbackRequired()
            );
        }
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        WebEconomyApi.INSTANCE.stop();
        ClaimAnchorChunkLoaderService.INSTANCE.stop(event.getServer());
        ServerActiveClockService.INSTANCE.stop();
        DatabaseSettings settings = null;
        try {
            settings = DatabaseSettings.fromConfig(event.getServer());
        } catch (RuntimeException ignored) {
            // Estado de parada deve ser seguro mesmo com configuracao invalida.
        }
        EconomyDatabase.close();
        BankSessionService.INSTANCE.clear();
        ClaimPurchaseSessionService.INSTANCE.clearAll();
        BankServerIdentityService.INSTANCE.clear();
        int migrations = settings == null ? 0 : MigrationCatalog.all(settings.engine()).size();
        EconomyDatabaseState.unavailable("Servidor parado. Persistencia SQL fechada.", migrations);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BankSessionService.INSTANCE.logout(event.getEntity().getUUID());
        ClaimPurchaseSessionService.INSTANCE.clear(event.getEntity().getUUID());
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
            new AccountPlayerIdentityService().refresh(
                    event.getEntity().getUUID(), event.getEntity().getGameProfile().getName());
            new GroupRepository().updateLastActivity(
                    event.getEntity().getUUID(),
                    ServerActiveClockService.INSTANCE.currentMillis()
            );
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao atualizar identidade Minecraft ou atividade social; identificadores omitidos.", exception);
        }
    }
}
