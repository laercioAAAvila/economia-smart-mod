package br.com.economiamod.server.command;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.server.account.AccountBalanceSummary;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.persistence.DatabaseStateSnapshot;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import br.com.economiamod.server.session.BankSession;
import br.com.economiamod.server.session.BankSessionService;
import java.sql.SQLException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class GeneralCommandHandlers {
    public int status(CommandSourceStack source) {
        DatabaseStateSnapshot snapshot = EconomyDatabaseState.current();
        source.sendSuccess(() -> Component.translatable(
                "commands.economia.status",
                snapshot.availability().name(),
                snapshot.knownMigrations(),
                snapshot.message()
        ), false);
        return 1;
    }

    public int unavailable(CommandSourceStack source) {
        source.sendFailure(Component.translatable("commands.economia.unavailable"));
        return 0;
    }

    public int secureUiPending(CommandSourceStack source) {
        if (!EconomyDatabaseState.isAvailable()) {
            return unavailable(source);
        }

        source.sendFailure(Component.translatable("commands.economia.secure_ui_pending"));
        return 0;
    }

    public int logout(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            boolean loggedOut = BankSessionService.INSTANCE.logout(player);
            if (loggedOut) {
                source.sendSuccess(() -> Component.translatable("commands.economia.logout.success"), false);
                return 1;
            }

            source.sendFailure(Component.translatable("commands.economia.logout.no_session"));
            return 0;
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("commands.economia.player_only"));
            return 0;
        }
    }

    public int balance(CommandSourceStack source) {
        if (!EconomyDatabaseState.isAvailable()) {
            return unavailable(source);
        }

        try {
            ServerPlayer player = source.getPlayerOrException();
            BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
            if (session == null) {
                source.sendFailure(Component.translatable("commands.economia.session.required"));
                return 0;
            }

            AccountBalanceSummary summary = new AccountQueryService().findBalanceSummary(session.accountId()).orElse(null);
            return summary == null ? invalidSession(source, player) : showBalance(source, summary);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao consultar saldo bancario.", exception);
            return unavailable(source);
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("commands.economia.player_only"));
            return 0;
        }
    }

    private int invalidSession(CommandSourceStack source, ServerPlayer player) {
        BankSessionService.INSTANCE.logout(player);
        source.sendFailure(Component.translatable("commands.economia.session.invalid"));
        return 0;
    }

    private int showBalance(CommandSourceStack source, AccountBalanceSummary summary) {
        source.sendSuccess(() -> Component.translatable("commands.economia.balance.bank", summary.balance()), false);
        source.sendSuccess(() -> Component.translatable("commands.economia.balance.available", summary.availableBalance()), false);
        source.sendSuccess(() -> Component.translatable("commands.economia.balance.credit_limit", summary.configuredCreditLimit()), false);
        source.sendSuccess(() -> Component.translatable("commands.economia.balance.principal", summary.creditPrincipalOutstanding()), false);
        source.sendSuccess(() -> Component.translatable("commands.economia.balance.interest", summary.creditInterestOutstanding()), false);
        source.sendSuccess(() -> Component.translatable("commands.economia.balance.debt", summary.totalDebt()), false);
        source.sendSuccess(() -> Component.translatable("commands.economia.balance.credit_available", summary.globalCreditAvailable()), false);
        return 1;
    }
}
