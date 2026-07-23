package br.com.economiamod.server.invoice;

public enum InvoicePaymentResultType {
    COMPLETED,
    DUPLICATE_COMPLETED,
    NO_DEBT,
    INSUFFICIENT_BALANCE,
    INACTIVE_ACCOUNT
}

