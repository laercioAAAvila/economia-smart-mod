package br.com.economiamod.server.invoice;

public record InvoicePaymentResult(InvoicePaymentResultType type, long paidAmount, long balanceAfter) {
    public static InvoicePaymentResult completed(long paidAmount, long balanceAfter) {
        return new InvoicePaymentResult(InvoicePaymentResultType.COMPLETED, paidAmount, balanceAfter);
    }

    public static InvoicePaymentResult duplicateCompleted(long paidAmount, long balanceAfter) {
        return new InvoicePaymentResult(InvoicePaymentResultType.DUPLICATE_COMPLETED, paidAmount, balanceAfter);
    }

    public static InvoicePaymentResult noDebt() {
        return new InvoicePaymentResult(InvoicePaymentResultType.NO_DEBT, 0L, 0L);
    }

    public static InvoicePaymentResult insufficientBalance() {
        return new InvoicePaymentResult(InvoicePaymentResultType.INSUFFICIENT_BALANCE, 0L, 0L);
    }

    public static InvoicePaymentResult inactiveAccount() {
        return new InvoicePaymentResult(InvoicePaymentResultType.INACTIVE_ACCOUNT, 0L, 0L);
    }
}

