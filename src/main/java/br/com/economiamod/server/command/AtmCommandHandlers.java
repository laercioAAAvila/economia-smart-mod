package br.com.economiamod.server.command;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.card.CardType;
import br.com.economiamod.server.card.CardIssueRequest;
import br.com.economiamod.server.card.CardIssueResult;
import br.com.economiamod.server.card.CardIssueResultType;
import br.com.economiamod.server.card.CardIssueService;
import br.com.economiamod.server.cash.CashAccountOperationResult;
import br.com.economiamod.server.cash.CashAccountOperationResultType;
import br.com.economiamod.server.cash.CashAccountOperationService;
import br.com.economiamod.server.cash.CashInventoryService;
import br.com.economiamod.server.operation.EconomyOperationService;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import br.com.economiamod.server.session.BankSession;
import br.com.economiamod.server.session.BankSessionService;
import br.com.economiamod.server.transaction.AccountFinancialService;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AtmCommandHandlers {
    private final CashAccountOperationService cashOperations = new CashAccountOperationService(
            new CashInventoryService(),
            new AccountFinancialService(),
            new EconomyOperationService()
    );
    private final CardIssueService cardIssueService = new CardIssueService();
    private final CardItemDataService cardItemDataService = new CardItemDataService();

    public int depositAll(CommandSourceStack source) {
        if (!EconomyDatabaseState.isAvailable()) {
            return unavailable(source);
        }
        try {
            ServerPlayer player = source.getPlayerOrException();
            BankSession session = requireSession(source, player);
            if (session == null) {
                return 0;
            }
            CashAccountOperationResult result = cashOperations.depositAll(player, session, idempotencyKey(player, "deposit"));
            return cashResult(source, result, "commands.economia.atm.deposit.success");
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao depositar pelo ATM.", exception);
            return unavailable(source);
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("commands.economia.player_only"));
            return 0;
        }
    }

    public int withdraw(CommandSourceStack source, int amount) {
        if (!EconomyDatabaseState.isAvailable()) {
            return unavailable(source);
        }
        try {
            ServerPlayer player = source.getPlayerOrException();
            BankSession session = requireSession(source, player);
            if (session == null) {
                return 0;
            }
            CashAccountOperationResult result = cashOperations.withdraw(player, session, amount, idempotencyKey(player, "withdraw"));
            return cashResult(source, result, "commands.economia.atm.withdraw.success");
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao sacar pelo ATM.", exception);
            return unavailable(source);
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("commands.economia.player_only"));
            return 0;
        }
    }

    public int issueCard(CommandSourceStack source, CardType cardType, long creditLimit) {
        if (!EconomyDatabaseState.isAvailable()) {
            return unavailable(source);
        }
        try {
            ServerPlayer player = source.getPlayerOrException();
            BankSession session = requireSession(source, player);
            if (session == null) {
                return 0;
            }
            CardIssueResult result = cardIssueService.issue(new CardIssueRequest(session.accountId(), cardType, null, creditLimit));
            return cardResult(source, player, result);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao emitir cartao pelo ATM.", exception);
            return unavailable(source);
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("commands.economia.player_only"));
            return 0;
        }
    }

    private int cashResult(CommandSourceStack source, CashAccountOperationResult result, String successKey) {
        if (result.type() == CashAccountOperationResultType.COMPLETED) {
            source.sendSuccess(() -> Component.translatable(successKey, result.amount(), result.balanceAfter()), false);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.economia.atm.operation." + result.type().name().toLowerCase()));
        return 0;
    }

    private int cardResult(CommandSourceStack source, ServerPlayer player, CardIssueResult result) {
        if (result.type() != CardIssueResultType.ISSUED) {
            source.sendFailure(Component.translatable("commands.economia.atm.card." + result.type().name().toLowerCase()));
            return 0;
        }
        ItemStack stack = cardItemDataService.createCardStack(
                result.cardType(),
                result.cardId(),
                result.securityVersion(),
                result.accountNumber(),
                result.cardName(),
                result.individualCreditLimit()
        );
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.translatable("commands.economia.atm.card.success"), false);
        return 1;
    }

    private BankSession requireSession(CommandSourceStack source, ServerPlayer player) {
        BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
        if (session == null) {
            source.sendFailure(Component.translatable("commands.economia.session.required"));
        }
        return session;
    }

    private int unavailable(CommandSourceStack source) {
        source.sendFailure(Component.translatable("commands.economia.unavailable"));
        return 0;
    }

    private String idempotencyKey(ServerPlayer player, String action) {
        return "atm:" + action + ":" + player.getUUID() + ":" + UUID.randomUUID();
    }
}
