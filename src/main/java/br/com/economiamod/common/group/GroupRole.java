package br.com.economiamod.common.group;

public enum GroupRole {
    OWNER,
    LEADER,
    VICE_LEADER,
    MEMBER;

    public boolean leadsClan() {
        return this == LEADER || this == VICE_LEADER;
    }

    public boolean ownsPrivateProperty() {
        return this == OWNER;
    }
}
