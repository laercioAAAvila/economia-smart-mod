package br.com.economiamod.server.shop.buy;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.card.CardStatus;
import br.com.economiamod.common.credit.CreditLimitPolicy;
import br.com.economiamod.common.credit.CreditMath;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.CreditPurchaseResult;
import br.com.economiamod.server.transaction.LedgerEntryType;
import br.com.economiamod.server.transaction.PaymentAccountRepository;
import br.com.economiamod.server.transaction.PaymentAccountSnapshot;
import br.com.economiamod.server.transaction.PaymentTransactionWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public final class BuyShopFundingCreditService {
    private final FundingCardRepository cardRepository = new FundingCardRepository();
    private final PaymentAccountRepository accountRepository = new PaymentAccountRepository();
    private final PaymentTransactionWriter transactionWriter = new PaymentTransactionWriter();

    public CreditPurchaseResult charge(UUID cardId, long amount, UUID initiatorPlayerUuid, String merchantName, String idempotencyKey) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (transactionWriter.completedTransactionExists(connection, idempotencyKey)) {
                    connection.commit();
                    return CreditPurchaseResult.duplicateCompleted();
                }

                cardRepository.lockCard(connection, cardId);
                FundingCardSnapshot card = cardRepository.find(connection, cardId).orElse(null);
                if (card == null || !card.cardType().hasCredit()) {
                    connection.rollback();
                    return CreditPurchaseResult.creditNotAllowed();
                }

                accountRepository.lockAccountsOrdered(connection, card.accountId(), card.accountId());
                PaymentAccountSnapshot account = accountRepository.findPaymentAccount(connection, card.accountId()).orElse(null);
                if (!active(account) || !CardStatus.ACTIVE.name().equals(card.cardStatus())) {
                    connection.rollback();
                    return CreditPurchaseResult.inactiveAccount();
                }
                if (!hasAvailableCredit(account, card, amount)) {
                    connection.rollback();
                    return CreditPurchaseResult.insufficientCredit();
                }

                UUID transactionId = UUID.randomUUID();
                accountRepository.increaseCreditPrincipal(connection, card.accountId(), card.cardId(), amount);
                transactionWriter.insertCreditTransaction(connection, transactionId, idempotencyKey, amount, initiatorPlayerUuid, card.accountId(), null, card.cardId());
                transactionWriter.insertLedger(connection, transactionId, card.accountId(), LedgerEntryType.CREDIT_PRINCIPAL_INCREASE, amount, account.balance(), account.balance());
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

    private boolean active(PaymentAccountSnapshot account) {
        return account != null && AccountStatus.ACTIVE.name().equals(account.status());
    }

    private boolean hasAvailableCredit(PaymentAccountSnapshot account, FundingCardSnapshot card, long amount) {
        long accountDebtAfter = CreditMath.debtTotal(account.principalOutstanding(), account.interestOutstanding()) + amount;
        long cardDebtAfter = CreditMath.debtTotal(card.cardPrincipalOutstanding(), card.cardInterestOutstanding()) + amount;
        long effectiveAccountLimit = CreditLimitPolicy.effectiveLimit(account.balance(), account.configuredCreditLimit());
        return accountDebtAfter <= effectiveAccountLimit
                && accountDebtAfter <= account.balance()
                && cardDebtAfter <= card.individualCreditLimit();
    }
}
