package br.com.economiamod.server.offer;

import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;

public final class OfferItemSnapshotMapper {
    public CommercialItemSnapshot item(BankOfferSnapshot offer) {
        return new CommercialItemSnapshot(
                offer.itemId(),
                offer.quantityPerOperation(),
                offer.itemComponents(),
                offer.itemDataVersion()
        );
    }
}
