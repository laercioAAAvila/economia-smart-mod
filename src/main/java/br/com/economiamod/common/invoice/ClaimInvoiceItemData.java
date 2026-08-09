package br.com.economiamod.common.invoice;

import java.util.UUID;

public record ClaimInvoiceItemData(UUID invoiceId, long amount, String invoiceType) {
}
