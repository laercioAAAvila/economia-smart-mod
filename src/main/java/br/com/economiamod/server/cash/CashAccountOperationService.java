package br.com.economiamod.server.cash;

import br.com.economiamod.common.money.BanknoteStackPlan;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.operation.EconomyOperationService;
import br.com.economiamod.server.operation.EconomyOperationState;
import br.com.economiamod.server.operation.EconomyOperationType;
import br.com.economiamod.server.operation.OperationStartResult;
import br.com.economiamod.server.operation.OperationStartType;
import br.com.economiamod.server.session.BankSession;
import br.com.economiamod.server.transaction.AccountFinancialService;
import br.com.economiamod.server.transaction.FinancialOperationResult;
import br.com.economiamod.server.transaction.FinancialOperationResultType;
import java.sql.SQLException;
import net.minecraft.server.level.ServerPlayer;

public final class CashAccountOperationService {
    private final CashInventoryService cashInventoryService;
    private final AccountFinancialService accountFinancialService;
    private final EconomyOperationService operationService;
    private final AccountQueryService accountQueryService = new AccountQueryService();

    public CashAccountOperationService(
            CashInventoryService cashInventoryService,
            AccountFinancialService accountFinancialService,
            EconomyOperationService operationService
    ) {
        this.cashInventoryService = cashInventoryService;
        this.accountFinancialService = accountFinancialService;
        this.operationService = operationService;
    }

    public CashAccountOperationResult depositAll(ServerPlayer player, BankSession session, String idempotencyKey) throws SQLException {
        long amount = cashInventoryService.totalBanknotes(player);
        if (amount <= 0L) {
            return CashAccountOperationResult.noMoney();
        }

        String payload = "amount=" + amount + ";account=" + session.accountId();
        OperationStartResult start = operationService.begin(idempotencyKey, EconomyOperationType.CASH_DEPOSIT,
                player.getUUID(), payload);
        if (start.type() == OperationStartType.DUPLICATE_COMPLETED) {
            return completedFromCurrentBalance(session, amount);
        }
        if (start.type() != OperationStartType.CREATED) {
            return CashAccountOperationResult.reconciliationRequired();
        }
        if (!operationService.mark(idempotencyKey, EconomyOperationState.ITEMS_RESERVED)) {
            operationService.markReconciliationRequired(idempotencyKey, "deposit could not enter reserved state");
            return CashAccountOperationResult.reconciliationRequired();
        }

        FinancialOperationResult financialResult = accountFinancialService.deposit(
                player.getUUID(), session.accountId(), amount, idempotencyKey);
        if (financialResult.type() == FinancialOperationResultType.DUPLICATE_COMPLETED
                || financialResult.type() == FinancialOperationResultType.IDEMPOTENCY_CONFLICT) {
            operationService.markReconciliationRequired(idempotencyKey, "deposit financial replay/conflict");
            return CashAccountOperationResult.reconciliationRequired();
        }
        if (financialResult.type() == FinancialOperationResultType.INACTIVE_ACCOUNT) {
            operationService.mark(idempotencyKey, EconomyOperationState.ROLLED_BACK);
            return CashAccountOperationResult.inactiveAccount();
        }
        if (financialResult.type() != FinancialOperationResultType.COMPLETED) {
            operationService.markReconciliationRequired(idempotencyKey, "unexpected deposit result=" + financialResult.type());
            return CashAccountOperationResult.reconciliationRequired();
        }

        operationService.mark(idempotencyKey, EconomyOperationState.SQL_COMMITTED);
        boolean removed = cashInventoryService.removeExactValue(player, amount);
        if (!removed) {
            operationService.markReconciliationRequired(idempotencyKey, "deposit credited but banknotes could not be removed");
            return CashAccountOperationResult.reconciliationRequired();
        }

        operationService.mark(idempotencyKey, EconomyOperationState.ITEMS_DELIVERED);
        operationService.mark(idempotencyKey, EconomyOperationState.COMPLETED);
        return CashAccountOperationResult.completed(amount, financialResult.balanceAfter());
    }

    public CashAccountOperationResult withdraw(ServerPlayer player, BankSession session, long amount, String idempotencyKey) throws SQLException {
        return withdraw(player, session, amount, null, idempotencyKey);
    }

    public CashAccountOperationResult withdraw(ServerPlayer player, BankSession session, long amount,
                                               Long banknoteValue, String idempotencyKey) throws SQLException {
        BanknoteStackPlan plan;
        try {
            plan = banknoteValue == null
                    ? cashInventoryService.buildWithdrawalPlan(amount)
                    : cashInventoryService.buildWithdrawalPlan(amount, banknoteValue);
        } catch (IllegalArgumentException exception) {
            return CashAccountOperationResult.invalidDenomination();
        }
        if (!cashInventoryService.canInsert(player, plan)) {
            return CashAccountOperationResult.insufficientInventorySpace();
        }

        String payload = "amount=" + amount + ";account=" + session.accountId()
                + ";banknote=" + (banknoteValue == null ? "auto" : banknoteValue);
        OperationStartResult start = operationService.begin(idempotencyKey, EconomyOperationType.CASH_WITHDRAW,
                player.getUUID(), payload);
        if (start.type() == OperationStartType.DUPLICATE_COMPLETED) {
            return completedFromCurrentBalance(session, amount);
        }
        if (start.type() != OperationStartType.CREATED) {
            return CashAccountOperationResult.reconciliationRequired();
        }
        if (!operationService.mark(idempotencyKey, EconomyOperationState.ITEMS_RESERVED)) {
            operationService.markReconciliationRequired(idempotencyKey, "withdraw could not enter reserved state");
            return CashAccountOperationResult.reconciliationRequired();
        }

        FinancialOperationResult financialResult = accountFinancialService.withdraw(
                player.getUUID(), session.accountId(), amount, idempotencyKey);
        if (financialResult.type() == FinancialOperationResultType.DUPLICATE_COMPLETED
                || financialResult.type() == FinancialOperationResultType.IDEMPOTENCY_CONFLICT) {
            operationService.markReconciliationRequired(idempotencyKey, "withdraw financial replay/conflict");
            return CashAccountOperationResult.reconciliationRequired();
        }
        if (financialResult.type() == FinancialOperationResultType.INSUFFICIENT_BALANCE) {
            operationService.mark(idempotencyKey, EconomyOperationState.ROLLED_BACK);
            return CashAccountOperationResult.insufficientBalance();
        }
        if (financialResult.type() == FinancialOperationResultType.INACTIVE_ACCOUNT) {
            operationService.mark(idempotencyKey, EconomyOperationState.ROLLED_BACK);
            return CashAccountOperationResult.inactiveAccount();
        }
        if (financialResult.type() != FinancialOperationResultType.COMPLETED) {
            operationService.markReconciliationRequired(idempotencyKey, "unexpected withdraw result=" + financialResult.type());
            return CashAccountOperationResult.reconciliationRequired();
        }

        operationService.mark(idempotencyKey, EconomyOperationState.SQL_COMMITTED);
        cashInventoryService.insert(player, plan);
        operationService.mark(idempotencyKey, EconomyOperationState.ITEMS_DELIVERED);
        operationService.mark(idempotencyKey, EconomyOperationState.COMPLETED);
        return CashAccountOperationResult.completed(amount, financialResult.balanceAfter());
    }

    private CashAccountOperationResult completedFromCurrentBalance(BankSession session, long amount) throws SQLException {
        long balance = accountQueryService.findBalanceSummary(session.accountId())
                .map(summary -> summary.balance())
                .orElse(0L);
        return CashAccountOperationResult.completed(amount, balance);
    }
}
