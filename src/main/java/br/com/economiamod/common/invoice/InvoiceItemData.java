package br.com.economiamod.common.invoice;

import java.util.UUID;

public record InvoiceItemData(UUID accountId, UUID entryId, long amount) {
}
