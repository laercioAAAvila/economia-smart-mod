package br.com.economiamod.common.group;

public enum TerritoryPermission {
    USE(1),
    DESTROY(1 << 1),
    PLACE(1 << 2);

    private final int bit;

    TerritoryPermission(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }

    public boolean presentIn(int mask) {
        return (mask & bit) != 0;
    }

    public static int clanDefaultMask() {
        return USE.bit;
    }

    public static int privatePropertyDefaultMask() {
        return USE.bit | DESTROY.bit | PLACE.bit;
    }
}
