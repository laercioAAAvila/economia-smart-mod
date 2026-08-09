package br.com.economiamod.server.claim;

import java.util.UUID;

public record ClaimInvoiceResult(boolean success, String code, UUID invoiceId, long amount) {
    public static ClaimInvoiceResult success(UUID id, long amount) {
        return new ClaimInvoiceResult(true, "success", id, amount);
    }

    public static ClaimInvoiceResult denied(String code) {
        return new ClaimInvoiceResult(false, code, null, 0L);
    }
}
