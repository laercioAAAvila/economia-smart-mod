package br.com.economiamod.server.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class EconomyServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> DATABASE_TYPE;
    public static final ModConfigSpec.ConfigValue<String> DATABASE_HOST;
    public static final ModConfigSpec.IntValue DATABASE_PORT;
    public static final ModConfigSpec.ConfigValue<String> DATABASE_NAME;
    public static final ModConfigSpec.ConfigValue<String> DATABASE_USERNAME;
    public static final ModConfigSpec.ConfigValue<String> DATABASE_PASSWORD;
    public static final ModConfigSpec.BooleanValue DATABASE_SSL;
    public static final ModConfigSpec.ConfigValue<String> DATABASE_SQLITE_FILE;
    public static final ModConfigSpec.IntValue DATABASE_POOL_MINIMUM;
    public static final ModConfigSpec.IntValue DATABASE_POOL_MAXIMUM;
    public static final ModConfigSpec.IntValue DATABASE_CONNECTION_TIMEOUT_MS;
    public static final ModConfigSpec.IntValue DATABASE_QUERY_TIMEOUT_MS;

    public static final ModConfigSpec.ConfigValue<String> ECONOMY_TIME_ZONE;
    public static final ModConfigSpec.IntValue ECONOMY_SESSION_TIMEOUT_SECONDS;

    public static final ModConfigSpec.BooleanValue WEB_API_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> WEB_API_BIND;
    public static final ModConfigSpec.IntValue WEB_API_PORT;
    public static final ModConfigSpec.ConfigValue<String> WEB_API_ALLOWED_ORIGIN;
    public static final ModConfigSpec.IntValue WEB_API_SESSION_TIMEOUT_SECONDS;
    public static final ModConfigSpec.IntValue WEB_API_LOGIN_MAX_ATTEMPTS;
    public static final ModConfigSpec.IntValue WEB_API_LOGIN_WINDOW_SECONDS;

    public static final ModConfigSpec.BooleanValue CREDIT_INTEREST_ENABLED;
    public static final ModConfigSpec.IntValue CREDIT_INTEREST_DAILY_RATE_BPS;
    public static final ModConfigSpec.ConfigValue<String> CREDIT_INTEREST_MODE;
    public static final ModConfigSpec.IntValue CREDIT_INTEREST_GRACE_DAYS;
    public static final ModConfigSpec.IntValue CREDIT_INTEREST_APPLICATION_HOUR;
    public static final ModConfigSpec.IntValue CREDIT_INVOICE_DUE_DAY;
    public static final ModConfigSpec.IntValue CREDIT_INVOICE_AVAILABLE_DAYS_BEFORE;

    public static final ModConfigSpec.BooleanValue BANK_GOLD_ENABLED;
    public static final ModConfigSpec.LongValue BANK_GOLD_NUGGET_VALUE;
    public static final ModConfigSpec.LongValue BANK_GOLD_DAILY_LIMIT_PER_PLAYER;
    public static final ModConfigSpec.LongValue BANK_GOLD_DAILY_GLOBAL_LIMIT;
    public static final ModConfigSpec.BooleanValue BANK_GOLD_ALLOW_ACCOUNT_CREDIT;
    public static final ModConfigSpec.BooleanValue BANK_GOLD_ALLOW_PHYSICAL_NOTES;
    public static final ModConfigSpec.BooleanValue BANK_GOLD_ALLOW_CREDIT_PURCHASE;
    public static final ModConfigSpec.LongValue BANK_CARD_ISSUE_FEE;
    public static final ModConfigSpec.IntValue BANK_MAX_ACCOUNTS_PER_PLAYER;
    public static final ModConfigSpec.LongValue BANK_ACCOUNT_OPENING_FEE;
    public static final ModConfigSpec.LongValue MAIL_PRICE_PER_OCCUPIED_SLOT;

    public static final ModConfigSpec.BooleanValue DYNAMIC_PRICING_ENABLED;
    public static final ModConfigSpec.IntValue DYNAMIC_PRICING_RECOVERY_HOUR;
    public static final ModConfigSpec.LongValue DYNAMIC_PRICING_DEFAULT_QUANTITY_PER_LEVEL;
    public static final ModConfigSpec.IntValue DYNAMIC_PRICING_DEFAULT_DEMAND_INCREASE_BPS;
    public static final ModConfigSpec.IntValue DYNAMIC_PRICING_DEFAULT_SUPPLY_DECREASE_BPS;
    public static final ModConfigSpec.IntValue DYNAMIC_PRICING_DEFAULT_RECOVERY_LEVELS_PER_IDLE_DAY;

    public static final ModConfigSpec.IntValue CLAN_MEMBER_LIMIT;
    public static final ModConfigSpec.IntValue PRIVATE_PROPERTY_MEMBER_LIMIT;
    public static final ModConfigSpec.IntValue CLAIM_MIN_CHUNKS;
    public static final ModConfigSpec.IntValue CLAIM_MAX_CHUNKS;
    public static final ModConfigSpec.LongValue CLAIM_UPGRADE_BASE_PRICE;
    public static final ModConfigSpec.IntValue CLAIM_UPGRADE_MIN_PERCENTAGE;
    public static final ModConfigSpec.IntValue CLAIM_UPGRADE_MAX_PERCENTAGE;
    public static final ModConfigSpec.IntValue CLAIM_EXTERNAL_DISTANCE;
    public static final ModConfigSpec.IntValue PRIVATE_PROPERTY_CLAIM_DISTANCE;
    public static final ModConfigSpec.IntValue CLAN_LEADERSHIP_INACTIVITY_DAYS;
    public static final ModConfigSpec.IntValue CLAN_LEADERSHIP_CANDIDATE_ACTIVE_DAYS;
    public static final ModConfigSpec.IntValue CLAN_MAX_TERRITORIES;
    public static final ModConfigSpec.IntValue PRIVATE_PROPERTY_MAX_TERRITORIES;
    public static final ModConfigSpec.IntValue GROUP_NAME_MIN_LENGTH;
    public static final ModConfigSpec.IntValue GROUP_NAME_MAX_LENGTH;
    public static final ModConfigSpec.IntValue LOCATION_NAME_MAX_LENGTH;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_OVERWORLD_BASE;
    public static final ModConfigSpec.IntValue CLAIM_PRICE_OVERWORLD_INTERVAL;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_OVERWORLD_LINEAR;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_OVERWORLD_PROGRESSIVE;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_NETHER_BASE;
    public static final ModConfigSpec.IntValue CLAIM_PRICE_NETHER_INTERVAL;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_NETHER_LINEAR;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_NETHER_PROGRESSIVE;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_END_BASE;
    public static final ModConfigSpec.IntValue CLAIM_PRICE_END_INTERVAL;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_END_LINEAR;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_END_PROGRESSIVE;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_OTHER_BASE;
    public static final ModConfigSpec.IntValue CLAIM_PRICE_OTHER_INTERVAL;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_OTHER_LINEAR;
    public static final ModConfigSpec.LongValue CLAIM_PRICE_OTHER_PROGRESSIVE;
    public static final ModConfigSpec.LongValue ANCHOR_BASE_PRICE;
    public static final ModConfigSpec.IntValue ANCHOR_LAND_PERCENTAGE;
    public static final ModConfigSpec.IntValue ANCHOR_DEFAULT_MINECRAFT_DAYS;
    public static final ModConfigSpec.IntValue ANCHOR_MAX_MINECRAFT_DAYS;
    public static final ModConfigSpec.IntValue ANCHOR_CLAN_TAX_MULTIPLIER_PERCENTAGE;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment(
                "============================================================",
                "BANCO DE DADOS",
                "Padrao: SQLite local, sem servidor externo.",
                "Use type = \"postgresql\" ou \"pgsql\" apenas se quiser PostgreSQL.",
                "============================================================")
                .push("database");
        DATABASE_TYPE = BUILDER
                .comment("Banco ativo: sqlite/sql ou postgresql/pgsql. Padrao: sqlite.")
                .define("type", "sqlite");
        DATABASE_SQLITE_FILE = BUILDER
                .comment("SQLite ativo por padrao. Caminho relativo ao save do mundo.")
                .define("sqliteFile", "economia/economia.db");

        DATABASE_HOST = BUILDER
                .comment(
                        "PostgreSQL - configuracoes abaixo sao ignoradas enquanto type = \"sqlite\".",
                        "Para usar PostgreSQL, altere type para \"postgresql\" ou \"pgsql\".")
                .define("host", "127.0.0.1");
        DATABASE_PORT = BUILDER.defineInRange("port", 5432, 1, 65535);
        DATABASE_NAME = BUILDER.define("name", "economia");
        DATABASE_USERNAME = BUILDER.define("username", "economia");
        DATABASE_PASSWORD = BUILDER.define("password", "");
        DATABASE_SSL = BUILDER.define("ssl", false);

        DATABASE_CONNECTION_TIMEOUT_MS = BUILDER
                .comment("Timeouts compartilhados pelos bancos suportados.")
                .defineInRange("connectionTimeout", 10000, 1000, 120000);
        DATABASE_QUERY_TIMEOUT_MS = BUILDER.defineInRange("queryTimeout", 10000, 1000, 120000);
        DATABASE_POOL_MINIMUM = BUILDER
                .comment("Pool padrao para SQLite: 1 conexao. Em PostgreSQL, o maximum pode ser aumentado.")
                .defineInRange("pool.minimum", 1, 0, 64);
        DATABASE_POOL_MAXIMUM = BUILDER.defineInRange("pool.maximum", 1, 1, 128);
        BUILDER.pop();

        BUILDER.comment(
                "============================================================",
                "ECONOMIA",
                "============================================================")
                .push("economy");
        ECONOMY_TIME_ZONE = BUILDER.define("timeZone", "America/Araguaina");
        ECONOMY_SESSION_TIMEOUT_SECONDS = BUILDER.defineInRange("sessionTimeoutSeconds", 900, 60, 86400);
        BUILDER.pop();

        BUILDER.comment(
                "============================================================",
                "WEB API",
                "Padrao: desativada. Recomendada apenas com PostgreSQL.",
                "============================================================")
                .push("webApi");
        WEB_API_ENABLED = BUILDER
                .comment("false = API/website desativados. Padrao: false.")
                .define("enabled", false);
        WEB_API_BIND = BUILDER
                .comment("Configuracoes abaixo sao ignoradas enquanto enabled = false.")
                .define("bind", "127.0.0.1");
        WEB_API_PORT = BUILDER.defineInRange("port", 8765, 1, 65535);
        WEB_API_ALLOWED_ORIGIN = BUILDER.define("allowedOrigin", "");
        WEB_API_SESSION_TIMEOUT_SECONDS = BUILDER.defineInRange("sessionTimeoutSeconds", 900, 60, 86400);
        WEB_API_LOGIN_MAX_ATTEMPTS = BUILDER.defineInRange("loginMaxAttempts", 5, 1, 100);
        WEB_API_LOGIN_WINDOW_SECONDS = BUILDER.defineInRange("loginWindowSeconds", 300, 30, 86400);
        BUILDER.pop();

        BUILDER.push("credit");
        BUILDER.push("interest");
        CREDIT_INTEREST_ENABLED = BUILDER.define("enabled", true);
        CREDIT_INTEREST_DAILY_RATE_BPS = BUILDER.defineInRange("dailyRateBps", 50, 0, 10000);
        CREDIT_INTEREST_MODE = BUILDER.define("mode", "COMPOUND");
        CREDIT_INTEREST_GRACE_DAYS = BUILDER.defineInRange("graceDays", 1, 0, 365);
        CREDIT_INTEREST_APPLICATION_HOUR = BUILDER.defineInRange("applicationHour", 0, 0, 23);
        BUILDER.pop();
        BUILDER.push("invoice");
        CREDIT_INVOICE_DUE_DAY = BUILDER.defineInRange("dueDay", 15, 1, 28);
        CREDIT_INVOICE_AVAILABLE_DAYS_BEFORE = BUILDER.defineInRange("availableDaysBeforeDue", 1, 0, 28);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("bank");
        BUILDER.push("accounts");
        BANK_MAX_ACCOUNTS_PER_PLAYER = BUILDER.defineInRange("maxAccountsPerPlayer", 3, 1, Integer.MAX_VALUE);
        BANK_ACCOUNT_OPENING_FEE = BUILDER.defineInRange("openingFee", 1_000L, 0L, (long) Integer.MAX_VALUE);
        BUILDER.pop();
        BUILDER.push("gold");
        BANK_GOLD_ENABLED = BUILDER.define("enabled", true);
        BANK_GOLD_NUGGET_VALUE = BUILDER.defineInRange("nuggetValue", 1L, 1L, Long.MAX_VALUE);
        BANK_GOLD_DAILY_LIMIT_PER_PLAYER = BUILDER.defineInRange("dailyLimitPerPlayer", 0L, 0L, Long.MAX_VALUE);
        BANK_GOLD_DAILY_GLOBAL_LIMIT = BUILDER.defineInRange("dailyGlobalLimit", 0L, 0L, Long.MAX_VALUE);
        BANK_GOLD_ALLOW_ACCOUNT_CREDIT = BUILDER.define("allowAccountCredit", true);
        BANK_GOLD_ALLOW_PHYSICAL_NOTES = BUILDER.define("allowPhysicalNotes", true);
        BANK_GOLD_ALLOW_CREDIT_PURCHASE = BUILDER.define("allowCreditPurchase", false);
        BUILDER.pop();
        BUILDER.push("cards");
        BANK_CARD_ISSUE_FEE = BUILDER.defineInRange("issueFee", 10L, 0L, Long.MAX_VALUE);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("mail");
        MAIL_PRICE_PER_OCCUPIED_SLOT = BUILDER.defineInRange("pricePerOccupiedSlot", 50L, 1L, Long.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("bank");
        BUILDER.push("dynamicPricing");
        DYNAMIC_PRICING_ENABLED = BUILDER.define("enabled", true);
        DYNAMIC_PRICING_RECOVERY_HOUR = BUILDER.defineInRange("recoveryHour", 0, 0, 23);
        DYNAMIC_PRICING_DEFAULT_QUANTITY_PER_LEVEL = BUILDER.defineInRange("defaultQuantityPerLevel", 64L, 1L, Long.MAX_VALUE);
        DYNAMIC_PRICING_DEFAULT_DEMAND_INCREASE_BPS = BUILDER.defineInRange("defaultDemandIncreaseBps", 500, 0, 10000);
        DYNAMIC_PRICING_DEFAULT_SUPPLY_DECREASE_BPS = BUILDER.defineInRange("defaultSupplyDecreaseBps", 500, 0, 10000);
        DYNAMIC_PRICING_DEFAULT_RECOVERY_LEVELS_PER_IDLE_DAY = BUILDER.defineInRange("defaultRecoveryLevelsPerIdleDay", 1, 0, 1000);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("groups");
        GROUP_NAME_MIN_LENGTH = BUILDER.defineInRange("nameMinLength", 3, 1, 64);
        GROUP_NAME_MAX_LENGTH = BUILDER.defineInRange("nameMaxLength", 32, 1, 64);
        LOCATION_NAME_MAX_LENGTH = BUILDER.defineInRange("locationNameMaxLength", 64, 1, 64);
        CLAIM_MIN_CHUNKS = BUILDER.defineInRange("claimMinChunks", 4, 1, 100000);
        CLAIM_MAX_CHUNKS = BUILDER.defineInRange("claimMaxChunks", 20, 1, 100000);
        CLAIM_UPGRADE_BASE_PRICE = BUILDER.defineInRange("claimUpgradeBasePrice", 10_000L, 1L, Long.MAX_VALUE);
        CLAIM_UPGRADE_MIN_PERCENTAGE = BUILDER.defineInRange("claimUpgradeMinPercentage", 10, 0, 10000);
        CLAIM_UPGRADE_MAX_PERCENTAGE = BUILDER.defineInRange("claimUpgradeMaxPercentage", 30, 0, 10000);
        BUILDER.push("clan");
        CLAN_MEMBER_LIMIT = BUILDER.defineInRange("memberLimit", 20, 1, 1000);
        CLAN_MAX_TERRITORIES = BUILDER.defineInRange("maxTerritories", 3, 1, Integer.MAX_VALUE);
        CLAN_LEADERSHIP_INACTIVITY_DAYS = BUILDER.defineInRange("leadershipInactivityDays", 20, 1, 3650);
        CLAN_LEADERSHIP_CANDIDATE_ACTIVE_DAYS = BUILDER.defineInRange("leadershipCandidateActiveDays", 3, 1, 3650);
        BUILDER.pop();
        BUILDER.push("privateProperty");
        PRIVATE_PROPERTY_MEMBER_LIMIT = BUILDER.defineInRange("memberLimit", 5, 1, 1000);
        PRIVATE_PROPERTY_MAX_TERRITORIES = BUILDER.defineInRange("maxTerritories", 3, 1, Integer.MAX_VALUE);
        PRIVATE_PROPERTY_CLAIM_DISTANCE = BUILDER.defineInRange("separateTerritoryDistance", 1, 0, 1000);
        BUILDER.pop();
        BUILDER.push("claims");
        CLAIM_EXTERNAL_DISTANCE = BUILDER.defineInRange("externalDistance", 3, 0, 1000);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("claimPrice");
        BUILDER.push("overworld");
        CLAIM_PRICE_OVERWORLD_BASE = BUILDER.defineInRange("base", 5_000L, 0L, Long.MAX_VALUE);
        CLAIM_PRICE_OVERWORLD_INTERVAL = BUILDER.defineInRange("interval", 1_000, 1, Integer.MAX_VALUE);
        CLAIM_PRICE_OVERWORLD_LINEAR = BUILDER.defineInRange("linearIncrease", 500L, 0L, Long.MAX_VALUE);
        CLAIM_PRICE_OVERWORLD_PROGRESSIVE = BUILDER.defineInRange("progressiveIncrease", 20L, 0L, Long.MAX_VALUE);
        BUILDER.pop();
        BUILDER.push("nether");
        CLAIM_PRICE_NETHER_BASE = BUILDER.defineInRange("base", 10_000L, 0L, Long.MAX_VALUE);
        CLAIM_PRICE_NETHER_INTERVAL = BUILDER.defineInRange("interval", 500, 1, Integer.MAX_VALUE);
        CLAIM_PRICE_NETHER_LINEAR = BUILDER.defineInRange("linearIncrease", 800L, 0L, Long.MAX_VALUE);
        CLAIM_PRICE_NETHER_PROGRESSIVE = BUILDER.defineInRange("progressiveIncrease", 30L, 0L, Long.MAX_VALUE);
        BUILDER.pop();
        BUILDER.push("end");
        CLAIM_PRICE_END_BASE = BUILDER.defineInRange("base", 15_000L, 0L, Long.MAX_VALUE);
        CLAIM_PRICE_END_INTERVAL = BUILDER.defineInRange("interval", 1_000, 1, Integer.MAX_VALUE);
        CLAIM_PRICE_END_LINEAR = BUILDER.defineInRange("linearIncrease", 1_000L, 0L, Long.MAX_VALUE);
        CLAIM_PRICE_END_PROGRESSIVE = BUILDER.defineInRange("progressiveIncrease", 35L, 0L, Long.MAX_VALUE);
        BUILDER.pop();
        BUILDER.push("otherDimensions");
        CLAIM_PRICE_OTHER_BASE = BUILDER.defineInRange("base", 10_000L, 0L, Long.MAX_VALUE);
        CLAIM_PRICE_OTHER_INTERVAL = BUILDER.defineInRange("interval", 1_000, 1, Integer.MAX_VALUE);
        CLAIM_PRICE_OTHER_LINEAR = BUILDER.defineInRange("linearIncrease", 750L, 0L, Long.MAX_VALUE);
        CLAIM_PRICE_OTHER_PROGRESSIVE = BUILDER.defineInRange("progressiveIncrease", 25L, 0L, Long.MAX_VALUE);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("claimAnchor");
        ANCHOR_BASE_PRICE = BUILDER.defineInRange("basePrice", 1_000L, 0L, Long.MAX_VALUE);
        ANCHOR_LAND_PERCENTAGE = BUILDER.defineInRange("landPercentage", 50, 0, 10_000);
        ANCHOR_DEFAULT_MINECRAFT_DAYS = BUILDER.defineInRange("defaultMinecraftDays", 25, 1, Integer.MAX_VALUE);
        ANCHOR_MAX_MINECRAFT_DAYS = BUILDER.defineInRange("maxMinecraftDays", 1_000, 1, Integer.MAX_VALUE);
        ANCHOR_CLAN_TAX_MULTIPLIER_PERCENTAGE = BUILDER.defineInRange(
                "clanTaxMultiplierPercentage", 200, 101, 10_000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private EconomyServerConfig() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC);
    }
}
