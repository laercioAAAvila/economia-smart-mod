package br.com.economiamod.server.invoice;

import java.util.UUID;

record InvoiceEntryPayment(UUID entryId, UUID cardId, boolean isInterest, long payAmount) {
}

