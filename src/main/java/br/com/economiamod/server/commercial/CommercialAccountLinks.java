package br.com.economiamod.server.commercial;

import java.util.UUID;

public record CommercialAccountLinks(
        UUID commercialBlockId,
        UUID linkedAccountId,
        UUID fundingCardId
) {
}
