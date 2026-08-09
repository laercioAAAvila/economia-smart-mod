package br.com.economiamod.common.network;

public enum GroupAction {
    AUTHENTICATE,
    REFRESH,
    CREATE,
    INVITE,
    ACCEPT_INVITE,
    DECLINE_INVITE,
    REMOVE_MEMBER,
    SET_PERMISSIONS,
    SET_VICE_LEADER,
    LEAVE,
    RENAME,
    CLOSE,
    SET_VISITOR_SHOPS,
    DEPOSIT,
    WITHDRAW;

    public static GroupAction byId(int id) {
        GroupAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : REFRESH;
    }
}
