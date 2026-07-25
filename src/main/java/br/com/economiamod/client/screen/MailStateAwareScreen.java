package br.com.economiamod.client.screen;

import br.com.economiamod.common.network.MailStatePayload;

public interface MailStateAwareScreen {
    void applyMailState(MailStatePayload payload);
}
