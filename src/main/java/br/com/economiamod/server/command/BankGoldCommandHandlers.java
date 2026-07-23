package br.com.economiamod.server.command;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.server.gold.GoldExchangeResult;
import br.com.economiamod.server.gold.GoldExchangeResultType;
import br.com.economiamod.server.gold.GoldExchangeService;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import br.com.economiamod.server.session.BankSession;
import br.com.economiamod.server.session.BankSessionService;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BankGoldCommandHandlers {
    private static final int MAX_STACK_SIZE = 64;

    private final BankCounterAccessService bankCounterAccessService = new BankCounterAccessService();
    private final GoldExchangeService goldExchangeService = new GoldExchangeService();

    public int mintSelectedGold(CommandSourceStack source) {
        if (!EconomyDatabaseState.isAvailable()) {
            return unavailable(source);
        }
        try {
            ServerPlayer player = source.getPlayerOrException();
            if (!bankCounterAccessService.hasValidAccess(source, player)) {
                return 0;
            }
            BankSession session = requireSession(source, player);
            if (session == null) {
                return 0;
            }
            ItemStack stack = player.getMainHandItem();
            GoldExchangeResult result = goldExchangeService.mintToAccount(player.getUUID(), session.accountId(), stack.copy(), null, idempotencyKey(player, "mint"));
            if (result.type() == GoldExchangeResultType.COMPLETED) {
                stack.shrink(stack.getCount());
            }
            return showResult(source, result, "commands.economia.bank.gold.mint.success");
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao emitir moeda por ouro.", exception);
            return unavailable(source);
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("commands.economia.player_only"));
            return 0;
        }
    }

    public int redeemGold(CommandSourceStack source, int goldUnits) {
        return redeemGold(source, goldUnits, GoldRedeemUnit.NUGGET);
    }

    public int redeemGold(CommandSourceStack source, int amount, GoldRedeemUnit unit) {
        if (!EconomyDatabaseState.isAvailable()) {
            return unavailable(source);
        }
        try {
            ServerPlayer player = source.getPlayerOrException();
            if (!bankCounterAccessService.hasValidAccess(source, player)) {
                return 0;
            }
            BankSession session = requireSession(source, player);
            if (session == null) {
                return 0;
            }
            long goldUnits = Math.multiplyExact(amount, unit.nuggetUnits);
            GoldExchangeResult result = goldExchangeService.redeemFromAccount(player.getUUID(), session.accountId(), goldUnits, null, idempotencyKey(player, "redeem"));
            if (result.type() == GoldExchangeResultType.COMPLETED) {
                giveGold(player, amount, unit.item);
            }
            return showResult(source, result, "commands.economia.bank.gold.redeem.success");
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao resgatar ouro.", exception);
            return unavailable(source);
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("commands.economia.player_only"));
            return 0;
        }
    }

    private int showResult(CommandSourceStack source, GoldExchangeResult result, String successKey) {
        if (result.type() == GoldExchangeResultType.COMPLETED || result.type() == GoldExchangeResultType.DUPLICATE_COMPLETED) {
            source.sendSuccess(() -> Component.translatable(successKey, result.goldNuggetUnits(), result.moneyAmount(), result.balanceAfter()), false);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.economia.bank.gold." + result.type().name().toLowerCase()));
        return 0;
    }

    private BankSession requireSession(CommandSourceStack source, ServerPlayer player) {
        BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
        if (session == null) {
            source.sendFailure(Component.translatable("commands.economia.session.required"));
        }
        return session;
    }

    private void giveGold(ServerPlayer player, int amount, net.minecraft.world.item.Item item) {
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = new ItemStack(item, Math.min(remaining, MAX_STACK_SIZE));
            remaining -= stack.getCount();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private int unavailable(CommandSourceStack source) {
        source.sendFailure(Component.translatable("commands.economia.unavailable"));
        return 0;
    }

    private String idempotencyKey(ServerPlayer player, String action) {
        return "bank-gold:" + action + ":" + player.getUUID() + ":" + UUID.randomUUID();
    }

    public enum GoldRedeemUnit {
        NUGGET(1L, Items.GOLD_NUGGET),
        INGOT(9L, Items.GOLD_INGOT),
        BLOCK(81L, Items.GOLD_BLOCK);

        private final long nuggetUnits;
        private final net.minecraft.world.item.Item item;

        GoldRedeemUnit(long nuggetUnits, net.minecraft.world.item.Item item) {
            this.nuggetUnits = nuggetUnits;
            this.item = item;
        }
    }
}
