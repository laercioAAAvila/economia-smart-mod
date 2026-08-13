package br.com.economiamod.server.transaction;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.credit.CreditMath;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class DebitPaymentService {
    private final CardValidationService cardValidationService;
    private final PaymentAccountRepository accountRepository;
    private final PaymentTransactionWriter transactionWriter;

    public DebitPaymentService(CardValidationService cardValidationService, PaymentAccountRepository accountRepository, PaymentTransactionWriter transactionWriter) {
        this.cardValidationService = cardValidationService;
        this.accountRepository = accountRepository;
        this.transactionWriter = transactionWriter;
    }

    public DebitPurchaseResult debitPurchase(ItemStack cardStack, UUID destinationAccountId, long amount, UUID playerUuid, String idempotencyKey) throws SQLException {
        requirePositive(amount);
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                DebitPurchaseResult result = debitPurchase(
                        connection, cardStack, destinationAccountId, amount, playerUuid, idempotencyKey);
                if (result.type() == DebitPurchaseResultType.COMPLETED
                        || result.type() == DebitPurchaseResultType.DUPLICATE_COMPLETED) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public DebitPurchaseResult debitPurchase(Connection connection, ItemStack cardStack,
                                              UUID destinationAccountId, long amount,
                                              UUID playerUuid, String idempotencyKey) throws SQLException {
        requirePositive(amount);
        CardValidationResult card = cardValidationService.validate(connection, cardStack);
        if (card.type() != CardValidationResultType.VALID) {
            return DebitPurchaseResult.invalidCard();
        }
        if (!card.cardType().hasDebit()) {
            return DebitPurchaseResult.debitNotAllowed();
        }
        if (card.accountId().equals(destinationAccountId)) {
            return DebitPurchaseResult.completed();
        }
        if (transactionWriter.completedTransactionExists(connection, idempotencyKey)) {
            return DebitPurchaseResult.duplicateCompleted();
        }

        accountRepository.lockAccountsOrdered(connection, card.accountId(), destinationAccountId);
        DebitDailyLimitState dailyLimit = lockDebitDailyLimit(connection, card.cardId());
        PaymentAccountSnapshot source = accountRepository.findPaymentAccount(connection, card.accountId()).orElse(null);
        PaymentAccountSnapshot destination = accountRepository.findPaymentAccount(connection, destinationAccountId).orElse(null);
        if (!active(source) || !active(destination)) {
            return DebitPurchaseResult.inactiveAccount();
        }

        long available = CreditMath.availableBalance(
                source.balance(), source.principalOutstanding(), source.interestOutstanding());
        if (available < amount) {
            return DebitPurchaseResult.insufficientBalance();
        }
        if (!dailyLimitAllows(dailyLimit, amount)) {
            return DebitPurchaseResult.dailyLimitReached();
        }

        UUID transactionId = UUID.randomUUID();
        long sourceAfter = source.balance() - amount;
        long destinationAfter = Math.addExact(destination.balance(), amount);
        updateDebitDailySpent(connection, card.cardId(), dailyLimit, amount);
        accountRepository.updateBalance(connection, card.accountId(), sourceAfter);
        accountRepository.updateBalance(connection, destinationAccountId, destinationAfter);
        transactionWriter.insertDebitTransaction(connection, transactionId, idempotencyKey, amount,
                playerUuid, card.accountId(), destinationAccountId, card.cardId());
        transactionWriter.insertLedger(connection, transactionId, card.accountId(),
                LedgerEntryType.DEBIT, amount, source.balance(), sourceAfter);
        transactionWriter.insertLedger(connection, transactionId, destinationAccountId,
                LedgerEntryType.CREDIT, amount, destination.balance(), destinationAfter);
        return DebitPurchaseResult.completed();
    }

    private boolean active(PaymentAccountSnapshot account) {
        return account != null && AccountStatus.ACTIVE.name().equals(account.status());
    }

    private void requirePositive(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private DebitDailyLimitState lockDebitDailyLimit(Connection connection, UUID cardId) throws SQLException {
        String sql = """
                SELECT debit_daily_limit,
                       debit_daily_spent,
                       debit_daily_spent_on
                  FROM economy_cards
                 WHERE id = ?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new DebitDailyLimitState(0L, 0L, null);
                }
                return new DebitDailyLimitState(
                        resultSet.getLong("debit_daily_limit"),
                        resultSet.getLong("debit_daily_spent"),
                        resultSet.getObject("debit_daily_spent_on", LocalDate.class)
                );
            }
        }
    }

    private boolean dailyLimitAllows(DebitDailyLimitState state, long amount) {
        if (state.limit() <= 0L) {
            return true;
        }
        LocalDate today = LocalDate.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
        long spent = today.equals(state.spentOn()) ? state.spent() : 0L;
        return spent <= state.limit() && amount <= state.limit() - spent;
    }

    private void updateDebitDailySpent(Connection connection, UUID cardId, DebitDailyLimitState state, long amount) throws SQLException {
        if (state.limit() <= 0L) {
            return;
        }
        LocalDate today = LocalDate.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
        long spent = today.equals(state.spentOn()) ? state.spent() : 0L;
        String sql = """
                UPDATE economy_cards
                   SET debit_daily_spent = ?,
                       debit_daily_spent_on = ?,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, spent + amount);
            statement.setObject(2, today);
            statement.setObject(3, cardId);
            statement.executeUpdate();
        }
    }

    private record DebitDailyLimitState(long limit, long spent, LocalDate spentOn) {
    }
}
