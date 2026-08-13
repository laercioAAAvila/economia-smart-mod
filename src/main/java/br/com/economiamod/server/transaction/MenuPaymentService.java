package br.com.economiamod.server.transaction;

import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.common.money.MoneyStackCalculator;
import br.com.economiamod.server.account.SystemAccountIds;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class MenuPaymentService {
    private final PaymentTransactionWriter paymentTransactions = new PaymentTransactionWriter();
    private final PaymentAccountRepository accounts = new PaymentAccountRepository();
    private final AccountTransactionWriter accountTransactions = new AccountTransactionWriter();
    private final CardPaymentService cards = new CardPaymentService(
            new CardValidationService(new CardItemDataService()));

    public MenuPaymentResult pay(ServerPlayer player, DirectPaymentMethod method, ItemStack card,
                                 Container cash, long amount, String merchant, String idempotencyKey)
            throws SQLException {
        if (amount <= 0L) {
            return MenuPaymentResult.denied("invalid_amount");
        }
        if (method == DirectPaymentMethod.CASH) {
            return payCash(player, cash, amount, idempotencyKey);
        }
        if (card == null || card.isEmpty()) {
            return MenuPaymentResult.denied("card_required");
        }
        if (method == DirectPaymentMethod.CREDIT) {
            CreditPurchaseResult result = cards.creditPurchase(card, SystemAccountIds.TREASURY, amount,
                    player.getUUID(), merchant, idempotencyKey);
            return result.type() == CreditPurchaseResultType.COMPLETED
                    || result.type() == CreditPurchaseResultType.DUPLICATE_COMPLETED
                    ? MenuPaymentResult.completed()
                    : MenuPaymentResult.denied(result.type().name().toLowerCase(java.util.Locale.ROOT));
        }
        DebitPurchaseResult result = cards.debitPurchase(card, SystemAccountIds.TREASURY, amount,
                player.getUUID(), idempotencyKey);
        return result.type() == DebitPurchaseResultType.COMPLETED
                || result.type() == DebitPurchaseResultType.DUPLICATE_COMPLETED
                ? MenuPaymentResult.completed()
                : MenuPaymentResult.denied(result.type().name().toLowerCase(java.util.Locale.ROOT));
    }

    public MenuPaymentResult payDebit(Connection connection, ServerPlayer player, ItemStack card,
                                      long amount, String idempotencyKey) throws SQLException {
        if (amount <= 0L) {
            return MenuPaymentResult.denied("invalid_amount");
        }
        if (card == null || card.isEmpty()) {
            return MenuPaymentResult.denied("card_required");
        }
        DebitPurchaseResult result = cards.debitPurchase(connection, card, SystemAccountIds.TREASURY,
                amount, player.getUUID(), idempotencyKey);
        return result.type() == DebitPurchaseResultType.COMPLETED
                || result.type() == DebitPurchaseResultType.DUPLICATE_COMPLETED
                ? MenuPaymentResult.completed()
                : MenuPaymentResult.denied(result.type().name().toLowerCase(java.util.Locale.ROOT));
    }

    private MenuPaymentResult payCash(ServerPlayer player, Container cash, long amount, String key)
            throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            if (paymentTransactions.completedTransactionExists(connection, key)) {
                cash.clearContent();
                return MenuPaymentResult.completed();
            }
        }
        long inserted = moneyIn(cash);
        if (inserted != amount) {
            return MenuPaymentResult.denied(inserted < amount ? "insufficient_cash" : "exact_cash_required");
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (paymentTransactions.completedTransactionExists(connection, key)) {
                    connection.commit();
                    cash.clearContent();
                    return MenuPaymentResult.completed();
                }
                accounts.lockAccountsOrdered(connection, SystemAccountIds.TREASURY, SystemAccountIds.TREASURY);
                PaymentAccountSnapshot treasury = accounts.findPaymentAccount(connection, SystemAccountIds.TREASURY)
                        .orElse(null);
                if (treasury == null || !"ACTIVE".equals(treasury.status())) {
                    connection.rollback();
                    return MenuPaymentResult.denied("inactive_account");
                }
                UUID transactionId = UUID.randomUUID();
                long balanceAfter = Math.addExact(treasury.balance(), amount);
                accounts.updateBalance(connection, SystemAccountIds.TREASURY, balanceAfter);
                accountTransactions.insertTransaction(connection, transactionId, key,
                        EconomyTransactionType.DEPOSIT, amount, player.getUUID(), null, SystemAccountIds.TREASURY);
                accountTransactions.insertLedger(connection, transactionId, SystemAccountIds.TREASURY,
                        LedgerEntryType.CREDIT, amount, treasury.balance(), balanceAfter);
                connection.commit();
                cash.clearContent();
                return MenuPaymentResult.completed();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private long moneyIn(Container container) {
        long total = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            var value = MoneyStackCalculator.banknoteValue(stack);
            if (value.isPresent()) {
                total = Math.addExact(total, Math.multiplyExact(value.getAsLong(), stack.getCount()));
            }
        }
        return total;
    }
}
