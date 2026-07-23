package br.com.economiamod.server.shop.buy;

import br.com.economiamod.common.money.BanknoteStackPlan;
import br.com.economiamod.server.cash.CashInventoryService;
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
import br.com.economiamod.server.shop.CommercialBanknoteReserveService;
import br.com.economiamod.server.shop.ShopTransactionResult;
import br.com.economiamod.server.shop.ShopTransactionResultType;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class BuyShopCashReservePurchaseService {
    private final BankOfferReadRepository offerRepository = new BankOfferReadRepository();
    private final BankOfferPriceService priceService = new BankOfferPriceService();
    private final BankOfferStatsRepository statsRepository = new BankOfferStatsRepository();
    private final CommercialInventoryMutationRepository inventoryRepository = new CommercialInventoryMutationRepository();
    private final CommercialBanknoteReserveService reserveService = new CommercialBanknoteReserveService();
    private final OfferItemSnapshotMapper offerItemMapper = new OfferItemSnapshotMapper();
    private final PlayerItemInventoryService playerItems = new PlayerItemInventoryService();
    private final CashInventoryService cashInventory = new CashInventoryService();

    public ShopTransactionResult sellToShop(ServerPlayer player, UUID commercialBlockId, int offerSlot, LocalDate businessDate) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                BankOfferSnapshot offer = offerRepository.lockByBlockSlot(connection, commercialBlockId, offerSlot).orElse(null);
                ShopTransactionResult validation = validate(player, connection, offer);
                if (validation.type() != ShopTransactionResultType.COMPLETED) {
                    connection.rollback();
                    return validation;
                }
                long amount = validation.amount();
                CommercialItemSnapshot item = offerItemMapper.item(offer);
                if (!playerItems.removeMatching(player, item, offer.comparisonMode(), offer.quantityPerOperation())) {
                    connection.rollback();
                    return ShopTransactionResult.invalid(ShopTransactionResultType.ITEM_NOT_ACCEPTED);
                }
                reserveService.removeExact(connection, commercialBlockId, amount);
                boolean stored = inventoryRepository.addToSlots(connection, commercialBlockId, CommercialInventoryType.PURCHASED_ITEMS, item, offer.quantityPerOperation());
                if (!stored) {
                    connection.rollback();
                    playerItems.insert(player, item, offer.quantityPerOperation());
                    return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_SPACE);
                }
                statsRepository.recordPlayerSale(connection, offer.id(), offer.quantityPerOperation(), amount, offer.supplyLevel(), businessDate);
                connection.commit();

                cashInventory.insert(player, cashInventory.buildWithdrawalPlan(amount));
                return ShopTransactionResult.completed(amount);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
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
        if (!cashInventory.canInsert(player, plan)) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_SPACE);
        }
        if (!reserveService.canRemoveExact(connection, offer.commercialBlockId(), price.bankBuyPrice())) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_PAYMENT);
        }
        return ShopTransactionResult.completed(price.bankBuyPrice());
    }

    private boolean buyLimitAllows(BankOfferSnapshot offer) {
        return offer.targetQuantity() == null
                || offer.purchasedQuantity() + offer.quantityPerOperation() <= offer.targetQuantity();
    }
}
