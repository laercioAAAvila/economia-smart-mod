package br.com.economiamod.server.cash;

import br.com.economiamod.common.money.BanknoteStackPlan;
import br.com.economiamod.server.operation.EconomyOperationService;
import br.com.economiamod.server.operation.EconomyOperationState;
import br.com.economiamod.server.operation.EconomyOperationType;
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

        operationService.createIfMissing(idempotencyKey, EconomyOperationType.CASH_DEPOSIT, player.getUUID(), "amount=" + amount);
        operationService.mark(idempotencyKey, EconomyOperationState.ITEMS_RESERVED);

        FinancialOperationResult financialResult = accountFinancialService.deposit(player.getUUID(), session.accountId(), amount, idempotencyKey);
        if (financialResult.type() == FinancialOperationResultType.DUPLICATE_COMPLETED) {
            return CashAccountOperationResult.completed(amount, financialResult.balanceAfter());
        }
        if (financialResult.type() == FinancialOperationResultType.INACTIVE_ACCOUNT) {
            operationService.mark(idempotencyKey, EconomyOperationState.ROLLBACK_REQUIRED);
            return CashAccountOperationResult.inactiveAccount();
        }

        operationService.mark(idempotencyKey, EconomyOperationState.SQL_COMMITTED);
        boolean removed = cashInventoryService.removeExactValue(player, amount);
        if (!removed) {
            operationService.mark(idempotencyKey, EconomyOperationState.ROLLBACK_REQUIRED);
            return CashAccountOperationResult.noMoney();
        }

        operationService.mark(idempotencyKey, EconomyOperationState.COMPLETED);
        return CashAccountOperationResult.completed(amount, financialResult.balanceAfter());
    }

    public CashAccountOperationResult withdraw(ServerPlayer player, BankSession session, long amount, String idempotencyKey) throws SQLException {
        return withdraw(player, session, amount, null, idempotencyKey);
    }

    public CashAccountOperationResult withdraw(ServerPlayer player, BankSession session, long amount, Long banknoteValue, String idempotencyKey) throws SQLException {
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

        operationService.createIfMissing(idempotencyKey, EconomyOperationType.CASH_WITHDRAW, player.getUUID(), "amount=" + amount);

        FinancialOperationResult financialResult = accountFinancialService.withdraw(player.getUUID(), session.accountId(), amount, idempotencyKey);
        if (financialResult.type() == FinancialOperationResultType.DUPLICATE_COMPLETED) {
            return CashAccountOperationResult.completed(amount, financialResult.balanceAfter());
        }
        if (financialResult.type() == FinancialOperationResultType.INSUFFICIENT_BALANCE) {
            operationService.mark(idempotencyKey, EconomyOperationState.ROLLBACK_REQUIRED);
            return CashAccountOperationResult.insufficientBalance();
        }
        if (financialResult.type() == FinancialOperationResultType.INACTIVE_ACCOUNT) {
            operationService.mark(idempotencyKey, EconomyOperationState.ROLLBACK_REQUIRED);
            return CashAccountOperationResult.inactiveAccount();
        }

        operationService.mark(idempotencyKey, EconomyOperationState.SQL_COMMITTED);
        cashInventoryService.insert(player, plan);
        operationService.mark(idempotencyKey, EconomyOperationState.ITEMS_DELIVERED);
        operationService.mark(idempotencyKey, EconomyOperationState.COMPLETED);
        return CashAccountOperationResult.completed(amount, financialResult.balanceAfter());
    }
}
