package br.com.economiamod.server.account;

public enum AccountCreditLimitResultType {
    UPDATED,
    INVALID_LIMIT,
    INACTIVE_ACCOUNT,
    LIMIT_BELOW_DEBT,
    LIMIT_BELOW_RESERVED,
    LIMIT_ABOVE_ALLOWED,
    DEBT_PRESENT,
    NO_CREDIT_AVAILABLE
}
