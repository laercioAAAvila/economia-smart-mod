package br.com.economiamod.server.transaction;

public enum LedgerEntryType {
    DEBIT,
    CREDIT,
    CREDIT_PRINCIPAL_INCREASE,
    CREDIT_INTEREST_INCREASE,
    CREDIT_DEBT_PAYMENT,
    CURRENCY_ISSUANCE,
    CURRENCY_REDEMPTION,
    ADJUSTMENT
}

