package br.com.economiamod.server.operation;

public enum EconomyOperationState {
    CREATED,
    ITEMS_RESERVED,
    SQL_COMMITTED,
    ITEMS_DELIVERED,
    COMPLETED,
    ROLLBACK_REQUIRED,
    RECONCILIATION_REQUIRED,
    ROLLED_BACK
}
