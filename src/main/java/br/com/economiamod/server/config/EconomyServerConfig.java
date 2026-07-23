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
    public static final ModConfigSpec.IntValue DATABASE_POOL_MINIMUM;
    public static final ModConfigSpec.IntValue DATABASE_POOL_MAXIMUM;
    public static final ModConfigSpec.IntValue DATABASE_CONNECTION_TIMEOUT_MS;
    public static final ModConfigSpec.IntValue DATABASE_QUERY_TIMEOUT_MS;

    public static final ModConfigSpec.ConfigValue<String> ECONOMY_TIME_ZONE;
    public static final ModConfigSpec.IntValue ECONOMY_SESSION_TIMEOUT_SECONDS;

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

    public static final ModConfigSpec.BooleanValue DYNAMIC_PRICING_ENABLED;
    public static final ModConfigSpec.IntValue DYNAMIC_PRICING_RECOVERY_HOUR;
    public static final ModConfigSpec.LongValue DYNAMIC_PRICING_DEFAULT_QUANTITY_PER_LEVEL;
    public static final ModConfigSpec.IntValue DYNAMIC_PRICING_DEFAULT_DEMAND_INCREASE_BPS;
    public static final ModConfigSpec.IntValue DYNAMIC_PRICING_DEFAULT_SUPPLY_DECREASE_BPS;
    public static final ModConfigSpec.IntValue DYNAMIC_PRICING_DEFAULT_RECOVERY_LEVELS_PER_IDLE_DAY;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("database");
        DATABASE_TYPE = BUILDER.define("type", "postgresql");
        DATABASE_HOST = BUILDER.define("host", "localhost");
        DATABASE_PORT = BUILDER.defineInRange("port", 5432, 1, 65535);
        DATABASE_NAME = BUILDER.define("name", "economia");
        DATABASE_USERNAME = BUILDER.define("username", "economia");
        DATABASE_PASSWORD = BUILDER.define("password", "");
        DATABASE_SSL = BUILDER.define("ssl", false);
        DATABASE_POOL_MINIMUM = BUILDER.defineInRange("pool.minimum", 1, 0, 64);
        DATABASE_POOL_MAXIMUM = BUILDER.defineInRange("pool.maximum", 10, 1, 128);
        DATABASE_CONNECTION_TIMEOUT_MS = BUILDER.defineInRange("connectionTimeout", 10000, 1000, 120000);
        DATABASE_QUERY_TIMEOUT_MS = BUILDER.defineInRange("queryTimeout", 10000, 1000, 120000);
        BUILDER.pop();

        BUILDER.push("economy");
        ECONOMY_TIME_ZONE = BUILDER.define("timeZone", "America/Araguaina");
        ECONOMY_SESSION_TIMEOUT_SECONDS = BUILDER.defineInRange("sessionTimeoutSeconds", 900, 60, 86400);
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

        SPEC = BUILDER.build();
    }

    private EconomyServerConfig() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC);
    }
}
