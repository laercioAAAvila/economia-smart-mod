package br.com.economiamod.client.screen;

import br.com.economiamod.common.network.AtmOperationHistoryPayload;

public interface AtmOperationHistoryAwareScreen {
    void applyOperationHistory(AtmOperationHistoryPayload payload);
}
