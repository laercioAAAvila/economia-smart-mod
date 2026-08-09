package br.com.economiamod.common.group;

public enum ChatChannel {
    GENERAL,
    CLAN,
    PRIVATE_PROPERTY;

    public static ChatChannel byId(int id) {
        ChatChannel[] values = values();
        return id >= 0 && id < values.length ? values[id] : GENERAL;
    }

    public String translationSuffix() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
