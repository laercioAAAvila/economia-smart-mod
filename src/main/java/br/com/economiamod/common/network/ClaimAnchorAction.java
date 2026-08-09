package br.com.economiamod.common.network;

public enum ClaimAnchorAction {
    CLAIM,
    ANCHOR_INVOICE,
    SALE_INVOICE,
    INVITE_MEMBER,
    REISSUE_INVOICES;

    public static ClaimAnchorAction byId(int id) {
        return values()[Math.max(0, Math.min(values().length - 1, id))];
    }
}
