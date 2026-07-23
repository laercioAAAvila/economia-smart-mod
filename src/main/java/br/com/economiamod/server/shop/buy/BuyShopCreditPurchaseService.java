package br.com.economiamod.server.shop.buy;

import br.com.economiamod.common.money.BanknoteStackPlan;
import br.com.economiamod.server.cash.CashInventoryService;
import br.com.economiamod.server.commercial.CommercialAccountLinkRepository;
import br.com.economiamod.server.commercial.CommercialAccountLinks;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryMutationRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryType;
import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;
import br.com.economiamod.server.offer.BankOfferPrice;
import br.com.economiamod.server.offer.BankOfferPriceService;
import br.com.economiamod.server.offer.BankOfferReadRepository;
import br.com.economiamod.server.offer.BankOfferSnapshot;
import br.com.economiamod.server.offer.BankOfferStatsRepository;
import br.com.economiamod.server.offer.OfferItemSnapshotMapper;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.player.PlayerItemInventoryService;
import br.com.economiamod.server.shop.ShopTransactionResult;
import br.com.economiamod.server.shop.ShopTransactionResultType;
import br.com.economiamod.server.transaction.CreditPurchaseResult;
import br.com.economiamod.server.transaction.CreditPurchaseResultType;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class BuyShopCreditPurchaseService {
    private final CommercialAccountLinkRepository linkRepository = new CommercialAccountLinkRepository();
    private final BankOfferReadRepository offerRepository = new BankOfferReadRepository();
    private final BankOfferPriceService priceService = new BankOfferPriceService();
    private final BankOfferStatsRepository statsRepository = new BankOfferStatsRepository();
    private final CommercialInventoryMutationRepository inventoryRepository = new CommercialInventoryMutationRepository();
    private final OfferItemSnapshotMapper offerItemMapper = new OfferItemSnapshotMapper();
    private final PlayerItemInventoryService playerItems = new PlayerItemInventoryService();
    private final CashInventoryService cashInventory = new CashInventoryService();
    private final BuyShopFundingCreditService fundingCreditService = new BuyShopFundingCreditService();

    public ShopTransactionResult sellToShop(ServerPlayer player, UUID commercialBlockId, int offerSlot, String idempotencyKey, LocalDate businessDate) throws SQLException {
        CommercialAccountLinks links = linkRepository.find(commercialBlockId).orElse(null);
        if (links == null || links.fundingCardId() == null) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INVALID_CARD);
        }

        try (Connection connection = EconomyDatabase.getConnection()) {
            BankOfferSnapshot offer = offerRepository.lockByBlockSlot(connection, commercialBlockId, offerSlot).orElse(null);
            ShopTransactionResult validation = validate(player, connection, offer);
            if (validation.type() != ShopTransactionResultType.COMPLETED) {
                return validation;
            }

            long amount = validation.amount();
            CommercialItemSnapshot item = offerItemMapper.item(offer);
            if (!playerItems.removeMatching(player, item, offer.comparisonMode(), offer.quantityPerOperation())) {
                return ShopTransactionResult.invalid(ShopTransactionResultType.ITEM_NOT_ACCEPTED);
            }

            CreditPurchaseResult credit = fundingCreditService.charge(links.fundingCardId(), amount, player.getUUID(), "Loja de Compra", idempotencyKey);
            if (credit.type() != CreditPurchaseResultType.COMPLETED && credit.type() != CreditPurchaseResultType.DUPLICATE_COMPLETED) {
                playerItems.insert(player, item, offer.quantityPerOperation());
                return creditFailure(credit);
            }

            return storeItemAndPay(player, commercialBlockId, offer, item, amount, businessDate);
        }
    }

    private ShopTransactionResult validate(ServerPlayer player, Connection connection, BankOfferSnapshot offer) throws SQLException {
        if (offer == null) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.OFFER_NOT_FOUND);
        }
        BankOfferPrice price = priceService.currentPrice(offer);
        if (!price.buyEnabled() || price.bankBuyPrice() == null || price.bankBuyPrice() <= 0L) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.OFFER_DISABLED);
        }
        if (!buyLimitAllows(offer)) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.OFFER_DISABLED);
        }
        CommercialItemSnapshot item = offerItemMapper.item(offer);
        if (!playerItems.hasMatching(player, item, offer.comparisonMode(), offer.quantityPerOperation())) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.ITEM_NOT_ACCEPTED);
        }
        if (!inventoryRepository.canAddToSlots(connection, offer.commercialBlockId(), CommercialInventoryType.PURCHASED_ITEMS, item, offer.quantityPerOperation())) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_SPACE);
        }
        BanknoteStackPlan plan = cashInventory.buildWithdrawalPlan(price.bankBuyPrice());
        return cashInventory.canInsert(player, plan)
                ? ShopTransactionResult.completed(price.bankBuyPrice())
                : ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_SPACE);
    }

    private boolean buyLimitAllows(BankOfferSnapshot offer) {
        return offer.targetQuantity() == null
                || offer.purchasedQuantity() + offer.quantityPerOperation() <= offer.targetQuantity();
    }

    private ShopTransactionResult storeItemAndPay(ServerPlayer player, UUID commercialBlockId, BankOfferSnapshot offer, CommercialItemSnapshot item, long amount, LocalDate businessDate) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                boolean stored = inventoryRepository.addToSlots(connection, commercialBlockId, CommercialInventoryType.PURCHASED_ITEMS, item, offer.quantityPerOperation());
                if (!stored) {
                    connection.rollback();
                    playerItems.insert(player, item, offer.quantityPerOperation());
                    return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_SPACE);
                }
                statsRepository.recordPlayerSale(connection, offer.id(), offer.quantityPerOperation(), amount, offer.supplyLevel(), businessDate);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
        cashInventory.insert(player, cashInventory.buildWithdrawalPlan(amount));
        return ShopTransactionResult.completed(amount);
    }

    private ShopTransactionResult creditFailure(CreditPurchaseResult credit) {
        if (credit.type() == CreditPurchaseResultType.INSUFFICIENT_CREDIT) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_CREDIT);
        }
        if (credit.type() == CreditPurchaseResultType.CREDIT_NOT_ALLOWED) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.CARD_FUNCTION_NOT_ALLOWED);
        }
        return ShopTransactionResult.invalid(ShopTransactionResultType.INACTIVE_ACCOUNT);
    }
}
