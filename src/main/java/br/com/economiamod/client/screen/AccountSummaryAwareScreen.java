package br.com.economiamod.client.screen;

import br.com.economiamod.common.network.AtmAccountSummaryPayload;

public interface AccountSummaryAwareScreen {
    void applyAccountSummary(AtmAccountSummaryPayload payload);
}
