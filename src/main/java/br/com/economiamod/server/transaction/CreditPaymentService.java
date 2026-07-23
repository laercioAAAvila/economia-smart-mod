package br.com.economiamod.server.transaction;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.card.CardStatus;
import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.common.credit.CreditMath;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class CreditPaymentService {
    private final CardValidationService cardValidationService;
    private final PaymentAccountRepository accountRepository;
    private final CardCreditRepository cardRepository;
    private final PaymentTransactionWriter transactionWriter;

    public CreditPaymentService(CardValidationService cardValidationService, PaymentAccountRepository accountRepository, CardCreditRepository cardRepository, PaymentTransactionWriter transactionWriter) {
        this.cardValidationService = cardValidationService;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.transactionWriter = transactionWriter;
    }

    public CreditPurchaseResult creditPurchase(ItemStack cardStack, UUID destinationAccountId, long amount, UUID playerUuid, String merchantName, String idempotencyKey) throws SQLException {
        requirePositive(amount);

        CardValidationResult card = cardValidationService.validate(cardStack);
        if (card.type() != CardValidationResultType.VALID) {
            return CreditPurchaseResult.invalidCard();
        }
        if (!card.cardType().hasCredit()) {
            return CreditPurchaseResult.creditNotAllowed();
        }
        if (card.accountId().equals(destinationAccountId)) {
            return CreditPurchaseResult.completed();
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (transactionWriter.completedTransactionExists(connection, idempotencyKey)) {
                    connection.commit();
                    return CreditPurchaseResult.duplicateCompleted();
                }

                accountRepository.lockAccountsOrdered(connection, card.accountId(), destinationAccountId);
                cardRepository.lockCard(connection, card.cardId());

                PaymentAccountSnapshot source = accountRepository.findPaymentAccount(connection, card.accountId()).orElse(null);
                PaymentAccountSnapshot destination = accountRepository.findPaymentAccount(connection, destinationAccountId).orElse(null);
                CardCreditSnapshot cardState = cardRepository.findCard(connection, card.cardId()).orElse(null);
                if (!active(source) || !active(destination) || !active(cardState)) {
                    connection.rollback();
                    return CreditPurchaseResult.inactiveAccount();
                }
                if (!cardState.cardType().hasCredit()) {
                    connection.rollback();
                    return CreditPurchaseResult.creditNotAllowed();
                }
                if (!hasAvailableCredit(source, cardState, amount)) {
                    connection.rollback();
                    return CreditPurchaseResult.insufficientCredit();
                }

                UUID transactionId = UUID.randomUUID();
                long destinationAfter = Math.addExact(destination.balance(), amount);
                accountRepository.increaseCreditPrincipal(connection, card.accountId(), card.cardId(), amount);
                accountRepository.updateBalance(connection, destinationAccountId, destinationAfter);
                transactionWriter.insertCreditTransaction(connection, transactionId, idempotencyKey, amount, playerUuid, card.accountId(), destinationAccountId, card.cardId());
                transactionWriter.insertLedger(connection, transactionId, card.accountId(), LedgerEntryType.CREDIT_PRINCIPAL_INCREASE, amount, source.balance(), source.balance());
                transactionWriter.insertLedger(connection, transactionId, destinationAccountId, LedgerEntryType.CREDIT, amount, destination.balance(), destinationAfter);
                transactionWriter.insertCardPurchaseEntry(connection, transactionId, card.cardId(), amount, merchantName);
                connection.commit();
                return CreditPurchaseResult.completed();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private boolean hasAvailableCredit(PaymentAccountSnapshot source, CardCreditSnapshot cardState, long amount) {
        long accountDebtAfter = CreditMath.debtTotal(source.principalOutstanding(), source.interestOutstanding()) + amount;
        long cardDebtAfter = CreditMath.debtTotal(cardState.principalOutstanding(), cardState.interestOutstanding()) + amount;
        long effectiveAccountLimit = CreditLimitPolicy.effectiveLimit(source.balance(), source.configuredCreditLimit());
        return accountDebtAfter <= effectiveAccountLimit
                && accountDebtAfter <= source.balance()
                && cardDebtAfter <= cardState.individualCreditLimit();
    }

    private boolean active(PaymentAccountSnapshot account) {
        return account != null && AccountStatus.ACTIVE.name().equals(account.status());
    }

    private boolean active(CardCreditSnapshot card) {
        return card != null && CardStatus.ACTIVE.name().equals(card.status());
    }

    private void requirePositive(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
