package br.com.economiamod.server.offer;

import br.com.economiamod.common.pricing.DynamicPriceCalculator;
import br.com.economiamod.common.pricing.PricingMode;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class BankOfferPriceService {
    private final BankOfferReadRepository repository = new BankOfferReadRepository();

    public Optional<BankOfferPrice> currentPrice(UUID offerId) throws SQLException {
        return repository.findById(offerId).map(this::currentPrice);
    }

    public BankOfferPrice currentPrice(BankOfferSnapshot offer) {
        if (offer.pricingMode() == PricingMode.DYNAMIC) {
            return dynamicPrice(offer);
        }
        return new BankOfferPrice(
                offer.id(),
                offer.baseBuyPrice(),
                offer.baseSellPrice(),
                offer.buyEnabled(),
                offer.sellEnabled()
        );
    }

    private BankOfferPrice dynamicPrice(BankOfferSnapshot offer) {
        Long buyPrice = offer.baseBuyPrice() == null ? null : DynamicPriceCalculator.bankBuyPrice(
                offer.baseBuyPrice(),
                offer.supplyLevel(),
                required(offer.supplyDecreaseBps(), "supplyDecreaseBps"),
                defaultZero(offer.minimumBuyPrice())
        );
        Long sellPrice = offer.baseSellPrice() == null ? null : DynamicPriceCalculator.bankSellPrice(
                offer.baseSellPrice(),
                offer.demandLevel(),
                required(offer.demandIncreaseBps(), "demandIncreaseBps"),
                defaultZero(offer.maximumSellPrice())
        );
        return new BankOfferPrice(offer.id(), buyPrice, sellPrice, offer.buyEnabled(), offer.sellEnabled());
    }

    private int required(Integer value, String name) {
        if (value == null) {
            throw new IllegalStateException(name + " is required for dynamic pricing");
        }
        return value;
    }

    private long defaultZero(Long value) {
        return value == null ? 0L : value;
    }
}
