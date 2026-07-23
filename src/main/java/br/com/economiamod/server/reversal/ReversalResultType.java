package br.com.economiamod.server.reversal;

public enum ReversalResultType {
    COMPLETED,
    DUPLICATE_COMPLETED,
    ORIGINAL_NOT_FOUND,
    ORIGINAL_NOT_COMPLETED,
    UNSUPPORTED_TRANSACTION,
    INSUFFICIENT_BALANCE
}
