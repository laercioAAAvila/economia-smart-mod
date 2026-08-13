package br.com.economiamod.common.network;

public enum ClaimAnchorAction {
    OPEN_PAYMENT,
    SET_PAYMENT_MODE,
    PAY_CLAIM,
    CLOSE_PAYMENT,
    SALE_INVOICE,
    INVITE_MEMBER,
    OPEN_CHUNK_MAP,
    CURRENT_TAX_INVOICE,
    ALL_TAXES_INVOICE,
    UPDATE_MEMBER_PERMISSIONS,
    REMOVE_MEMBER,
    AUTHENTICATE;

    public static ClaimAnchorAction byId(int id) {
        return values()[Math.max(0, Math.min(values().length - 1, id))];
    }
}
