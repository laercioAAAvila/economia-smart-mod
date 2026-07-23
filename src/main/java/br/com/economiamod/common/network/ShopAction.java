package br.com.economiamod.common.network;

public enum ShopAction {
    SAVE_CONFIG(0),
    TRADE(1);

    private final int id;

    ShopAction(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static ShopAction byId(int id) {
        for (ShopAction action : values()) {
            if (action.id == id) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown shop action: " + id);
    }
}
