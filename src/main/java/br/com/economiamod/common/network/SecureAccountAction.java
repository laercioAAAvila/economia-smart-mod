package br.com.economiamod.common.network;

public enum SecureAccountAction {
    LOGIN(0),
    CREATE_ACCOUNT(1),
    CHANGE_PASSWORD(2),
    CARD_LOGIN(3),
    RECOVER_PASSWORD(4),
    SESSION_STATE(5),
    ACCOUNT_SUMMARY(6),
    UPDATE_CARD_CREDIT(7),
    TRANSFER(8),
    GOLD_PRICE_REFRESH(9),
    WITHDRAW(10),
    ISSUE_CARD(11),
    UPDATE_ACCOUNT_CREDIT(12),
    UNBLOCK_CARD(13),
    REQUEST_ACCOUNT_CREDIT(14),
    LOGOUT(15),
    SET_CARD_SLOT_MODE(16),
    UPDATE_DEBIT_DAILY_LIMIT(17),
    PAY_INVOICE(18),
    ISSUE_INVOICE(19),
    PAY_ALL_INVOICES(20),
    REFRESH_CARDS(21),
    BLOCK_CARD_BY_ID(22),
    DISABLE_CARD_BY_ID(23),
    DEPOSIT(24);

    private final int id;

    SecureAccountAction(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static SecureAccountAction byId(int id) {
        for (SecureAccountAction action : values()) {
            if (action.id == id) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown secure account action: " + id);
    }
}
