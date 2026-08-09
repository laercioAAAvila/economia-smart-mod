package br.com.economiamod.common.network;

public enum MapAction {
    REFRESH,
    SAVE_LOCATION,
    UPDATE_LOCATION,
    DELETE_LOCATION,
    TOGGLE_CLAIM;

    public static MapAction byId(int id) {
        MapAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : REFRESH;
    }
}
