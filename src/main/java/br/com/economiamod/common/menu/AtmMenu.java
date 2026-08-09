package br.com.economiamod.common.menu;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.gold.GoldUnitConverter;
import br.com.economiamod.common.invoice.InvoiceItemDataService;
import br.com.economiamod.common.invoice.ClaimInvoiceItemDataService;
import br.com.economiamod.common.network.AtmSessionStatePayload;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.server.card.CardSecurityService;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.gold.GoldDynamicPricingService;
import br.com.economiamod.server.gold.GoldPriceSnapshot;
import br.com.economiamod.server.invoice.InvoiceOpenEntry;
import br.com.economiamod.server.invoice.InvoiceQueryService;
import br.com.economiamod.server.session.BankSession;
import br.com.economiamod.server.session.BankSessionService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AtmMenu extends AbstractContainerMenu {
    public static final int CARD_SLOT_INDEX = 0;
    public static final int INVOICE_SLOT_INDEX = 1;
    private static final int PLAYER_INVENTORY_START = 2;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final SimpleContainer cardContainer = new SimpleContainer(1);
    private final SimpleContainer invoiceContainer = new SimpleContainer(1);
    private final CardItemDataService cardItemDataService = new CardItemDataService();
    private final InvoiceItemDataService invoiceItemDataService = new InvoiceItemDataService();
    private final ClaimInvoiceItemDataService claimInvoiceItemDataService = new ClaimInvoiceItemDataService();
    private final InvoiceQueryService invoiceQueryService = new InvoiceQueryService();
    private final CardSecurityService cardSecurityService = new CardSecurityService();
    private final GoldDynamicPricingService goldPricingService = new GoldDynamicPricingService();
    private final BlockPos accessPos;
    private final Block expectedBlock;
    private final Player menuPlayer;
    private boolean slotsVisible = true;
    private PlayerInventoryMode playerInventoryMode = PlayerInventoryMode.FULL;
    private CardSlotMode cardSlotMode = CardSlotMode.LOGIN;
    private String sessionAccountNumber = "";
    private int goldBuyBaseNuggetValue = 1;
    private int goldBuyBps = 10_000;
    private int goldBuyNuggetValue = 1;
    private int goldBuyIngotValue = 9;
    private int goldBuyBlockValue = 81;
    private int cardIssueFee = 10;

    public AtmMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null);
    }

    public AtmMenu(int containerId, Inventory inventory, BlockPos accessPos) {
        this(containerId, inventory, accessPos, ModBlocks.ATM.get());
        syncSessionState(inventory.player);
    }

    private AtmMenu(int containerId, Inventory inventory, BlockPos accessPos, Block expectedBlock) {
        super(ModMenus.ATM.get(), containerId);
        this.accessPos = accessPos;
        this.expectedBlock = expectedBlock;
        this.menuPlayer = inventory.player;
        addDataSlots();
        addCardSlot();
        addInvoiceSlot();
        addPlayerInventorySlots(inventory);
        refreshGoldPricing(inventory.player);
    }

    public AtmMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory);
    }

    public ItemStack cardStack() {
        return cardContainer.getItem(0);
    }

    public ItemStack invoiceStack() {
        return invoiceContainer.getItem(0);
    }

    public void clearCardSlot() {
        cardContainer.setItem(0, ItemStack.EMPTY);
    }

    public void clearInvoiceSlot() {
        invoiceContainer.setItem(0, ItemStack.EMPTY);
        broadcastChanges();
    }

    public void returnCardToPlayer(Player player) {
        ItemStack stack = cardContainer.removeItemNoUpdate(0);
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        broadcastChanges();
    }

    public void setSlotsVisible(boolean slotsVisible) {
        this.slotsVisible = slotsVisible;
    }

    public void setPlayerInventoryMode(PlayerInventoryMode playerInventoryMode) {
        this.playerInventoryMode = playerInventoryMode == null ? PlayerInventoryMode.FULL : playerInventoryMode;
    }

    public void setCardSlotMode(CardSlotMode cardSlotMode) {
        this.cardSlotMode = cardSlotMode == null ? CardSlotMode.LOGIN : cardSlotMode;
    }

    public void refreshInvoiceSlot(UUID accountId) {
        if (accountId == null || cardSlotMode != CardSlotMode.CREDIT || !invoiceWindowOpen()) {
            return;
        }
        try {
            var summary = invoiceQueryService.accountInvoice(accountId).orElse(null);
            if (summary == null || summary.totalDebt() <= 0L || summary.openEntries().isEmpty()) {
                return;
            }
            InvoiceOpenEntry oldest = summary.openEntries().get(0);
            ItemStack current = invoiceContainer.getItem(0);
            if (claimInvoiceItemDataService.read(current).isPresent()) {
                return;
            }
            var currentInvoice = invoiceItemDataService.read(current).orElse(null);
            if (currentInvoice != null && accountId.equals(currentInvoice.accountId()) && oldest.entryId().equals(currentInvoice.entryId()) && currentInvoice.amount() == oldest.remainingAmount()) {
                return;
            }
            if (current.isEmpty() || invoiceItemDataService.isInvoiceItem(current)) {
                invoiceContainer.setItem(0, invoiceItemDataService.create(accountId, oldest.entryId(), oldest.remainingAmount()));
                broadcastChanges();
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao emitir fatura no ATM.", exception);
        }
    }

    public void setSessionAccountNumber(String sessionAccountNumber) {
        this.sessionAccountNumber = sessionAccountNumber == null ? "" : sessionAccountNumber;
    }

    public long goldBuyNuggetValue() {
        return goldBuyNuggetValue;
    }

    public long goldBuyIngotValue() {
        return goldBuyIngotValue;
    }

    public long goldBuyBlockValue() {
        return goldBuyBlockValue;
    }

    public long goldBuyPercent() {
        return Math.max(1L, Math.round(goldBuyBps / 100.0D));
    }

    public long goldSellPercent() {
        return 100L;
    }

    public long cardIssueFee() {
        return cardIssueFee;
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
            goldBuyBaseNuggetValue = clampToInt(EconomyServerConfig.BANK_GOLD_NUGGET_VALUE.get());
            goldBuyBps = 10_000;
            goldBuyNuggetValue = goldBuyBaseNuggetValue;
            goldBuyIngotValue = clampToInt(safeMultiply(goldBuyBaseNuggetValue, GoldUnitConverter.NUGGET_UNITS_PER_INGOT));
            goldBuyBlockValue = clampToInt(safeMultiply(goldBuyBaseNuggetValue, GoldUnitConverter.NUGGET_UNITS_PER_BLOCK));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack current = slot.getItem();
        ItemStack original = current.copy();

        if (slotIndex == CARD_SLOT_INDEX || slotIndex == INVOICE_SLOT_INDEX) {
            int targetStart = playerInventoryMode == PlayerInventoryMode.HOTBAR ? PLAYER_HOTBAR_START : PLAYER_INVENTORY_START;
            if (!moveItemStackTo(current, targetStart, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (cardItemDataService.isValidCardItem(current)) {
            if (!moveItemStackTo(current, CARD_SLOT_INDEX, CARD_SLOT_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (invoiceItemDataService.isInvoiceItem(current) && cardSlotMode == CardSlotMode.CREDIT) {
            if (!moveItemStackTo(current, INVOICE_SLOT_INDEX, INVOICE_SLOT_INDEX + 1, false)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, cardContainer);
        clearContainer(player, invoiceContainer);
    }

    private void addCardSlot() {
        addSlot(new Slot(cardContainer, 0, 290, 84) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mayPlaceCard(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return slotsVisible;
            }
        });
    }

    private void addInvoiceSlot() {
        addSlot(new Slot(invoiceContainer, 0, 244, 118) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return cardSlotMode == CardSlotMode.CREDIT && invoiceItemDataService.isInvoiceItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return slotsVisible && cardSlotMode == CardSlotMode.CREDIT;
            }
        });
    }

    private void addDataSlots() {
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
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return clampToInt(EconomyServerConfig.BANK_CARD_ISSUE_FEE.get());
            }

            @Override
            public void set(int value) {
                cardIssueFee = Math.max(0, value);
            }
        });
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 94 + column * 18, 166 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return slotsVisible && playerInventoryMode == PlayerInventoryMode.FULL;
                    }
                });
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 94 + column * 18, 224) {
                @Override
                public boolean isActive() {
                    return slotsVisible && playerInventoryMode != PlayerInventoryMode.NONE;
                }
            });
        }
    }

    private void syncSessionState(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BankSessionService.INSTANCE.findActiveSession(serverPlayer)
                .ifPresentOrElse(
                        session -> {
                            setSessionAccountNumber(session.accountNumber());
                            PacketDistributor.sendToPlayer(serverPlayer, new AtmSessionStatePayload(true, session.username(), session.accountNumber(), session.showUsername()));
                        },
                        () -> {
                            setSessionAccountNumber("");
                            PacketDistributor.sendToPlayer(serverPlayer, new AtmSessionStatePayload(false, "", "", false));
                        }
                );
    }

    private boolean mayPlaceCard(ItemStack stack) {
        var itemData = cardItemDataService.read(stack).orElse(null);
        if (itemData == null) {
            return false;
        }
        if (cardSlotMode == CardSlotMode.CREDIT) {
            return itemData.cardType().hasCredit();
        }
        if (cardSlotMode == CardSlotMode.SECURITY) {
            String accountNumber = cardItemDataService.accountNumber(stack).orElse("");
            if (!sessionAccountNumber.isBlank() && !accountNumber.equals(sessionAccountNumber)) {
                return false;
            }
            return securityCardAllowed(stack);
        }
        return true;
    }

    private boolean securityCardAllowed(ItemStack stack) {
        if (menuPlayer instanceof ServerPlayer serverPlayer) {
            BankSession session = BankSessionService.INSTANCE.findActiveSession(serverPlayer).orElse(null);
            if (session == null) {
                return false;
            }
            String accountNumber = cardItemDataService.accountNumber(stack).orElse("");
            if (!accountNumber.equals(session.accountNumber())) {
                return false;
            }
            try {
                return cardSecurityService.isBlockedOwnerCard(session.accountId(), stack);
            } catch (SQLException exception) {
                EconomiaMod.LOGGER.warn("Falha ao validar cartao bloqueado no ATM.", exception);
                return false;
            }
        }
        return true;
    }

    private boolean invoiceWindowOpen() {
        LocalDate today = LocalDate.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
        int dueDay = Math.min(today.lengthOfMonth(), EconomyServerConfig.CREDIT_INVOICE_DUE_DAY.get());
        int availableDay = Math.max(1, dueDay - EconomyServerConfig.CREDIT_INVOICE_AVAILABLE_DAYS_BEFORE.get());
        return today.getDayOfMonth() >= availableDay;
    }

    public enum CardSlotMode {
        LOGIN,
        CREDIT,
        SECURITY
    }

    public enum PlayerInventoryMode {
        NONE,
        HOTBAR,
        FULL
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

    @Override
    public boolean stillValid(Player player) {
        return CommercialMenuAccess.stillValid(player, accessPos, expectedBlock);
    }
}
