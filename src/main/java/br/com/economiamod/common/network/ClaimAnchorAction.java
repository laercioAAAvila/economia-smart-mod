package br.com.economiamod.common.network;

public enum ClaimAnchorAction {
    CLAIM,
    OPEN_PAYMENT,
    SET_PAYMENT_MODE,
    PAY_CLAIM,
    CLOSE_PAYMENT,
    ANCHOR_INVOICE,
    SALE_INVOICE,
    INVITE_MEMBER,
    REISSUE_INVOICES,
    OPEN_CHUNK_MAP;

    public static ClaimAnchorAction byId(int id) {
        return values()[Math.max(0, Math.min(values().length - 1, id))];
    }
}
