package br.com.economiamod.server.shop.sell;

import br.com.economiamod.server.cash.CashInventoryService;
import br.com.economiamod.server.offer.BankOfferPrice;
import br.com.economiamod.server.offer.BankOfferPriceService;
import br.com.economiamod.server.offer.BankOfferReadRepository;
import br.com.economiamod.server.offer.BankOfferSnapshot;
import br.com.economiamod.server.offer.BankOfferStatsRepository;
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

public final class SellShopCashSaleService {
    private final BankOfferReadRepository offerRepository = new BankOfferReadRepository();
    private final BankOfferPriceService priceService = new BankOfferPriceService();
    private final BankOfferStatsRepository statsRepository = new BankOfferStatsRepository();
    private final SellShopStockService stockService = new SellShopStockService();
    private final CommercialBanknoteReserveService reserveService = new CommercialBanknoteReserveService();
    private final PlayerItemInventoryService playerItems = new PlayerItemInventoryService();
    private final CashInventoryService cashInventory = new CashInventoryService();

    public ShopTransactionResult buyFromShop(ServerPlayer player, UUID commercialBlockId, int offerSlot, LocalDate businessDate) throws SQLException {
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
                if (!cashInventory.removeExactValue(player, amount)) {
                    connection.rollback();
                    return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_PAYMENT);
                }
                if (!stockService.removeStock(connection, offer) || !reserveService.addExact(connection, commercialBlockId, amount)) {
                    connection.rollback();
                    cashInventory.insert(player, cashInventory.buildWithdrawalPlan(amount));
                    return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_STOCK);
                }
                statsRepository.recordPlayerPurchase(connection, offer.id(), offer.quantityPerOperation(), amount, offer.demandLevel(), businessDate);
                connection.commit();

                playerItems.insert(player, stockService.item(offer), offer.quantityPerOperation());
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
        if (!price.sellEnabled() || price.bankSellPrice() == null || price.bankSellPrice() <= 0L) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.OFFER_DISABLED);
        }
        if (!stockService.hasStock(connection, offer)) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_STOCK);
        }
        if (!playerItems.canInsert(player, stockService.item(offer), offer.quantityPerOperation())) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_SPACE);
        }
        if (cashInventory.totalBanknotes(player) < price.bankSellPrice()) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_PAYMENT);
        }
        if (!cashInventory.canRemoveExactValue(player, price.bankSellPrice())) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_PAYMENT);
        }
        if (!reserveService.canAddExact(connection, offer.commercialBlockId(), price.bankSellPrice())) {
            return ShopTransactionResult.invalid(ShopTransactionResultType.INSUFFICIENT_SPACE);
        }
        return ShopTransactionResult.completed(price.bankSellPrice());
    }
}
