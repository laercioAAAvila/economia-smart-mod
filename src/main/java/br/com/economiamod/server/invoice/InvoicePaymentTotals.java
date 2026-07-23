package br.com.economiamod.server.invoice;

import java.util.List;

record InvoicePaymentTotals(long principalPaid, long interestPaid) {
    static InvoicePaymentTotals from(List<InvoiceEntryPayment> entries) {
        long principalPaid = 0L;
        long interestPaid = 0L;
        for (InvoiceEntryPayment entry : entries) {
            if (entry.isInterest()) {
                interestPaid += entry.payAmount();
            } else {
                principalPaid += entry.payAmount();
            }
        }
        return new InvoicePaymentTotals(principalPaid, interestPaid);
    }
}

