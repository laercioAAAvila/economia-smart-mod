package br.com.economiamod.server.offer;

import br.com.economiamod.common.pricing.DynamicPriceCalculator;
import br.com.economiamod.common.pricing.PricingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class BankOfferDemandService {
    private final BankOfferReadRepository readRepository = new BankOfferReadRepository();
    private final BankOfferStatsRepository statsRepository = new BankOfferStatsRepository();
    private final BankOfferPriceService priceService = new BankOfferPriceService();

    public Optional<BankOfferPrice> recordPlayerPurchase(UUID offerId, long quantity, long moneyReceived, LocalDate businessDate) throws SQLException {
        try (Connection connection = readRepository.openConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Optional<BankOfferSnapshot> offer = readRepository.lockById(connection, offerId);
                if (offer.isEmpty()) {
                    connection.rollback();
                    return Optional.empty();
                }
                int demandLevel = nextDemandLevel(offer.get(), quantity);
                statsRepository.recordPlayerPurchase(connection, offerId, quantity, moneyReceived, demandLevel, businessDate);
                connection.commit();
                return Optional.of(priceService.currentPrice(offer.get().withDemandLevel(demandLevel)));
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public Optional<BankOfferPrice> recordPlayerSale(UUID offerId, long quantity, long moneyPaid, LocalDate businessDate) throws SQLException {
        try (Connection connection = readRepository.openConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Optional<BankOfferSnapshot> offer = readRepository.lockById(connection, offerId);
                if (offer.isEmpty()) {
                    connection.rollback();
                    return Optional.empty();
                }
                int supplyLevel = nextSupplyLevel(offer.get(), quantity);
                statsRepository.recordPlayerSale(connection, offerId, quantity, moneyPaid, supplyLevel, businessDate);
                connection.commit();
                return Optional.of(priceService.currentPrice(offer.get().withSupplyLevel(supplyLevel)));
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private int nextDemandLevel(BankOfferSnapshot offer, long quantity) {
        if (offer.pricingMode() != PricingMode.DYNAMIC) {
            return offer.demandLevel();
        }
        int levels = DynamicPriceCalculator.levelForQuantity(quantity, requiredQuantityPerLevel(offer), offer.maximumDemandLevel());
        return Math.min(offer.maximumDemandLevel(), offer.demandLevel() + levels);
    }

    private int nextSupplyLevel(BankOfferSnapshot offer, long quantity) {
        if (offer.pricingMode() != PricingMode.DYNAMIC) {
            return offer.supplyLevel();
        }
        int levels = DynamicPriceCalculator.levelForQuantity(quantity, requiredQuantityPerLevel(offer), offer.maximumSupplyLevel());
        return Math.min(offer.maximumSupplyLevel(), offer.supplyLevel() + levels);
    }

    private long requiredQuantityPerLevel(BankOfferSnapshot offer) {
        if (offer.quantityPerPriceLevel() == null) {
            throw new IllegalStateException("quantityPerPriceLevel is required for dynamic pricing");
        }
        return offer.quantityPerPriceLevel();
    }
}
