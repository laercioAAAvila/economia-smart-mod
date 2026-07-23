package br.com.economiamod.server.gold;

public record GoldPriceSnapshot(
        long baseNuggetValue,
        long buyBps,
        long nuggetBuyValue,
        long ingotBuyValue,
        long blockBuyValue,
        int demandLevel,
        int idleLevel,
        long recentMintedNuggetUnits
) {
}
