package br.com.economiamod.server.shop.sell;

import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.commercial.CommercialAccountLinkRepository;
import br.com.economiamod.server.commercial.CommercialAccountLinks;
import br.com.economiamod.server.offer.BankOfferPrice;
import br.com.economiamod.server.offer.BankOfferPriceService;
import br.com.economiamod.server.offer.BankOfferReadRepository;
import br.com.economiamod.server.offer.BankOfferSnapshot;
import br.com.economiamod.server.offer.BankOfferStatsRepository;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.player.PlayerItemInventoryService;
import br.com.economiamod.server.shop.ShopTransactionResult;
import br.com.economiamod.server.shop.ShopTransactionResultType;
import br.com.economiamod.server.transaction.CardPaymentService;
import br.com.economiamod.server.transaction.CreditPurchaseResult;
import br.com.economiamod.server.transaction.CreditPurchaseResultType;
import br.com.economiamod.server.transaction.DebitPurchaseResult;
import br.com.economiamod.server.transaction.DebitPurchaseResultType;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class SellShopCardSaleService {
    private final CommercialAccountLinkRepository linkRepository = new CommercialAccountLinkRepository();
    private final BankOfferReadRepository offerRepository = new BankOfferReadRepository();
    private final BankOfferPriceService priceService = new BankOfferPriceService();
    private final BankOfferStatsRepository statsRepository = new BankOfferStatsRepository();
    private final SellShopStockService stockService = new SellShopStockService();
    private final PlayerItemInventoryService playerItems = new PlayerItemInventoryService();
    private final CardPaymentService cardPaymentService = new CardPaymentService(new CardValidationService(new CardItemDataService()));

    public ShopTransactionResult buyWithDebit(ServerPlayer player, UUID commercialBlockId, int offerSlot, ItemStack cardStack, String idempotencyKey, LocalDate businessDate) throws SQLException {
        return buyWithCard(player, commercialBlockId, offerSlot, cardStack, idempotencyKey, businessDate, false);
    }

    public ShopTransactionResult buyWithCredit(ServerPlayer player, UUID commercialBlockId, int offerSlot, ItemStack cardStack, String idempotencyKey, LocalDate businessDate) throws SQLException {
        return buyWithCard(player, commercialBlockId, offerSlot, cardStack, idempotencyKey, businessDate, true);
    }

    private ShopTransactionResult buyWithCard(ServerPlayer player, UUID commercialBlockId, int offerSlot, ItemStack cardStack, String idempotencyKey, LocalDate businessDate, boolean credit) throws SQLException {
        CommercialAccountLinks links = linkRepository.find(commercialBlockId).orElse(null);
        if (links == null || links.linkedAccountId() == null) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.LINKED_ACCOUNT_REQUIRED);
        }

        BankOfferSnapshot offer;
        long amount;
        try (Connection connection = EconomyDatabase.getConnection()) {
            offer = offerRepository.lockByBlockSlot(connection, commercialBlockId, offerSlot).orElse(null);
            ShopTransactionResult validation = validate(player, connection, offer);
            if (validation.type() != ShopTransactionResultType.COMPLETED) {
                return validation;
            }
            amount = validation.amount();
        }

        ShopTransactionResult payment = credit
                ? creditPayment(player, cardStack, links.linkedAccountId(), amount, idempotencyKey)
                : debitPayment(player, cardStack, links.linkedAccountId(), amount, idempotencyKey);
        if (payment.type() != ShopTransactionResultType.COMPLETED) {
            return payment;
        }

        return deliverProduct(player, commercialBlockId, offer, amount, businessDate);
    }

    private ShopTransactionResult validate(ServerPlayer player, Connection connection, BankOfferSnapshot offer) throws SQLException {
        if (offer == null) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.OFFER_NOT_FOUND);
        }
        BankOfferPrice price = priceService.currentPrice(offer);
        if (!price.sellEnabled() || price.bankSellPrice() == null || price.bankSellPrice() <= 0L) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.OFFER_DISABLED);
        }
        if (!stockService.hasStock(connection, offer)) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_STOCK);
        }
        return playerItems.canInsert(player, stockService.item(offer), offer.quantityPerOperation())
                ? ShopTransactionResult.completed(price.bankSellPrice())
                : ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_SPACE);
    }

    private ShopTransactionResult debitPayment(ServerPlayer player, ItemStack cardStack, UUID linkedAccountId, long amount, String idempotencyKey) throws SQLException {
        DebitPurchaseResult result = cardPaymentService.debitPurchase(cardStack, linkedAccountId, amount, player.getUUID(), idempotencyKey);
        if (result.type() == DebitPurchaseResultType.COMPLETED || result.type() == DebitPurchaseResultType.DUPLICATE_COMPLETED) {
            return ShopTransactionResult.completed(amount);
        }
        if (result.type() == DebitPurchaseResultType.INSUFFICIENT_BALANCE) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_BALANCE);
        }
        if (result.type() == DebitPurchaseResultType.DEBIT_NOT_ALLOWED) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.CARD_FUNCTION_NOT_ALLOWED);
        }
        return ShopTransactionResult.invalid(result.type() == DebitPurchaseResultType.INVALID_CARD
                ? ShopTransactionResultType.INVALID_CARD
                : ShopTransactionResultType.INACTIVE_ACCOUNT);
    }

    private ShopTransactionResult creditPayment(ServerPlayer player, ItemStack cardStack, UUID linkedAccountId, long amount, String idempotencyKey) throws SQLException {
        CreditPurchaseResult result = cardPaymentService.creditPurchase(cardStack, linkedAccountId, amount, player.getUUID(), "Loja de Venda", idempotencyKey);
        if (result.type() == CreditPurchaseResultType.COMPLETED || result.type() == CreditPurchaseResultType.DUPLICATE_COMPLETED) {
            return ShopTransactionResult.completed(amount);
        }
        if (result.type() == CreditPurchaseResultType.INSUFFICIENT_CREDIT) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_CREDIT);
        }
        if (result.type() == CreditPurchaseResultType.CREDIT_NOT_ALLOWED) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.CARD_FUNCTION_NOT_ALLOWED);
        }
        return ShopTransactionResult.invalid(result.type() == CreditPurchaseResultType.INVALID_CARD
                ? ShopTransactionResultType.INVALID_CARD
                : ShopTransactionResultType.INACTIVE_ACCOUNT);
    }

    private ShopTransactionResult deliverProduct(ServerPlayer player, UUID commercialBlockId, BankOfferSnapshot offer, long amount, LocalDate businessDate) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (!stockService.removeStock(connection, offer)) {
                    connection.rollback();
                    return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_STOCK);
                }
                statsRepository.recordPlayerPurchase(connection, offer.id(), offer.quantityPerOperation(), amount, offer.demandLevel(), businessDate);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
        playerItems.insert(player, stockService.item(offer), offer.quantityPerOperation());
        return ShopTransactionResult.completed(amount);
    }
}
