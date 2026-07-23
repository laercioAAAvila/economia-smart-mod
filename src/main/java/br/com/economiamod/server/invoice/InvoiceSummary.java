package br.com.economiamod.server.invoice;

import java.util.List;

public record InvoiceSummary(
        long principalOutstanding,
        long interestOutstanding,
        long totalDebt,
        List<InvoiceOpenEntry> openEntries
) {
}

