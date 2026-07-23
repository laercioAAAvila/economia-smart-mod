package br.com.economiamod.server.gold;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.credit.CreditMath;
import br.com.economiamod.common.gold.GoldUnitConverter;
import br.com.economiamod.server.account.SystemAccountIds;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.transaction.EconomyTransactionType;
import br.com.economiamod.server.transaction.LedgerEntryType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class GoldExchangeService {
    private final GoldExchangeRepository repository = new GoldExchangeRepository();
    private final GoldExchangeWriter writer = new GoldExchangeWriter();
    private final GoldDynamicPricingService pricingService = new GoldDynamicPricingService();

    public GoldExchangeResult mintToAccount(UUID playerUuid, UUID accountId, ItemStack goldStack, UUID commercialBlockId, String idempotencyKey) throws SQLException {
        return mintToAccount(playerUuid, accountId, List.of(goldStack), commercialBlockId, idempotencyKey);
    }

    public GoldExchangeResult mintToAccount(UUID playerUuid, UUID accountId, List<ItemStack> goldStacks, UUID commercialBlockId, String idempotencyKey) throws SQLException {
        long goldUnits;
        try {
            goldUnits = totalGoldUnits(goldStacks);
        } catch (RuntimeException exception) {
            return GoldExchangeResult.invalid(GoldExchangeResultType.INVALID_GOLD);
        }
        try (Connection connection = repository.openConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Optional<GoldExchangeResult> duplicate = writer.completed(connection, idempotencyKey);
                if (duplicate.isPresent()) {
                    connection.commit();
                    return duplicate.get();
                }

                GoldAccountSnapshot account = repository.lockAccount(connection, accountId).orElse(null);
                if (!active(account)) {
                    connection.rollback();
                    return GoldExchangeResult.invalid(GoldExchangeResultType.INACTIVE_ACCOUNT);
                }

                GoldPriceSnapshot price = pricingService.currentBuySnapshot(connection);
                long moneyAmount = pricingService.moneyAmount(goldUnits, price.baseNuggetValue(), price.buyBps());
                UUID transactionId = UUID.randomUUID();
                long balanceAfter = Math.addExact(account.balance(), moneyAmount);
                repository.mintReserve(connection, goldUnits, moneyAmount);
                repository.updateAccountBalance(connection, accountId, balanceAfter);
                writer.insertTransaction(connection, transactionId, idempotencyKey, EconomyTransactionType.GOLD_MINT, moneyAmount, playerUuid, accountId, commercialBlockId);
                writer.insertLedger(connection, transactionId, accountId, LedgerEntryType.CURRENCY_ISSUANCE, moneyAmount, account.balance(), balanceAfter);
                for (ItemStack stack : goldStacks) {
                    if (stack.isEmpty()) {
                        continue;
                    }
                    long stackGoldUnits = GoldUnitConverter.nuggetUnits(stack);
                    writer.insertGoldEntry(connection, transactionId, playerUuid, "MINT", itemId(stack), stack.getCount(), stackGoldUnits, price.nuggetBuyValue(), pricingService.moneyAmount(stackGoldUnits, price.baseNuggetValue(), price.buyBps()), commercialBlockId);
                }
                connection.commit();
                return GoldExchangeResult.completed(goldUnits, moneyAmount, balanceAfter);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public GoldExchangeResult mintToCash(UUID playerUuid, List<ItemStack> goldStacks, UUID commercialBlockId, String idempotencyKey) throws SQLException {
        long goldUnits;
        try {
            goldUnits = totalGoldUnits(goldStacks);
        } catch (RuntimeException exception) {
            return GoldExchangeResult.invalid(GoldExchangeResultType.INVALID_GOLD);
        }
        try (Connection connection = repository.openConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Optional<GoldExchangeResult> duplicate = writer.completed(connection, idempotencyKey);
                if (duplicate.isPresent()) {
                    connection.commit();
                    return duplicate.get();
                }

                GoldAccountSnapshot issuanceAccount = repository.lockAnyAccount(connection, SystemAccountIds.CURRENCY_ISSUANCE).orElse(null);
                if (!active(issuanceAccount)) {
                    connection.rollback();
                    return GoldExchangeResult.invalid(GoldExchangeResultType.INACTIVE_ACCOUNT);
                }

                GoldPriceSnapshot price = pricingService.currentBuySnapshot(connection);
                long moneyAmount = pricingService.moneyAmount(goldUnits, price.baseNuggetValue(), price.buyBps());
                UUID transactionId = UUID.randomUUID();
                long balanceAfter = Math.addExact(issuanceAccount.balance(), moneyAmount);
                repository.mintReserve(connection, goldUnits, moneyAmount);
                repository.updateAccountBalance(connection, SystemAccountIds.CURRENCY_ISSUANCE, balanceAfter);
                writer.insertTransaction(connection, transactionId, idempotencyKey, EconomyTransactionType.GOLD_MINT, moneyAmount, playerUuid, SystemAccountIds.CURRENCY_ISSUANCE, commercialBlockId);
                writer.insertLedger(connection, transactionId, SystemAccountIds.CURRENCY_ISSUANCE, LedgerEntryType.CURRENCY_ISSUANCE, moneyAmount, issuanceAccount.balance(), balanceAfter);
                for (ItemStack stack : goldStacks) {
                    if (stack.isEmpty()) {
                        continue;
                    }
                    long stackGoldUnits = GoldUnitConverter.nuggetUnits(stack);
                    writer.insertGoldEntry(connection, transactionId, playerUuid, "MINT", itemId(stack), stack.getCount(), stackGoldUnits, price.nuggetBuyValue(), pricingService.moneyAmount(stackGoldUnits, price.baseNuggetValue(), price.buyBps()), commercialBlockId);
                }
                connection.commit();
                return GoldExchangeResult.completed(goldUnits, moneyAmount, 0L);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public GoldExchangeResult redeemFromAccount(UUID playerUuid, UUID accountId, long goldUnits, UUID commercialBlockId, String idempotencyKey) throws SQLException {
        long moneyAmount = GoldUnitConverter.moneyAmount(goldUnits, EconomyServerConfig.BANK_GOLD_NUGGET_VALUE.get());

        try (Connection connection = repository.openConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Optional<GoldExchangeResult> duplicate = writer.completed(connection, idempotencyKey);
                if (duplicate.isPresent()) {
                    connection.commit();
                    return duplicate.get();
                }

                GoldAccountSnapshot account = repository.lockAccount(connection, accountId).orElse(null);
                GoldReserveSnapshot reserve = repository.lockReserve(connection);
                if (!active(account)) {
                    connection.rollback();
                    return GoldExchangeResult.invalid(GoldExchangeResultType.INACTIVE_ACCOUNT);
                }
                if (reserve.goldNuggetUnits() < goldUnits) {
                    connection.rollback();
                    return GoldExchangeResult.invalid(GoldExchangeResultType.INSUFFICIENT_RESERVE);
                }
                if (CreditMath.availableBalance(account.balance(), account.principalOutstanding(), account.interestOutstanding()) < moneyAmount) {
                    connection.rollback();
                    return GoldExchangeResult.invalid(GoldExchangeResultType.INSUFFICIENT_BALANCE);
                }

                UUID transactionId = UUID.randomUUID();
                long balanceAfter = account.balance() - moneyAmount;
                repository.redeemReserve(connection, goldUnits, moneyAmount);
                repository.updateAccountBalance(connection, accountId, balanceAfter);
                writer.insertTransaction(connection, transactionId, idempotencyKey, EconomyTransactionType.GOLD_REDEMPTION, moneyAmount, playerUuid, accountId, commercialBlockId);
                writer.insertLedger(connection, transactionId, accountId, LedgerEntryType.CURRENCY_REDEMPTION, moneyAmount, account.balance(), balanceAfter);
                writer.insertGoldEntry(connection, transactionId, playerUuid, "REDEMPTION", "minecraft:gold_nugget", goldUnits, goldUnits, EconomyServerConfig.BANK_GOLD_NUGGET_VALUE.get(), moneyAmount, commercialBlockId);
                connection.commit();
                return GoldExchangeResult.completed(goldUnits, moneyAmount, balanceAfter);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private boolean active(GoldAccountSnapshot account) {
        return account != null && AccountStatus.ACTIVE.name().equals(account.status());
    }

    private long totalGoldUnits(List<ItemStack> goldStacks) {
        if (goldStacks.isEmpty()) {
            throw new IllegalArgumentException("goldStacks cannot be empty");
        }
        long total = 0L;
        boolean hasGold = false;
        for (ItemStack stack : goldStacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!GoldUnitConverter.isMonetaryGold(stack)) {
                throw new IllegalArgumentException("item is not monetary gold");
            }
            hasGold = true;
            total = Math.addExact(total, GoldUnitConverter.nuggetUnits(stack));
        }
        if (!hasGold) {
            throw new IllegalArgumentException("goldStacks cannot be empty");
        }
        return total;
    }

    private String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
