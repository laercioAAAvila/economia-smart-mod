package br.com.economiamod.common.network;

public enum MailAction {
    SET_NAME(0),
    ADD_RECIPIENT(1),
    DELETE_RECIPIENT(2),
    SELECT_RECIPIENT(3),
    PAY_CASH(4),
    PAY_CARD(5),
    CONFIRM_CHANGE_TO_OWNER(6),
    SEND(7),
    REFRESH(8),
    OPEN_PAYMENT(9),
    CLOSE_PAYMENT(10),
    RETURN_TO_PAYMENT(11);

    private final int id;

    MailAction(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static MailAction byId(int id) {
        for (MailAction action : values()) {
            if (action.id == id) {
                return action;
            }
        }
        return REFRESH;
    }
}
