package br.com.economiamod.server.mail;

import java.util.UUID;

public record MailBlockRecord(
        UUID id,
        UUID ownerPlayerUuid,
        UUID ownerAccountId,
        String ownerName,
        String ownerAccountNumber,
        String name,
        String dimension,
        int x,
        int y,
        int z
) {
    public boolean named() {
        return name != null && !name.isBlank();
    }
}
