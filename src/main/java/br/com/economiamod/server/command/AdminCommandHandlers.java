package br.com.economiamod.server.command;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.server.job.DailyJobRunResult;
import br.com.economiamod.server.job.DailyJobRunResultType;
import br.com.economiamod.server.job.DailyJobService;
import br.com.economiamod.server.persistence.DatabaseResetService;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AdminCommandHandlers {
    public int giveAtm(CommandSourceStack source, ServerPlayer target) {
        return giveItem(source, target, new ItemStack(ModBlocks.ATM_ITEM.get()));
    }

    public int giveSellShop(CommandSourceStack source, ServerPlayer target) {
        return giveItem(source, target, new ItemStack(ModBlocks.SELL_SHOP_ITEM.get()));
    }

    public int giveBuyShop(CommandSourceStack source, ServerPlayer target) {
        return giveItem(source, target, new ItemStack(ModBlocks.BUY_SHOP_ITEM.get()));
    }

    public int giveBankCounter(CommandSourceStack source, ServerPlayer target) {
        return giveItem(source, target, new ItemStack(ModBlocks.BANK_COUNTER_ITEM.get()));
    }

    private int giveItem(CommandSourceStack source, ServerPlayer target, ItemStack stack) {
        boolean added = target.getInventory().add(stack);
        if (!added) {
            target.drop(stack, false);
        }

        source.sendSuccess(() -> Component.translatable(
                "commands.economia.admin.item.give",
                stack.getHoverName(),
                target.getGameProfile().getName()
        ), true);
        return 1;
    }

    public int processInterest(CommandSourceStack source, String dateText) {
        if (!EconomyDatabaseState.isAvailable()) {
            source.sendFailure(Component.translatable("commands.economia.unavailable"));
            return 0;
        }

        LocalDate businessDate;
        try {
            businessDate = LocalDate.parse(dateText);
        } catch (DateTimeParseException exception) {
            source.sendFailure(Component.translatable("commands.economia.admin.interest.invalid_date"));
            return 0;
        }

        try {
            DailyJobRunResult result = new DailyJobService().runCreditInterest(businessDate);
            return showInterestResult(source, businessDate, result);
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao processar juros diarios.", exception);
            source.sendFailure(Component.translatable("commands.economia.admin.interest.failed"));
            return 0;
        }
    }

    private int showInterestResult(CommandSourceStack source, LocalDate businessDate, DailyJobRunResult result) {
        if (result.type() == DailyJobRunResultType.ALREADY_COMPLETED) {
            source.sendSuccess(() -> Component.translatable("commands.economia.admin.interest.already_completed", businessDate), true);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable(
                "commands.economia.admin.interest.completed",
                businessDate,
                result.interestResult().processedCards(),
                result.interestResult().skippedCards(),
                result.interestResult().interestAmount()
        ), true);
        return 1;
    }

    public int resetDatabase(CommandSourceStack source) {
        try {
            int migrations = new DatabaseResetService().reset();
            source.sendSuccess(() -> Component.translatable("commands.economia.admin.database.reset.success", migrations), true);
            return 1;
        } catch (IOException | SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao resetar banco de dados.", exception);
            source.sendFailure(Component.translatable("commands.economia.admin.database.reset.failed"));
            return 0;
        }
    }
}
