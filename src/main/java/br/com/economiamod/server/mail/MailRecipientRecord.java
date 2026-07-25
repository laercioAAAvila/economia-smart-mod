package br.com.economiamod.server.mail;

import java.util.UUID;

public record MailRecipientRecord(
        UUID destinationBlockId,
        String ownerName,
        String mailName,
        String dimension,
        int x,
        int y,
        int z
) {
}
