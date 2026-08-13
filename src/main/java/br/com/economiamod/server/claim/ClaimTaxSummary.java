package br.com.economiamod.server.claim;

public record ClaimTaxSummary(long currentAmount, long totalAmount, int invoiceCount) {
    public static ClaimTaxSummary empty() {
        return new ClaimTaxSummary(0L, 0L, 0);
    }
}
