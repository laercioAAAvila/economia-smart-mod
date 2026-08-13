package br.com.economiamod.server.claim;

import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.config.EconomyServerConfig;

public final class ClaimPriceService {
    public long landPrice(String dimension, int blockX, int blockZ) {
        PriceBand band = band(dimension);
        long distance = (long) Math.floor(Math.hypot((double) blockX, (double) blockZ));
        long bands = distance / band.interval();
        return saturatingAdd(band.base(), saturatingAdd(
                saturatingMultiply(bands, band.linear()),
                saturatingMultiply(saturatingMultiply(bands, bands), band.progressive())));
    }

    public long anchorPrice(long landPrice) {
        long percentage = EconomyServerConfig.ANCHOR_LAND_PERCENTAGE.get();
        long normalizedLandPrice = Math.max(0L, landPrice);
        long proportional = saturatingAdd(
                saturatingMultiply(normalizedLandPrice / 100L, percentage),
                saturatingMultiply(normalizedLandPrice % 100L, percentage) / 100L);
        return saturatingAdd(EconomyServerConfig.ANCHOR_BASE_PRICE.get(), proportional);
    }

    public long anchorPrice(long landPrice, GroupType groupType) {
        long price = anchorPrice(landPrice);
        if (groupType != GroupType.CLAN) {
            return price;
        }
        int percentage = EconomyServerConfig.ANCHOR_CLAN_TAX_MULTIPLIER_PERCENTAGE.get();
        return saturatingAdd(saturatingMultiply(price / 100L, percentage),
                saturatingMultiply(price % 100L, percentage) / 100L);
    }

    private PriceBand band(String dimension) {
        if ("minecraft:overworld".equals(dimension)) {
            return new PriceBand(EconomyServerConfig.CLAIM_PRICE_OVERWORLD_BASE.get(),
                    EconomyServerConfig.CLAIM_PRICE_OVERWORLD_INTERVAL.get(),
                    EconomyServerConfig.CLAIM_PRICE_OVERWORLD_LINEAR.get(),
                    EconomyServerConfig.CLAIM_PRICE_OVERWORLD_PROGRESSIVE.get());
        }
        if ("minecraft:the_nether".equals(dimension)) {
            return new PriceBand(EconomyServerConfig.CLAIM_PRICE_NETHER_BASE.get(),
                    EconomyServerConfig.CLAIM_PRICE_NETHER_INTERVAL.get(),
                    EconomyServerConfig.CLAIM_PRICE_NETHER_LINEAR.get(),
                    EconomyServerConfig.CLAIM_PRICE_NETHER_PROGRESSIVE.get());
        }
        if ("minecraft:the_end".equals(dimension)) {
            return new PriceBand(EconomyServerConfig.CLAIM_PRICE_END_BASE.get(),
                    EconomyServerConfig.CLAIM_PRICE_END_INTERVAL.get(),
                    EconomyServerConfig.CLAIM_PRICE_END_LINEAR.get(),
                    EconomyServerConfig.CLAIM_PRICE_END_PROGRESSIVE.get());
        }
        return new PriceBand(EconomyServerConfig.CLAIM_PRICE_OTHER_BASE.get(),
                EconomyServerConfig.CLAIM_PRICE_OTHER_INTERVAL.get(),
                EconomyServerConfig.CLAIM_PRICE_OTHER_LINEAR.get(),
                EconomyServerConfig.CLAIM_PRICE_OTHER_PROGRESSIVE.get());
    }

    private long saturatingMultiply(long left, long right) {
        if (left == 0L || right == 0L) {
            return 0L;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private long saturatingAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private record PriceBand(long base, int interval, long linear, long progressive) {
    }
}
