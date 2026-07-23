package br.com.economiamod.client.screen;

import br.com.economiamod.common.network.AtmCardsPayload;

public interface AtmCardsAwareScreen {
    void applyAtmCards(AtmCardsPayload payload);
}
