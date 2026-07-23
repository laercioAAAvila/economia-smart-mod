package br.com.economiamod.server.gold;

import br.com.economiamod.common.gold.GoldUnitConverter;
import br.com.economiamod.server.config.EconomyServerConfig;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

public final class GoldDynamicPricingService {
    private static final long BPS_BASE = 10_000L;
    private static final long SNAPSHOT_CACHE_TTL_MILLIS = 10_000L;
    private static final int MIN_BUY_BPS = 5_000;
    private static final int MAX_BUY_BPS = 15_000;
    private static final int MAX_LEVELS = 10;
    private static final long MAX_COUNTER_NUGGET_UNITS_PER_OPERATION = GoldUnitConverter.NUGGET_UNITS_PER_BLOCK * 64L * 9L;
    private static final long MAX_SAFE_NUGGET_VALUE = Math.min(Integer.MAX_VALUE, Long.MAX_VALUE / MAX_COUNTER_NUGGET_UNITS_PER_OPERATION);

    private static volatile CachedSnapshot cachedSnapshot;

    private final GoldExchangeRepository repository = new GoldExchangeRepository();

    public GoldPriceSnapshot currentSnapshot() throws SQLException {
        long now = System.currentTimeMillis();
        CachedSnapshot cached = cachedSnapshot;
        if (cached != null && cached.expiresAtMillis > now) {
            return cached.snapshot;
        }
        try (Connection connection = repository.openConnection()) {
            GoldPriceSnapshot snapshot = currentSnapshot(connection);
            cachedSnapshot = new CachedSnapshot(snapshot, now + SNAPSHOT_CACHE_TTL_MILLIS);
            return snapshot;
        }
    }

    public GoldPriceSnapshot currentSnapshot(Connection connection) throws SQLException {
        long baseValue = EconomyServerConfig.BANK_GOLD_NUGGET_VALUE.get();
        if (!EconomyServerConfig.DYNAMIC_PRICING_ENABLED.get()) {
            return snapshot(baseValue, BPS_BASE, 0, 0, 0L);
        }

        Instant now = Instant.now();
        long recentUnits = recentMintedNuggetUnits(connection, now.minus(Duration.ofHours(24)));
        long goldUnitsPerLevel = goldUnitsPerLevel(EconomyServerConfig.DYNAMIC_PRICING_DEFAULT_QUANTITY_PER_LEVEL.get());
        int demandLevel = cappedLevel(recentUnits, goldUnitsPerLevel);
        int idleLevel = idleLevel(lastMintAt(connection), now);
        long bps = BPS_BASE
                - Math.multiplyExact((long) demandLevel, EconomyServerConfig.DYNAMIC_PRICING_DEFAULT_SUPPLY_DECREASE_BPS.get())
                + Math.multiplyExact((long) idleLevel, EconomyServerConfig.DYNAMIC_PRICING_DEFAULT_DEMAND_INCREASE_BPS.get());
        bps = Math.max(MIN_BUY_BPS, Math.min(MAX_BUY_BPS, bps));
        long safeBaseValue = Math.min(MAX_SAFE_NUGGET_VALUE, baseValue);
        return snapshot(safeBaseValue, bps, demandLevel, idleLevel, recentUnits);
    }

    public GoldPriceSnapshot currentBuySnapshot(Connection connection) throws SQLException {
        return currentSnapshot(connection);
    }

    public long moneyAmount(long nuggetUnits, long baseNuggetValue, long buyBps) {
        if (nuggetUnits <= 0L) {
            return 0L;
        }
        long nuggetValue = roundedMoneyAmount(GoldUnitConverter.NUGGET_UNITS_PER_NUGGET, baseNuggetValue, buyBps);
        BigInteger result = BigInteger.valueOf(nuggetUnits)
                .multiply(BigInteger.valueOf(nuggetValue));
        return result.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    private long roundedMoneyAmount(long nuggetUnits, long baseNuggetValue, long buyBps) {
        BigInteger result = BigInteger.valueOf(nuggetUnits)
                .multiply(BigInteger.valueOf(baseNuggetValue))
                .multiply(BigInteger.valueOf(buyBps))
                .divide(BigInteger.valueOf(BPS_BASE));
        return Math.max(1L, result.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
    }

    private GoldPriceSnapshot snapshot(long baseNuggetValue, long buyBps, int demandLevel, int idleLevel, long recentUnits) {
        long nuggetBuyValue = moneyAmount(GoldUnitConverter.NUGGET_UNITS_PER_NUGGET, baseNuggetValue, buyBps);
        return new GoldPriceSnapshot(
                baseNuggetValue,
                buyBps,
                nuggetBuyValue,
                moneyAmount(GoldUnitConverter.NUGGET_UNITS_PER_INGOT, baseNuggetValue, buyBps),
                moneyAmount(GoldUnitConverter.NUGGET_UNITS_PER_BLOCK, baseNuggetValue, buyBps),
                demandLevel,
                idleLevel,
                recentUnits
        );
    }

    private long recentMintedNuggetUnits(Connection connection, Instant since) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(gold_nugget_units), 0) AS total_units
                  FROM economy_gold_exchange_entries
                 WHERE operation_type = 'MINT'
                   AND created_at >= ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(since));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("total_units") : 0L;
            }
        }
    }

    private Timestamp lastMintAt(Connection connection) throws SQLException {
        String sql = "SELECT MAX(created_at) AS last_mint_at FROM economy_gold_exchange_entries WHERE operation_type = 'MINT'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getTimestamp("last_mint_at") : null;
        }
    }

    private int idleLevel(Timestamp lastMintAt, Instant now) {
        if (lastMintAt == null) {
            return 0;
        }
        long idleDays = Duration.between(lastMintAt.toInstant(), now).toDays();
        if (idleDays <= 0L) {
            return 0;
        }
        long recoveryPerDay = EconomyServerConfig.DYNAMIC_PRICING_DEFAULT_RECOVERY_LEVELS_PER_IDLE_DAY.get();
        if (recoveryPerDay <= 0L) {
            return 0;
        }
        if (idleDays >= MAX_LEVELS) {
            return MAX_LEVELS;
        }
        long levels = idleDays * recoveryPerDay;
        return Math.toIntExact(Math.min(MAX_LEVELS, levels));
    }

    private int cappedLevel(long quantity, long quantityPerLevel) {
        if (quantity <= 0L || quantityPerLevel <= 0L) {
            return 0;
        }
        long level = Math.floorDiv(quantity - 1L, quantityPerLevel) + 1L;
        return Math.toIntExact(Math.min(MAX_LEVELS, level));
    }

    private long goldUnitsPerLevel(long configuredQuantityPerLevel) {
        BigInteger result = BigInteger.valueOf(Math.max(1L, configuredQuantityPerLevel))
                .multiply(BigInteger.valueOf(GoldUnitConverter.NUGGET_UNITS_PER_BLOCK));
        return result.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    private record CachedSnapshot(GoldPriceSnapshot snapshot, long expiresAtMillis) {
    }
}
