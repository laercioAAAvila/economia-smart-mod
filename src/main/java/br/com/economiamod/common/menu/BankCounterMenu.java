package br.com.economiamod.common.menu;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.gold.GoldUnitConverter;
import br.com.economiamod.common.money.BanknoteStackPlan;
import br.com.economiamod.common.network.BankCounterActionPayload;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.server.cash.CashInventoryService;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.gold.GoldDynamicPricingService;
import br.com.economiamod.server.gold.GoldExchangeResultType;
import br.com.economiamod.server.gold.GoldExchangeService;
import br.com.economiamod.server.gold.GoldPriceSnapshot;
import br.com.economiamod.server.operation.EconomyOperationService;
import br.com.economiamod.server.operation.EconomyOperationState;
import br.com.economiamod.server.operation.EconomyOperationType;
import br.com.economiamod.server.operation.OperationStartResult;
import br.com.economiamod.server.operation.OperationStartType;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class BankCounterMenu extends AbstractContainerMenu {
    private static final int GOLD_SLOTS = 9;
    private static final int CARD_SLOT_INDEX = 0;
    private static final int GOLD_START = 1;
    private static final int GOLD_END = GOLD_START + GOLD_SLOTS;
    private static final int PLAYER_START = GOLD_END;
    private static final int PLAYER_END = PLAYER_START + 36;
    private static final int MAX_STACK_SIZE = 64;

    private final SimpleContainer cardContainer = new SimpleContainer(1);
    private final SimpleContainer goldContainer = new SimpleContainer(GOLD_SLOTS);
    private final CardItemDataService cardItemDataService = new CardItemDataService();
    private final CardValidationService cardValidationService = new CardValidationService(cardItemDataService);
    private final CashInventoryService cashInventoryService = new CashInventoryService();
    private final GoldExchangeService goldExchangeService = new GoldExchangeService();
    private final GoldDynamicPricingService goldPricingService = new GoldDynamicPricingService();
    private final EconomyOperationService operationService = new EconomyOperationService();
    private final BlockPos accessPos;
    private final Block expectedBlock;
    private final UUID commercialBlockId;
    private int cardPresentFlag;
    private int goldBuyBaseNuggetValue = 1;
    private int goldBuyBps = 10_000;
    private int goldBuyNuggetValue = 1;
    private int goldBuyIngotValue = 9;
    private int goldBuyBlockValue = 81;

    public BankCounterMenu(int containerId, Inventory inventory) {
        super(ModMenus.BANK_COUNTER.get(), containerId);
        this.accessPos = null;
        this.expectedBlock = null;
        this.commercialBlockId = null;
        addDataSlots();
        addCardSlot();
        addGoldSlots();
        addPlayerInventorySlots(inventory);
        refreshGoldPricing(inventory.player);
    }

    public BankCounterMenu(int containerId, Inventory inventory, BlockPos accessPos) {
        this(containerId, inventory, accessPos, null);
    }

    public BankCounterMenu(int containerId, Inventory inventory, BlockPos accessPos, UUID commercialBlockId) {
        super(ModMenus.BANK_COUNTER.get(), containerId);
        this.accessPos = accessPos;
        this.expectedBlock = ModBlocks.BANK_COUNTER.get();
        this.commercialBlockId = commercialBlockId;
        addDataSlots();
        addCardSlot();
        addGoldSlots();
        addPlayerInventorySlots(inventory);
        refreshGoldPricing(inventory.player);
    }

    public BankCounterMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack current = slot.getItem();
        ItemStack original = current.copy();

        if (slotIndex < PLAYER_START) {
            if (!moveItemStackTo(current, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (cardItemDataService.isValidCardItem(current)) {
            if (!moveItemStackTo(current, CARD_SLOT_INDEX, CARD_SLOT_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (GoldUnitConverter.isMonetaryGold(current)) {
            if (!moveItemStackTo(current, GOLD_START, GOLD_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return original;
    }

    public boolean hasCard() {
        return cardPresentFlag == 1;
    }

    public long goldBuyNuggetValue() {
        return goldBuyNuggetValue;
    }

    public long goldBaseNuggetValue() {
        return goldBuyBaseNuggetValue;
    }

    public long goldBaseIngotValue() {
        return safeMultiply(goldBuyBaseNuggetValue, GoldUnitConverter.NUGGET_UNITS_PER_INGOT);
    }

    public long goldBaseBlockValue() {
        return safeMultiply(goldBuyBaseNuggetValue, GoldUnitConverter.NUGGET_UNITS_PER_BLOCK);
    }

    public long goldBuyPercent() {
        return Math.max(1L, Math.round(goldBuyBps / 100.0D));
    }

    public long goldSellPercent() {
        return 100L;
    }

    public long goldBuyIngotValue() {
        return goldBuyIngotValue;
    }

    public long goldBuyBlockValue() {
        return goldBuyBlockValue;
    }

    public long goldInventoryNuggetUnits() {
        long total = 0L;
        for (int slot = 0; slot < goldContainer.getContainerSize(); slot++) {
            ItemStack stack = goldContainer.getItem(slot);
            if (!GoldUnitConverter.isMonetaryGold(stack)) {
                continue;
            }
            try {
                total = Math.addExact(total, GoldUnitConverter.nuggetUnits(stack));
            } catch (ArithmeticException exception) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    public long estimatedMintPayment() {
        try {
            return goldPricingService.moneyAmount(goldInventoryNuggetUnits(), goldBuyBaseNuggetValue, goldBuyBps);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    public void mintGold(ServerPlayer player) {
        mintGold(player, UUID.randomUUID());
    }

    public void mintGold(ServerPlayer player, UUID requestId) {
        ItemStack cardStack = cardContainer.getItem(0);
        CardValidationResult card = null;
        if (!cardStack.isEmpty()) {
            card = validateCard(player);
            if (card == null) {
                return;
            }
        }

        List<ItemStack> goldStacks = new ArrayList<>();
        for (int slot = 0; slot < goldContainer.getContainerSize(); slot++) {
            ItemStack stack = goldContainer.getItem(slot);
            if (!GoldUnitConverter.isMonetaryGold(stack)) {
                continue;
            }
            goldStacks.add(stack.copy());
        }
        if (goldStacks.isEmpty()) {
            return;
        }

        try {
            String requestKey = stableRequestId(requestId);
            String transactionKey = card == null
                    ? "bank-counter-cash:" + player.getUUID() + ":" + requestKey
                    : "bank-counter:" + player.getUUID() + ":" + requestKey;
            long nuggetUnits = goldInventoryNuggetUnits();
            String operationKey = "operation:" + transactionKey;
            String payload = "mode=mint;account=" + (card == null ? "cash" : card.accountId())
                    + ";nuggetUnits=" + nuggetUnits + ";block=" + commercialBlockId;
            OperationStartResult start = operationService.begin(operationKey, EconomyOperationType.GOLD_MINT,
                    player.getUUID(), payload);
            if (start.type() == OperationStartType.DUPLICATE_COMPLETED) {
                player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
                return;
            }
            if (start.type() != OperationStartType.CREATED) {
                player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
                return;
            }
            if (!operationService.mark(operationKey, EconomyOperationState.ITEMS_RESERVED)) {
                operationService.markReconciliationRequired(operationKey, "gold mint could not enter reserved state");
                player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
                return;
            }

            var result = card == null
                    ? goldExchangeService.mintToCash(player.getUUID(), goldStacks, commercialBlockId, transactionKey)
                    : goldExchangeService.mintToAccount(player.getUUID(), card.accountId(), goldStacks, commercialBlockId, transactionKey);
            if (result.type() == GoldExchangeResultType.COMPLETED) {
                if (!operationService.mark(operationKey, EconomyOperationState.SQL_COMMITTED)) {
                    operationService.markReconciliationRequired(operationKey, "gold mint committed but state transition failed");
                }
                if (card == null && result.moneyAmount() > 0L) {
                    BanknoteStackPlan plan = cashInventoryService.buildWithdrawalPlan(result.moneyAmount());
                    cashInventoryService.insert(player, plan);
                }
                for (int slot = 0; slot < goldContainer.getContainerSize(); slot++) {
                    if (GoldUnitConverter.isMonetaryGold(goldContainer.getItem(slot))) {
                        goldContainer.setItem(slot, ItemStack.EMPTY);
                    }
                }
                operationService.mark(operationKey, EconomyOperationState.ITEMS_DELIVERED);
                operationService.mark(operationKey, EconomyOperationState.COMPLETED);
                refreshGoldPricing(player);
                player.sendSystemMessage(Component.translatable("commands.economia.bank.gold.counter_mint.success", result.moneyAmount()));
            } else if (result.type() == GoldExchangeResultType.DUPLICATE_COMPLETED) {
                operationService.markReconciliationRequired(operationKey, "financial transaction already existed");
                player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
            } else {
                operationService.mark(operationKey, EconomyOperationState.ROLLED_BACK);
                player.sendSystemMessage(Component.translatable("commands.economia.bank.gold." + result.type().name().toLowerCase()));
            }
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao trocar ouro da bancada.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
        }
    }

    public void refreshGoldPricing(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        try {
            GoldPriceSnapshot snapshot = goldPricingService.currentSnapshot();
            goldBuyBaseNuggetValue = clampToInt(snapshot.baseNuggetValue());
            goldBuyBps = clampToInt(snapshot.buyBps());
            goldBuyNuggetValue = clampToInt(snapshot.nuggetBuyValue());
            goldBuyIngotValue = clampToInt(snapshot.ingotBuyValue());
            goldBuyBlockValue = clampToInt(snapshot.blockBuyValue());
            broadcastChanges();
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao atualizar preco dinamico do ouro.", exception);
            try {
                goldBuyBaseNuggetValue = clampToInt(EconomyServerConfig.BANK_GOLD_NUGGET_VALUE.get());
                goldBuyBps = 10_000;
                goldBuyNuggetValue = goldBuyBaseNuggetValue;
                goldBuyIngotValue = clampToInt(safeMultiply(goldBuyBaseNuggetValue, GoldUnitConverter.NUGGET_UNITS_PER_INGOT));
                goldBuyBlockValue = clampToInt(safeMultiply(goldBuyBaseNuggetValue, GoldUnitConverter.NUGGET_UNITS_PER_BLOCK));
            } catch (RuntimeException ignored) {
                goldBuyBaseNuggetValue = 1;
                goldBuyBps = 10_000;
                goldBuyNuggetValue = 1;
                goldBuyIngotValue = 9;
                goldBuyBlockValue = 81;
            }
        }
    }

    public void redeemGold(ServerPlayer player, int amount, int unit) {
        redeemGold(player, amount, unit, UUID.randomUUID());
    }

    public void redeemGold(ServerPlayer player, int amount, int unit, UUID requestId) {
        CardValidationResult card = validateCard(player);
        if (card == null || amount <= 0) {
            return;
        }
        try {
            GoldRedeemUnit redeemUnit = GoldRedeemUnit.byId(unit);
            long goldUnits = Math.multiplyExact((long) amount, redeemUnit.nuggetUnits);
            String transactionKey = "bank-counter-redeem:" + player.getUUID() + ":" + stableRequestId(requestId);
            String operationKey = "operation:" + transactionKey;
            String payload = "mode=redeem;account=" + card.accountId() + ";amount=" + amount
                    + ";unit=" + unit + ";nuggetUnits=" + goldUnits + ";block=" + commercialBlockId;
            OperationStartResult start = operationService.begin(operationKey, EconomyOperationType.GOLD_REDEEM,
                    player.getUUID(), payload);
            if (start.type() == OperationStartType.DUPLICATE_COMPLETED) {
                player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
                return;
            }
            if (start.type() != OperationStartType.CREATED) {
                player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
                return;
            }
            if (!operationService.mark(operationKey, EconomyOperationState.ITEMS_RESERVED)) {
                operationService.markReconciliationRequired(operationKey, "gold redeem could not enter reserved state");
                player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
                return;
            }

            var result = goldExchangeService.redeemFromAccount(player.getUUID(), card.accountId(), goldUnits,
                    commercialBlockId, transactionKey);
            if (result.type() == GoldExchangeResultType.COMPLETED) {
                if (!operationService.mark(operationKey, EconomyOperationState.SQL_COMMITTED)) {
                    operationService.markReconciliationRequired(operationKey, "gold redeem committed but state transition failed");
                }
                giveGold(player, amount, redeemUnit.item);
                operationService.mark(operationKey, EconomyOperationState.ITEMS_DELIVERED);
                operationService.mark(operationKey, EconomyOperationState.COMPLETED);
                refreshGoldPricing(player);
                player.sendSystemMessage(Component.translatable("commands.economia.bank.gold.redeem.success", result.goldNuggetUnits(), result.moneyAmount(), result.balanceAfter()));
            } else if (result.type() == GoldExchangeResultType.DUPLICATE_COMPLETED) {
                operationService.markReconciliationRequired(operationKey, "financial transaction already existed");
                player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
            } else {
                operationService.mark(operationKey, EconomyOperationState.ROLLED_BACK);
                refreshGoldPricing(player);
                player.sendSystemMessage(Component.translatable("commands.economia.bank.gold." + result.type().name().toLowerCase()));
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao resgatar ouro da bancada.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
        } catch (ArithmeticException exception) {
            player.sendSystemMessage(Component.translatable("commands.economia.bank.gold.invalid_gold"));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, cardContainer);
        clearContainer(player, goldContainer);
    }

    private void addDataSlots() {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return cardContainer.getItem(0).isEmpty() ? 0 : 1;
            }

            @Override
            public void set(int value) {
                cardPresentFlag = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return goldBuyBaseNuggetValue;
            }

            @Override
            public void set(int value) {
                goldBuyBaseNuggetValue = Math.max(1, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return goldBuyBps;
            }

            @Override
            public void set(int value) {
                goldBuyBps = Math.max(1, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return goldBuyNuggetValue;
            }

            @Override
            public void set(int value) {
                goldBuyNuggetValue = Math.max(1, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return goldBuyIngotValue;
            }

            @Override
            public void set(int value) {
                goldBuyIngotValue = Math.max(1, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return goldBuyBlockValue;
            }

            @Override
            public void set(int value) {
                goldBuyBlockValue = Math.max(1, value);
            }
        });
    }

    private void addCardSlot() {
        addSlot(new Slot(cardContainer, 0, 34, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return cardItemDataService.isValidCardItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    private void addGoldSlots() {
        for (int slot = 0; slot < GOLD_SLOTS; slot++) {
            addSlot(new Slot(goldContainer, slot, 72 + slot * 18, 50) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GoldUnitConverter.isMonetaryGold(stack);
                }
            });
        }
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 49 + column * 18, 204 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 49 + column * 18, 262));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return CommercialMenuAccess.stillValid(player, accessPos, expectedBlock);
    }

    private CardValidationResult validateCard(ServerPlayer player) {
        try {
            CardValidationResult card = cardValidationService.validate(cardContainer.getItem(0));
            if (card.type() != CardValidationResultType.VALID) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.card_failed"));
                return null;
            }
            return card;
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao validar cartao da bancada.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
            return null;
        }
    }

    private void giveGold(ServerPlayer player, int amount, net.minecraft.world.item.Item item) {
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = new ItemStack(item, Math.min(remaining, MAX_STACK_SIZE));
            remaining -= stack.getCount();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private int clampToInt(long value) {
        return Math.toIntExact(Math.max(1L, Math.min(Integer.MAX_VALUE, value)));
    }

    private long safeMultiply(long value, long multiplier) {
        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return value * multiplier;
    }

    private String stableRequestId(UUID requestId) {
        return (requestId == null ? UUID.randomUUID() : requestId).toString();
    }

    private enum GoldRedeemUnit {
        NUGGET(BankCounterActionPayload.UNIT_NUGGET, GoldUnitConverter.NUGGET_UNITS_PER_NUGGET, Items.GOLD_NUGGET),
        INGOT(BankCounterActionPayload.UNIT_INGOT, GoldUnitConverter.NUGGET_UNITS_PER_INGOT, Items.GOLD_INGOT),
        BLOCK(BankCounterActionPayload.UNIT_BLOCK, GoldUnitConverter.NUGGET_UNITS_PER_BLOCK, Items.GOLD_BLOCK);

        private final int id;
        private final long nuggetUnits;
        private final net.minecraft.world.item.Item item;

        GoldRedeemUnit(int id, long nuggetUnits, net.minecraft.world.item.Item item) {
            this.id = id;
            this.nuggetUnits = nuggetUnits;
            this.item = item;
        }

        private static GoldRedeemUnit byId(int id) {
            for (GoldRedeemUnit unit : values()) {
                if (unit.id == id) {
                    return unit;
                }
            }
            return NUGGET;
        }
    }
}
