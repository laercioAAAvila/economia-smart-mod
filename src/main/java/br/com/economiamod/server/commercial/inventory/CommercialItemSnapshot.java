package br.com.economiamod.server.commercial.inventory;

public record CommercialItemSnapshot(String itemId, int count, String components, Integer dataVersion) {
    public static CommercialItemSnapshot empty() {
        return new CommercialItemSnapshot(null, 0, null, null);
    }

    public boolean isEmpty() {
        return itemId == null || count <= 0;
    }
}

