package br.com.economiamod.server.transaction;

import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.common.money.MoneyStackCalculator;
import br.com.economiamod.server.account.SystemAccountIds;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.operation.EconomyOperationService;
import br.com.economiamod.server.operation.EconomyOperationState;
import br.com.economiamod.server.operation.EconomyOperationType;
import br.com.economiamod.server.operation.OperationStartResult;
import br.com.economiamod.server.operation.OperationStartType;
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
    private final EconomyOperationService operations = new EconomyOperationService();
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

    /**
     * Cash is a cross-resource operation (SQL + Minecraft inventory), so a completed SQL transaction
     * must never blindly clear the current menu contents on replay. The operation row tracks the
     * physical phase and any ambiguous crash is left for reconciliation instead of guessing.
     */
    private MenuPaymentResult payCash(ServerPlayer player, Container cash, long amount, String key)
            throws SQLException {
        String transactionKey = IdempotencyKeys.requireValid(key);
        String operationKey = "cash-payment:" + RequestFingerprint.of(transactionKey);
        String payload = "amount=" + amount + ";treasury=" + SystemAccountIds.TREASURY
                + ";transaction=" + RequestFingerprint.of(transactionKey);

        OperationStartResult start = operations.begin(operationKey, EconomyOperationType.CASH_PAYMENT,
                player.getUUID(), payload);
        if (start.type() == OperationStartType.DUPLICATE_COMPLETED) {
            // The original payment already consumed its original banknotes. Never touch whatever
            // happens to be in the cash slots now.
            return MenuPaymentResult.completed();
        }
        if (start.type() == OperationStartType.CONFLICT) {
            return MenuPaymentResult.denied("idempotency_conflict");
        }
        if (start.type() != OperationStartType.CREATED) {
            return MenuPaymentResult.denied("reconciliation_required");
        }

        long inserted = moneyIn(cash);
        if (inserted != amount) {
            operations.mark(operationKey, EconomyOperationState.ROLLED_BACK);
            return MenuPaymentResult.denied(inserted < amount ? "insufficient_cash" : "exact_cash_required");
        }
        if (!operations.mark(operationKey, EconomyOperationState.ITEMS_RESERVED)) {
            operations.markReconciliationRequired(operationKey, "unable to reserve cash operation state");
            return MenuPaymentResult.denied("reconciliation_required");
        }

        String fingerprint = RequestFingerprint.of(EconomyTransactionType.DEPOSIT, player.getUUID(),
                null, SystemAccountIds.TREASURY, amount, "CASH_PAYMENT");
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                IdempotencyCheck idempotency = paymentTransactions.idempotencyCheck(
                        connection, transactionKey, fingerprint);
                if (idempotency != IdempotencyCheck.ABSENT) {
                    operations.markReconciliationRequired(connection, operationKey,
                            idempotency == IdempotencyCheck.MATCH
                                    ? "cash transaction already completed before physical reconciliation"
                                    : "cash transaction idempotency conflict");
                    connection.commit();
                    return MenuPaymentResult.denied(idempotency == IdempotencyCheck.CONFLICT
                            ? "idempotency_conflict" : "reconciliation_required");
                }

                accounts.lockAccountsOrdered(connection, SystemAccountIds.TREASURY, SystemAccountIds.TREASURY);
                PaymentAccountSnapshot treasury = accounts.findPaymentAccount(connection, SystemAccountIds.TREASURY)
                        .orElse(null);
                if (treasury == null || !"ACTIVE".equals(treasury.status())) {
                    connection.rollback();
                    operations.mark(operationKey, EconomyOperationState.ROLLED_BACK);
                    return MenuPaymentResult.denied("inactive_account");
                }

                UUID transactionId = UUID.randomUUID();
                long balanceAfter = Math.addExact(treasury.balance(), amount);
                accounts.updateBalance(connection, SystemAccountIds.TREASURY, balanceAfter);
                accountTransactions.insertTransaction(connection, transactionId, transactionKey,
                        EconomyTransactionType.DEPOSIT, amount, player.getUUID(), null,
                        SystemAccountIds.TREASURY, fingerprint, TransactionOrigin.MINECRAFT);
                accountTransactions.insertLedger(connection, transactionId, SystemAccountIds.TREASURY,
                        LedgerEntryType.CREDIT, amount, treasury.balance(), balanceAfter);

                // Keep the SQL payment and operation phase in the same database transaction.
                if (!operations.mark(connection, operationKey, EconomyOperationState.SQL_COMMITTED)) {
                    connection.rollback();
                    operations.mark(operationKey, EconomyOperationState.ROLLED_BACK);
                    return MenuPaymentResult.denied("reconciliation_required");
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }

        // Only the banknote stacks that were validated are consumed. A malformed/non-money item in
        // the menu is never deleted as a side effect of payment processing.
        clearBanknotes(cash);
        boolean delivered = operations.mark(operationKey, EconomyOperationState.ITEMS_DELIVERED);
        if (!delivered || !operations.mark(operationKey, EconomyOperationState.COMPLETED)) {
            operations.markReconciliationRequired(operationKey,
                    "cash was consumed after SQL commit but operation state could not be finalized");
        }
        return MenuPaymentResult.completed();
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

    private void clearBanknotes(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (MoneyStackCalculator.banknoteValue(stack).isPresent()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        container.setChanged();
    }
}
