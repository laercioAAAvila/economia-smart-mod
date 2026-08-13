package br.com.economiamod.server.transaction;

import br.com.economiamod.server.card.CardValidationService;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class CardPaymentService {
    private final DebitPaymentService debitPaymentService;
    private final CreditPaymentService creditPaymentService;

    public CardPaymentService(CardValidationService cardValidationService) {
        PaymentAccountRepository accountRepository = new PaymentAccountRepository();
        CardCreditRepository cardCreditRepository = new CardCreditRepository();
        PaymentTransactionWriter transactionWriter = new PaymentTransactionWriter();
        this.debitPaymentService = new DebitPaymentService(cardValidationService, accountRepository, transactionWriter);
        this.creditPaymentService = new CreditPaymentService(cardValidationService, accountRepository, cardCreditRepository, transactionWriter);
    }

    public DebitPurchaseResult debitPurchase(
            ItemStack cardStack,
            UUID destinationAccountId,
            long amount,
            UUID initiatorPlayerUuid,
            String idempotencyKey
    ) throws SQLException {
        return debitPaymentService.debitPurchase(cardStack, destinationAccountId, amount, initiatorPlayerUuid, idempotencyKey);
    }

    public DebitPurchaseResult debitPurchase(
            Connection connection,
            ItemStack cardStack,
            UUID destinationAccountId,
            long amount,
            UUID initiatorPlayerUuid,
            String idempotencyKey
    ) throws SQLException {
        return debitPaymentService.debitPurchase(connection, cardStack, destinationAccountId,
                amount, initiatorPlayerUuid, idempotencyKey);
    }

    public CreditPurchaseResult creditPurchase(
            ItemStack cardStack,
            UUID destinationAccountId,
            long amount,
            UUID initiatorPlayerUuid,
            String merchantName,
            String idempotencyKey
    ) throws SQLException {
        return creditPaymentService.creditPurchase(cardStack, destinationAccountId, amount, initiatorPlayerUuid, merchantName, idempotencyKey);
    }
}
