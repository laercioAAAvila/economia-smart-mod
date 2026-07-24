package br.com.economiamod.common.menu;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.money.MoneyStackCalculator;
import br.com.economiamod.common.pricing.ComparisonMode;
import br.com.economiamod.common.pricing.PricingMode;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.commercial.CommercialOwnerRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryRepository.SlotUpdate;
import br.com.economiamod.server.commercial.inventory.CommercialInventorySlot;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryType;
import br.com.economiamod.server.commercial.inventory.ItemStackSnapshotMapper;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.offer.BankOfferAdminService;
import br.com.economiamod.server.offer.BankOfferDraft;
import br.com.economiamod.server.offer.BankOfferReadRepository;
import br.com.economiamod.server.offer.BankOfferSnapshot;
import br.com.economiamod.server.offer.BankOfferWriteRepository;
import br.com.economiamod.server.player.ItemStackFactory;
import br.com.economiamod.server.transaction.AccountFinancialService;
import br.com.economiamod.server.transaction.CardPaymentService;
import br.com.economiamod.server.transaction.CreditPurchaseResultType;
import br.com.economiamod.server.transaction.DebitPurchaseResultType;
import br.com.economiamod.server.transaction.FinancialOperationResultType;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class PlayerShopMenu extends AbstractContainerMenu {
    public static final int REFERENCE_SLOT = 0;
    public static final int PAYMENT_SLOT = 1;
    public static final int CARD_SLOT = 2;
    public static final int SELL_INPUT_START = 3;
    public static final int SELL_INPUT_END = SELL_INPUT_START + 9;
    public static final int CASH_START = SELL_INPUT_END;
    public static final int CASH_END = CASH_START + 9;
    public static final int STOCK_START = CASH_END;
    public static final int STOCK_END = STOCK_START + 9;
    public static final int PLAYER_START = STOCK_END;
    public static final int PLAYER_END = PLAYER_START + 36;

    private static final int OFFER_SLOT = 0;

    private final SimpleContainer referenceContainer = new SimpleContainer(1);
    private final SimpleContainer paymentContainer = new SimpleContainer(1);
    private final SimpleContainer cardContainer = new SimpleContainer(1);
    private final SimpleContainer sellInputContainer = new SimpleContainer(9);
    private final SimpleContainer cashContainer = new SimpleContainer(9);
    private final SimpleContainer stockContainer = new SimpleContainer(9);
    private final CommercialInventoryRepository inventoryRepository = new CommercialInventoryRepository();
    private final CommercialOwnerRepository ownerRepository = new CommercialOwnerRepository();
    private final ItemStackSnapshotMapper snapshotMapper = new ItemStackSnapshotMapper();
    private final ItemStackFactory itemStackFactory = new ItemStackFactory();
    private final BankOfferReadRepository offerReadRepository = new BankOfferReadRepository();
    private final BankOfferWriteRepository offerWriteRepository = new BankOfferWriteRepository();
    private final BankOfferAdminService offerAdminService = new BankOfferAdminService();
    private final AccountQueryService accountQueryService = new AccountQueryService();
    private final CardItemDataService cardItemDataService = new CardItemDataService();
    private final CardValidationService cardValidationService = new CardValidationService(cardItemDataService);
    private final CardPaymentService cardPaymentService = new CardPaymentService(cardValidationService);
    private final AccountFinancialService accountFinancialService = new AccountFinancialService();
    private final BlockPos accessPos;
    private final Block expectedBlock;
    private final UUID commercialBlockId;
    private final UUID ownerPlayerUuid;
    private final ShopKind shopKind;
    private final boolean serverOwner;
    private final HolderLookup.Provider registries;
    private final List<CommercialInventorySlot> productSlots = new ArrayList<>();
    private final List<CommercialInventorySlot> cashSlots = new ArrayList<>();
    private int ownerFlag;
    private int activeFlag;
    private long price;
    private int quantity = 1;
    private int stockCount;
    private long purchasedQuantity;

    protected PlayerShopMenu(MenuType<?> menuType, int containerId, Inventory inventory, ShopKind shopKind) {
        this(menuType, containerId, inventory, shopKind, null, null, null, null, false);
    }

    protected PlayerShopMenu(MenuType<?> menuType, int containerId, Inventory inventory, ShopKind shopKind, UUID commercialBlockId, UUID ownerPlayerUuid, BlockPos accessPos, Block expectedBlock, boolean owner) {
        super(menuType, containerId);
        this.shopKind = shopKind;
        this.commercialBlockId = commercialBlockId;
        this.ownerPlayerUuid = ownerPlayerUuid;
        this.accessPos = accessPos;
        this.expectedBlock = expectedBlock;
        this.serverOwner = owner;
        this.ownerFlag = owner ? 1 : 0;
        this.registries = inventory.player.registryAccess();

        addDataSlots();
        loadPersistentState();
        addShopSlots();
        addPlayerInventorySlots(inventory);
    }

    protected PlayerShopMenu(MenuType<?> menuType, int containerId, Inventory inventory, ShopKind shopKind, FriendlyByteBuf data) {
        this(menuType, containerId, inventory, shopKind);
    }

    public ShopKind shopKind() {
        return shopKind;
    }

    public boolean ownerMode() {
        return ownerFlag == 1;
    }

    public boolean active() {
        return activeFlag == 1;
    }

    public long price() {
        return price;
    }

    public int quantity() {
        return quantity;
    }

    public boolean tradeReady() {
        return tradeReady(false);
    }

    public boolean tradeReady(boolean creditPayment) {
        if (!active() || ownerMode()) {
            return false;
        }
        int operationQuantity = operationQuantity();
        if (shopKind == ShopKind.SELL) {
            return paymentMethodReady(creditPayment) && maxSellShopOperations(operationQuantity, 1, creditPayment) > 0;
        }
        return maxBuyShopOperations(operationQuantity, 1) > 0
                && paymentMethodReady(false);
    }

    public int availableStock() {
        return stockCount;
    }

    public boolean visibleSlot(int menuSlot) {
        if (menuSlot == REFERENCE_SLOT) {
            return true;
        }
        if (menuSlot == PAYMENT_SLOT) {
            return false;
        }
        if (menuSlot == CARD_SLOT) {
            return !ownerMode();
        }
        if (menuSlot >= SELL_INPUT_START && menuSlot < SELL_INPUT_END) {
            return !ownerMode() && shopKind == ShopKind.BUY;
        }
        if (menuSlot >= CASH_START && menuSlot < CASH_END) {
            return ownerMode();
        }
        if (menuSlot >= STOCK_START && menuSlot < STOCK_END) {
            return ownerMode();
        }
        return true;
    }

    public void saveOwnerConfig(ServerPlayer player, long requestedPrice, int requestedQuantity, boolean active) {
        if (!serverOwner || commercialBlockId == null) {
            return;
        }
        saveInventories(player);
        if (referenceContainer.getItem(0).isEmpty()) {
            setOfferEnabled(false);
            return;
        }
        long safePrice = Math.max(0L, requestedPrice);
        int operationQuantity = operationQuantity();
        int safeLimit = Math.max(1, requestedQuantity);
        try {
            ItemStack reference = referenceContainer.getItem(0).copy();
            reference.setCount(operationQuantity);
            var item = snapshotMapper.fromStack(reference, player.registryAccess());
            Long targetQuantity = shopKind == ShopKind.BUY ? (long) safeLimit : null;
            boolean safeActive = active
                    && offerCanOperate(reference, operationQuantity)
                    && (targetQuantity == null || purchasedQuantity < targetQuantity);
            offerAdminService.saveOffer(new BankOfferDraft(
                    commercialBlockId,
                    OFFER_SLOT,
                    item.itemId(),
                    item.components(),
                    item.dataVersion(),
                    reference.getCount(),
                    shopKind == ShopKind.BUY ? safePrice : null,
                    shopKind == ShopKind.SELL ? safePrice : null,
                    null,
                    null,
                    targetQuantity,
                    ComparisonMode.FULL_COMPONENTS,
                    PricingMode.FIXED,
                    null,
                    null,
                    null,
                    0,
                    0,
                    0,
                    shopKind == ShopKind.BUY && safeActive,
                    shopKind == ShopKind.SELL && safeActive
            ));
            activeFlag = safeActive ? 1 : 0;
            price = safePrice;
            quantity = shopKind == ShopKind.BUY ? safeLimit : operationQuantity;
            broadcastChanges();
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao salvar configuracao da loja.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.shop.save_failed"));
        }
    }

    public void setBuyReferenceFromClient(ServerPlayer player, ItemStack stack) {
        if (commercialBlockId == null || active() || shopKind != ShopKind.BUY || stack.isEmpty() || !isCurrentOwner(player)) {
            return;
        }
        setReferenceFrom(stack);
        saveInventories(player);
        broadcastChanges();
    }

    public void trade(ServerPlayer player) {
        trade(player, UUID.randomUUID());
    }

    public void trade(ServerPlayer player, UUID requestId) {
        trade(player, requestId, 1);
    }

    public void trade(ServerPlayer player, UUID requestId, int requestedOperations) {
        trade(player, requestId, requestedOperations, false);
    }

    public void trade(ServerPlayer player, UUID requestId, int requestedOperations, boolean creditPayment) {
        if (serverOwner || commercialBlockId == null) {
            return;
        }
        if (!active()) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.inactive"));
            return;
        }
        if (shopKind == ShopKind.SELL) {
            buyFromShop(player, requestId, requestedOperations, creditPayment);
        } else {
            sellToShop(player, requestId, requestedOperations);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex == REFERENCE_SLOT) {
            return ItemStack.EMPTY;
        }
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
        } else if (paymentMethodItem(current) && visibleSlot(CARD_SLOT)) {
            if (!moveItemStackTo(current, CARD_SLOT, CARD_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ownerMode()) {
            if (!active() && referenceContainer.getItem(0).isEmpty()) {
                setReferenceFrom(current);
                broadcastChanges();
                return ItemStack.EMPTY;
            }
            if (!moveOwnerItem(current)) {
                return ItemStack.EMPTY;
            }
        } else if (isCustomerBuyInputVisible() && matchesReference(current)) {
            if (!moveItemStackTo(current, SELL_INPUT_START, SELL_INPUT_END, false)) {
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == REFERENCE_SLOT && serverOwner && !active()) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                referenceContainer.setItem(0, ItemStack.EMPTY);
                purchasedQuantity = 0L;
            } else {
                setReferenceFrom(carried);
            }
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (serverOwner && player instanceof ServerPlayer serverPlayer) {
            saveInventories(serverPlayer);
        }
        clearContainer(player, paymentContainer);
        clearContainer(player, cardContainer);
        clearContainer(player, sellInputContainer);
    }

    @Override
    public boolean stillValid(Player player) {
        return CommercialMenuAccess.stillValid(player, accessPos, expectedBlock);
    }

    private void buyFromShop(ServerPlayer player, UUID requestId, int requestedOperations, boolean creditPayment) {
        ItemStack product = referenceContainer.getItem(0);
        int operationQuantity = operationQuantity();
        int operations = maxSellShopOperations(operationQuantity, requestedOperations, creditPayment);
        if (product.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.no_offer"));
            return;
        }
        if (!hasStock(product, operationQuantity)) {
            setOfferEnabled(false);
            player.sendSystemMessage(Component.translatable("commands.economia.shop.no_stock"));
            return;
        }
        if (operations <= 0) {
            sendCustomerPaymentBlocked(player, creditPayment);
            return;
        }
        int totalQuantity = safeItemCount(operationQuantity, operations);
        long totalPrice = safePrice(operations);
        if (!takePayment(player, requestId, totalPrice, creditPayment)) {
            return;
        }
        ItemStack delivered = product.copy();
        delivered.setCount(totalQuantity);
        if (!player.getInventory().add(delivered)) {
            player.drop(delivered, false);
        }
        removeStock(product, totalQuantity);
        saveInventories(player);
        updateStockCount();
        if (!hasStock(product, operationQuantity)) {
            setOfferEnabled(false);
        }
        player.sendSystemMessage(Component.translatable("commands.economia.shop.buy_success"));
    }

    private boolean takePayment(ServerPlayer player, UUID requestId, long amount, boolean creditPayment) {
        if (amount <= 0L) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.trade_failed"));
            return false;
        }
        ItemStack payment = cardContainer.getItem(0);
        if (payment.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.no_card"));
            return false;
        }
        if (MoneyStackCalculator.isBanknote(payment)) {
            if (ShopMoneyContainerOps.moneyIn(cardContainer) < amount) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.insufficient_cash"));
                return false;
            }
            if (!ShopMoneyContainerOps.movePaymentToCashReserve(cardContainer, cashContainer)) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.stock_full"));
                return false;
            }
            return true;
        }
        if (!payOwnerWithCard(player, requestId, amount, creditPayment)) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.card_failed"));
            return false;
        }
        return true;
    }

    private void sellToShop(ServerPlayer player, UUID requestId, int requestedOperations) {
        ItemStack reference = referenceContainer.getItem(0);
        int operationQuantity = operationQuantity();
        int operations = maxBuyShopOperations(operationQuantity, requestedOperations);
        ItemStack offered = findSellInputStack(operationQuantity);
        if (reference.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.no_offer"));
            return;
        }
        if (offered == null || offered.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.no_item"));
            return;
        }
        if (offered.getCount() < operationQuantity || !ItemStack.isSameItemSameComponents(reference, offered)) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.wrong_item"));
            return;
        }
        if (!buyLimitAllows(operationQuantity)) {
            setOfferEnabled(false);
            player.sendSystemMessage(Component.translatable("commands.economia.shop.buy_limit_reached"));
            return;
        }
        if (!canInsertStock(reference, operationQuantity)) {
            setOfferEnabled(false);
            player.sendSystemMessage(Component.translatable("commands.economia.shop.stock_full"));
            return;
        }
        if (operations <= 0) {
            sendShopPayoutBlocked(player);
            return;
        }
        int totalQuantity = safeItemCount(operationQuantity, operations);
        ItemStack received = reference.copy();
        received.setCount(totalQuantity);
        if (!buyLimitAllows(totalQuantity)) {
            setOfferEnabled(false);
            player.sendSystemMessage(Component.translatable("commands.economia.shop.buy_limit_reached"));
            return;
        }
        if (!canInsertStock(received)) {
            setOfferEnabled(false);
            player.sendSystemMessage(Component.translatable("commands.economia.shop.stock_full"));
            return;
        }
        long totalPrice = safePrice(operations);
        if (!payCustomer(player, requestId, totalPrice)) {
            return;
        }
        removeSellInputItems(reference, totalQuantity);
        insertStock(received);
        purchasedQuantity += totalQuantity;
        recordPurchasedQuantity(totalQuantity);
        saveInventories(player);
        updateStockCount();
        if (!buyLimitAllows(operationQuantity) || !canInsertStock(received)) {
            setOfferEnabled(false);
        }
        player.sendSystemMessage(Component.translatable("commands.economia.shop.sell_success"));
    }

    private void loadPersistentState() {
        if (commercialBlockId == null) {
            return;
        }
        try {
            inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.PRODUCT_STOCK, 10);
            inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.CASH_RESERVE, 9);
            productSlots.clear();
            productSlots.addAll(inventoryRepository.loadSlots(commercialBlockId, CommercialInventoryType.PRODUCT_STOCK));
            cashSlots.clear();
            cashSlots.addAll(inventoryRepository.loadSlots(commercialBlockId, CommercialInventoryType.CASH_RESERVE));
            loadReferenceAndStock();
            loadContainer(cashContainer, cashSlots);
            updateStockCount();

            try (Connection connection = offerReadRepository.openConnection()) {
                BankOfferSnapshot offer = offerReadRepository.lockByBlockSlot(connection, commercialBlockId, OFFER_SLOT).orElse(null);
                if (offer != null) {
                    activeFlag = shopKind == ShopKind.SELL ? (offer.sellEnabled() ? 1 : 0) : (offer.buyEnabled() ? 1 : 0);
                    Long offerPrice = shopKind == ShopKind.SELL ? offer.baseSellPrice() : offer.baseBuyPrice();
                    price = offerPrice == null ? 0L : offerPrice;
                    purchasedQuantity = offer.purchasedQuantity();
                    quantity = shopKind == ShopKind.BUY && offer.targetQuantity() != null
                            ? Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(1L, offer.targetQuantity())))
                            : operationQuantity();
                    if (referenceContainer.getItem(0).isEmpty()) {
                        referenceContainer.setItem(0, itemStackFactory.create(
                                new br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot(
                                        offer.itemId(),
                                        offer.quantityPerOperation(),
                                        offer.itemComponents(),
                                        offer.itemDataVersion()
                                ),
                                offer.quantityPerOperation(),
                                registries
                        ));
                    }
                }
            }
            updateStockCount();
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao carregar loja.", exception);
        }
    }

    private void saveInventories(ServerPlayer player) {
        saveReferenceAndStock(player);
        saveContainer(player, cashContainer, cashSlots);
    }

    private void saveReferenceAndStock(ServerPlayer player) {
        List<PendingSlotUpdate> updates = new ArrayList<>();
        for (int index = 0; index < productSlots.size(); index++) {
            CommercialInventorySlot slot = productSlots.get(index);
            Container container = slot.slotIndex() == 0 ? referenceContainer : stockContainer;
            int containerSlot = slot.slotIndex() == 0 ? 0 : slot.slotIndex() - 1;
            ItemStack stack = container.getItem(containerSlot);
            if (slot.slotIndex() == 0 && !stack.isEmpty()) {
                stack = stack.copy();
                stack.setCount(1);
            }
            var snapshot = snapshotMapper.fromStack(stack, player.registryAccess());
            if (!snapshot.equals(slot.item())) {
                updates.add(new PendingSlotUpdate(index, slot, snapshot));
            }
        }
        saveSlotUpdates(productSlots, updates, "Falha ao salvar estoque de loja.");
    }

    private void saveContainer(ServerPlayer player, Container container, List<CommercialInventorySlot> slots) {
        List<PendingSlotUpdate> updates = new ArrayList<>();
        for (int index = 0; index < slots.size(); index++) {
            CommercialInventorySlot slot = slots.get(index);
            var snapshot = snapshotMapper.fromStack(container.getItem(slot.slotIndex()), player.registryAccess());
            if (!snapshot.equals(slot.item())) {
                updates.add(new PendingSlotUpdate(index, slot, snapshot));
            }
        }
        saveSlotUpdates(slots, updates, "Falha ao salvar slot de loja.");
    }

    private void saveSlotUpdates(List<CommercialInventorySlot> slots, List<PendingSlotUpdate> updates, String failureMessage) {
        if (updates.isEmpty()) {
            return;
        }
        try {
            List<SlotUpdate> batch = updates.stream()
                    .map(update -> new SlotUpdate(update.slot().id(), update.snapshot(), update.slot().version()))
                    .toList();
            boolean[] saved = inventoryRepository.updateSlots(batch);
            for (int index = 0; index < saved.length; index++) {
                if (saved[index]) {
                    PendingSlotUpdate update = updates.get(index);
                    slots.set(update.index(), nextVersion(update.slot(), update.snapshot()));
                }
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn(failureMessage, exception);
        }
    }

    private CommercialInventorySlot nextVersion(CommercialInventorySlot slot, br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot item) {
        return new CommercialInventorySlot(
                slot.id(),
                slot.commercialBlockId(),
                slot.inventoryType(),
                slot.slotIndex(),
                item,
                slot.version() + 1
        );
    }

    private record PendingSlotUpdate(int index, CommercialInventorySlot slot, br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot snapshot) {
    }

    private void loadContainer(Container container, List<CommercialInventorySlot> slots) {
        for (CommercialInventorySlot slot : slots) {
            if (slot.item().itemId() == null || slot.item().count() <= 0) {
                continue;
            }
            container.setItem(slot.slotIndex(), itemStackFactory.create(slot.item(), slot.item().count(), registries));
        }
    }

    private void loadReferenceAndStock() {
        for (CommercialInventorySlot slot : productSlots) {
            if (slot.item().itemId() == null || slot.item().count() <= 0) {
                continue;
            }
            ItemStack stack = itemStackFactory.create(slot.item(), slot.item().count(), registries);
            if (slot.slotIndex() == 0) {
                stack.setCount(1);
                referenceContainer.setItem(0, stack);
            } else if (slot.slotIndex() <= stockContainer.getContainerSize()) {
                stockContainer.setItem(slot.slotIndex() - 1, stack);
            }
        }
    }

    private void setOfferEnabled(boolean enabled) {
        try {
            offerWriteRepository.setEnabledByBlockSlot(
                    commercialBlockId,
                    OFFER_SLOT,
                    shopKind == ShopKind.BUY && enabled,
                    shopKind == ShopKind.SELL && enabled
            );
            activeFlag = enabled ? 1 : 0;
            broadcastChanges();
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao alterar estado da oferta.", exception);
        }
    }

    private void addDataSlots() {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ownerFlag;
            }

            @Override
            public void set(int value) {
                ownerFlag = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return activeFlag;
            }

            @Override
            public void set(int value) {
                activeFlag = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (int) price;
            }

            @Override
            public void set(int value) {
                price = (price & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (int) (price >>> 32);
            }

            @Override
            public void set(int value) {
                price = ((long) value << 32) | (price & 0xFFFFFFFFL);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return quantity;
            }

            @Override
            public void set(int value) {
                quantity = Math.max(1, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return stockCount;
            }

            @Override
            public void set(int value) {
                stockCount = Math.max(0, value);
            }
        });
    }

    private void addShopSlots() {
        addSlot(new Slot(referenceContainer, 0, 22, 56) {
            @Override
            public boolean isActive() {
                return visibleSlot(REFERENCE_SLOT);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(paymentContainer, 0, 174, 56) {
            @Override
            public boolean isActive() {
                return visibleSlot(PAYMENT_SLOT);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                if (serverOwner) {
                    return false;
                }
                return shopKind == ShopKind.BUY && matchesReference(stack);
            }
        });
        addSlot(new Slot(cardContainer, 0, 228, 56) {
            @Override
            public boolean isActive() {
                return visibleSlot(CARD_SLOT);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return !serverOwner && paymentMethodItem(stack);
            }
        });
        for (int slot = 0; slot < 9; slot++) {
            int menuSlot = SELL_INPUT_START + slot;
            addSlot(new Slot(sellInputContainer, slot, 58 + slot * 18, 56) {
                @Override
                public boolean isActive() {
                    return visibleSlot(menuSlot);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isCustomerBuyInputVisible() && matchesReference(stack);
                }
            });
        }
        for (int slot = 0; slot < 9; slot++) {
            int menuSlot = CASH_START + slot;
            addSlot(new Slot(cashContainer, slot, 22 + slot * 18, 98) {
                @Override
                public boolean isActive() {
                    return visibleSlot(menuSlot);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return serverOwner && MoneyStackCalculator.isBanknote(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return serverOwner;
                }
            });
        }
        for (int slot = 0; slot < 9; slot++) {
            int menuSlot = STOCK_START + slot;
            addSlot(new Slot(stockContainer, slot, 22 + slot * 18, 124) {
                @Override
                public boolean isActive() {
                    return visibleSlot(menuSlot);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return serverOwner && matchesReference(stack) && (shopKind == ShopKind.BUY || !active());
                }

                @Override
                public boolean mayPickup(Player player) {
                    return serverOwner && (shopKind == ShopKind.BUY || !active());
                }
            });
        }
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 22 + column * 18, 158 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 22 + column * 18, 216));
        }
    }

    private boolean moveOwnerItem(ItemStack stack) {
        if (MoneyStackCalculator.isBanknote(stack)) {
            return moveItemStackTo(stack, CASH_START, CASH_END, false);
        }
        if (active() && shopKind == ShopKind.SELL) {
            return false;
        }
        if (referenceContainer.getItem(0).isEmpty()) {
            setReferenceFrom(stack);
            broadcastChanges();
            return false;
        }
        if (matchesReference(stack)) {
            return moveItemStackTo(stack, STOCK_START, STOCK_END, false);
        }
        return false;
    }

    private boolean isCustomerBuyInputVisible() {
        return !ownerMode() && shopKind == ShopKind.BUY;
    }

    private boolean isCurrentOwner(ServerPlayer player) {
        if (serverOwner) {
            return true;
        }
        try {
            return ownerRepository.owner(commercialBlockId)
                    .map(player.getUUID()::equals)
                    .orElse(false);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao validar dono da loja.", exception);
            return false;
        }
    }

    private void setReferenceFrom(ItemStack stack) {
        ItemStack reference = stack.copy();
        reference.setCount(1);
        ItemStack current = referenceContainer.getItem(0);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, reference)) {
            purchasedQuantity = 0L;
        }
        referenceContainer.setItem(0, reference);
    }

    private boolean matchesReference(ItemStack stack) {
        ItemStack reference = referenceContainer.getItem(0);
        return !stack.isEmpty() && !reference.isEmpty() && ItemStack.isSameItemSameComponents(reference, stack);
    }

    private boolean hasStock(ItemStack reference, int count) {
        int total = 0;
        for (int slot = 0; slot < stockContainer.getContainerSize(); slot++) {
            ItemStack stack = stockContainer.getItem(slot);
            if (ItemStack.isSameItemSameComponents(reference, stack)) {
                total += stack.getCount();
                if (total >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countStock(ItemStack reference) {
        if (reference.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < stockContainer.getContainerSize(); slot++) {
            ItemStack stack = stockContainer.getItem(slot);
            if (ItemStack.isSameItemSameComponents(reference, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void updateStockCount() {
        stockCount = countStock(referenceContainer.getItem(0));
        broadcastChanges();
    }

    private boolean hasSellInputItem(int count) {
        return findSellInputStack(count) != null;
    }

    private int countSellInputItems() {
        ItemStack reference = referenceContainer.getItem(0);
        if (reference.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < sellInputContainer.getContainerSize(); slot++) {
            ItemStack stack = sellInputContainer.getItem(slot);
            if (ItemStack.isSameItemSameComponents(reference, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private ItemStack findSellInputStack(int count) {
        ItemStack reference = referenceContainer.getItem(0);
        if (reference.isEmpty() || count <= 0) {
            return null;
        }
        for (int slot = 0; slot < sellInputContainer.getContainerSize(); slot++) {
            ItemStack stack = sellInputContainer.getItem(slot);
            if (stack.getCount() >= count && ItemStack.isSameItemSameComponents(reference, stack)) {
                return stack;
            }
        }
        return null;
    }

    private void removeSellInputItems(ItemStack reference, int count) {
        int remaining = count;
        for (int slot = 0; slot < sellInputContainer.getContainerSize(); slot++) {
            ItemStack stack = sellInputContainer.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(reference, stack)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) {
                sellInputContainer.setItem(slot, ItemStack.EMPTY);
            } else {
                sellInputContainer.setChanged();
            }
            remaining -= removed;
            if (remaining == 0) {
                return;
            }
        }
    }

    private void removeStock(ItemStack reference, int count) {
        int remaining = count;
        for (int slot = 0; slot < stockContainer.getContainerSize(); slot++) {
            ItemStack stack = stockContainer.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(reference, stack)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) {
                stockContainer.setItem(slot, ItemStack.EMPTY);
            } else {
                stockContainer.setChanged();
            }
            remaining -= removed;
            if (remaining == 0) {
                return;
            }
        }
    }

    private void insertStock(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < stockContainer.getContainerSize(); slot++) {
            ItemStack target = stockContainer.getItem(slot);
            if (target.isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack inserted = remaining.copy();
                inserted.setCount(moved);
                stockContainer.setItem(slot, inserted);
                remaining.shrink(moved);
                if (remaining.isEmpty()) {
                    return;
                }
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(target, remaining)) {
                continue;
            }
            int moved = Math.min(remaining.getCount(), target.getMaxStackSize() - target.getCount());
            if (moved <= 0) {
                continue;
            }
            target.grow(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
                return;
            }
        }
    }

    private boolean canInsertStock(ItemStack stack) {
        int remaining = stack.getCount();
        for (int slot = 0; slot < stockContainer.getContainerSize(); slot++) {
            ItemStack target = stockContainer.getItem(slot);
            if (target.isEmpty()) {
                remaining -= Math.min(remaining, stack.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(target, stack)) {
                remaining -= Math.max(0, target.getMaxStackSize() - target.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private int stockInsertCapacity(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < stockContainer.getContainerSize(); slot++) {
            ItemStack target = stockContainer.getItem(slot);
            if (target.isEmpty()) {
                total += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(target, stack)) {
                total += Math.max(0, target.getMaxStackSize() - target.getCount());
            }
        }
        return total;
    }

    private boolean canInsertStock(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return false;
        }
        ItemStack operationStack = stack.copy();
        operationStack.setCount(count);
        return canInsertStock(operationStack);
    }

    private boolean offerCanOperate(ItemStack reference, int count) {
        if (reference.isEmpty() || count <= 0) {
            return false;
        }
        if (shopKind == ShopKind.SELL) {
            return hasStock(reference, count);
        }
        return canInsertStock(reference, count);
    }

    private int operationQuantity() {
        return 1;
    }

    private int maxRequestedOperations(int requestedOperations) {
        return requestedOperations <= 0 ? Integer.MAX_VALUE : requestedOperations;
    }

    private int maxBuyShopOperations(int operationQuantity, int requestedOperations) {
        ItemStack reference = referenceContainer.getItem(0);
        if (reference.isEmpty() || operationQuantity <= 0 || price <= 0L) {
            return 0;
        }
        int max = maxRequestedOperations(requestedOperations);
        max = Math.min(max, countSellInputItems() / operationQuantity);
        max = Math.min(max, stockInsertCapacity(reference) / operationQuantity);
        long remainingLimit = (long) quantity - purchasedQuantity;
        if (remainingLimit <= 0L) {
            return 0;
        }
        max = Math.min(max, Math.toIntExact(Math.min(Integer.MAX_VALUE, remainingLimit / operationQuantity)));
        max = Math.min(max, affordableOperationsForShopPayout());
        return Math.max(0, max);
    }

    private int maxSellShopOperations(int operationQuantity, int requestedOperations, boolean creditPayment) {
        ItemStack reference = referenceContainer.getItem(0);
        if (reference.isEmpty() || operationQuantity <= 0 || price <= 0L) {
            return 0;
        }
        int max = maxRequestedOperations(requestedOperations);
        max = Math.min(max, countStock(reference) / operationQuantity);
        max = Math.min(max, affordableOperationsForCustomerPayment(creditPayment));
        return Math.max(0, max);
    }

    private int affordableOperationsForCustomerPayment(boolean creditPayment) {
        ItemStack payment = cardContainer.getItem(0);
        if (payment.isEmpty() || price <= 0L) {
            return 0;
        }
        if (MoneyStackCalculator.isBanknote(payment)) {
            return Math.toIntExact(Math.min(Integer.MAX_VALUE, ShopMoneyContainerOps.moneyIn(cardContainer) / price));
        }
        try {
            CardValidationResult card = cardValidationService.validate(payment);
            if (card.type() != CardValidationResultType.VALID) {
                return 0;
            }
            if (!creditPayment && card.cardType().hasDebit()) {
                var summary = accountQueryService.findBalanceSummary(card.accountId()).orElse(null);
                if (summary == null) {
                    return 0;
                }
                long available = Math.min(summary.availableBalance(), debitDailyRemaining(card.cardId()));
                return Math.toIntExact(Math.min(Integer.MAX_VALUE, available / price));
            }
            if (creditPayment && card.cardType().hasCredit()) {
                var summary = accountQueryService.findBalanceSummary(card.accountId()).orElse(null);
                if (summary == null) {
                    return 0;
                }
                long cardDebt = br.com.economiamod.common.credit.CreditMath.debtTotal(card.creditPrincipalOutstanding(), card.creditInterestOutstanding());
                long accountAvailable = summary.globalCreditAvailable();
                long cardAvailable = Math.max(0L, card.individualCreditLimit() - cardDebt);
                long available = Math.min(accountAvailable, cardAvailable);
                return Math.toIntExact(Math.min(Integer.MAX_VALUE, available / price));
            }
            return 0;
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao calcular pagamento disponivel da loja.", exception);
            return 0;
        }
    }

    private void sendCustomerPaymentBlocked(ServerPlayer player, boolean creditPayment) {
        ItemStack payment = cardContainer.getItem(0);
        if (payment.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.no_card"));
            return;
        }
        if (MoneyStackCalculator.isBanknote(payment)) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.insufficient_cash"));
            return;
        }
        try {
            CardValidationResult card = cardValidationService.validate(payment);
            if (card.type() != CardValidationResultType.VALID) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.no_card"));
                return;
            }
            if (creditPayment && !card.cardType().hasCredit()) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.payment_credit_required"));
                return;
            }
            if (!creditPayment && !card.cardType().hasDebit()) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.payment_debit_required"));
                return;
            }
            if (creditPayment) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.insufficient_credit"));
                return;
            }
            long remainingDebit = debitDailyRemaining(card.cardId());
            player.sendSystemMessage(Component.translatable(remainingDebit < price
                    ? "commands.economia.shop.daily_limit_reached"
                    : "commands.economia.shop.insufficient_balance"));
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao explicar pagamento recusado da loja.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.shop.card_failed"));
        }
    }

    private void sendShopPayoutBlocked(ServerPlayer player) {
        if (price <= 0L) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.trade_failed"));
            return;
        }
        ItemStack target = cardContainer.getItem(0);
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.receive_method_required"));
            return;
        }
        if (MoneyStackCalculator.isBanknote(target)) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.cash_reserve_insufficient"));
            return;
        }
        try {
            CardValidationResult card = cardValidationService.validate(target);
            if (card.type() != CardValidationResultType.VALID) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.receive_card_invalid"));
                return;
            }
            if (ownerPlayerUuid == null) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.owner_account_missing"));
                return;
            }
            UUID ownerAccountId = accountQueryService.findActiveAccountIdByPlayer(ownerPlayerUuid).orElse(null);
            if (ownerAccountId == null) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.owner_account_missing"));
                return;
            }
            var summary = accountQueryService.findBalanceSummary(ownerAccountId).orElse(null);
            if (summary == null) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.owner_account_missing"));
                return;
            }
            player.sendSystemMessage(Component.translatable(summary.availableBalance() < price
                    ? "commands.economia.shop.shop_cannot_pay"
                    : "commands.economia.shop.trade_failed"));
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao explicar pagamento da loja de compra.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.shop.card_failed"));
        }
    }

    private int affordableOperationsForShopPayout() {
        ItemStack payment = cardContainer.getItem(0);
        if (payment.isEmpty() || price <= 0L) {
            return 0;
        }
        if (MoneyStackCalculator.isBanknote(payment)) {
            return Math.toIntExact(Math.min(Integer.MAX_VALUE, ShopMoneyContainerOps.moneyIn(cashContainer) / price));
        }
        try {
            CardValidationResult card = cardValidationService.validate(payment);
            if (card.type() != CardValidationResultType.VALID || ownerPlayerUuid == null) {
                return 0;
            }
            UUID ownerAccountId = accountQueryService.findActiveAccountIdByPlayer(ownerPlayerUuid).orElse(null);
            if (ownerAccountId == null) {
                return 0;
            }
            var summary = accountQueryService.findBalanceSummary(ownerAccountId).orElse(null);
            return summary == null ? 0 : Math.toIntExact(Math.min(Integer.MAX_VALUE, summary.availableBalance() / price));
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao calcular pagamento disponivel da loja.", exception);
            return 0;
        }
    }

    private long debitDailyRemaining(UUID cardId) throws SQLException {
        String sql = """
                SELECT debit_daily_limit,
                       debit_daily_spent,
                       debit_daily_spent_on
                  FROM economy_cards
                 WHERE id = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return 0L;
                }
                long limit = resultSet.getLong("debit_daily_limit");
                if (limit <= 0L) {
                    return Long.MAX_VALUE;
                }
                LocalDate today = LocalDate.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
                LocalDate spentOn = resultSet.getObject("debit_daily_spent_on", LocalDate.class);
                long spent = today.equals(spentOn) ? resultSet.getLong("debit_daily_spent") : 0L;
                return Math.max(0L, limit - spent);
            }
        }
    }

    private int safeItemCount(int operationQuantity, int operations) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, (long) operationQuantity * Math.max(0, operations)));
    }

    private long safePrice(int operations) {
        return Math.multiplyExact((long) price, Math.max(0L, operations));
    }

    private boolean buyLimitAllows(int count) {
        return shopKind != ShopKind.BUY || (long) quantity - purchasedQuantity >= count;
    }

    private void recordPurchasedQuantity(long count) {
        try {
            offerWriteRepository.incrementPurchasedByBlockSlot(commercialBlockId, OFFER_SLOT, count);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao atualizar limite comprado da loja.", exception);
        }
    }

    private boolean paymentMethodItem(ItemStack stack) {
        return cardItemDataService.isValidCardItem(stack) || MoneyStackCalculator.isBanknote(stack);
    }

    private boolean paymentMethodReady(boolean creditPayment) {
        if (price <= 0L) {
            return false;
        }
        ItemStack payment = cardContainer.getItem(0);
        if (payment.isEmpty()) {
            return false;
        }
        if (MoneyStackCalculator.isBanknote(payment)) {
            if (shopKind == ShopKind.BUY) {
                return ShopMoneyContainerOps.moneyIn(cashContainer) >= price;
            }
            return ShopMoneyContainerOps.moneyIn(cardContainer) >= price;
        }
        CardValidationResult card;
        try {
            card = cardValidationService.validate(payment);
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao validar forma de pagamento da loja.", exception);
            return false;
        }
        if (card.type() != CardValidationResultType.VALID) {
            return false;
        }
        if (shopKind != ShopKind.SELL) {
            return true;
        }
        return creditPayment ? card.cardType().hasCredit() : card.cardType().hasDebit();
    }

    private boolean payCustomer(ServerPlayer player, UUID requestId, long amount) {
        if (amount <= 0L) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.trade_failed"));
            return false;
        }
        ItemStack target = cardContainer.getItem(0);
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.no_card"));
            return false;
        }
        if (MoneyStackCalculator.isBanknote(target)) {
            if (!ShopMoneyContainerOps.removeCashReserve(cashContainer, amount)) {
                player.sendSystemMessage(Component.translatable("commands.economia.shop.cash_reserve_insufficient"));
                return false;
            }
            ShopMoneyContainerOps.givePayment(player, amount);
            return true;
        }
        if (!payCustomerCard(player, requestId, amount)) {
            player.sendSystemMessage(Component.translatable("commands.economia.shop.card_failed"));
            return false;
        }
        return true;
    }

    private boolean payOwnerWithCard(ServerPlayer player, UUID requestId, long amount, boolean creditPayment) {
        if (ownerPlayerUuid == null || amount <= 0L) {
            return false;
        }
        try {
            UUID ownerAccountId = accountQueryService.findActiveAccountIdByPlayer(ownerPlayerUuid).orElse(null);
            if (ownerAccountId == null) {
                return false;
            }
            CardValidationResult card = cardValidationService.validate(cardContainer.getItem(0));
            if (card.type() != CardValidationResultType.VALID) {
                return false;
            }
            String key = "shop-card:" + commercialBlockId + ":" + player.getUUID() + ":" + stableRequestId(requestId);
            if (!creditPayment && card.cardType().hasDebit()) {
                var debit = cardPaymentService.debitPurchase(cardContainer.getItem(0), ownerAccountId, amount, player.getUUID(), key);
                return debit.type() == DebitPurchaseResultType.COMPLETED || debit.type() == DebitPurchaseResultType.DUPLICATE_COMPLETED;
            }
            if (creditPayment && card.cardType().hasCredit()) {
                var credit = cardPaymentService.creditPurchase(cardContainer.getItem(0), ownerAccountId, amount, player.getUUID(), "Loja", key);
                return credit.type() == CreditPurchaseResultType.COMPLETED || credit.type() == CreditPurchaseResultType.DUPLICATE_COMPLETED;
            }
            return false;
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao pagar loja com cartao.", exception);
            return false;
        }
    }

    private boolean payCustomerCard(ServerPlayer player, UUID requestId, long amount) {
        if (ownerPlayerUuid == null || amount <= 0L) {
            return false;
        }
        try {
            UUID ownerAccountId = accountQueryService.findActiveAccountIdByPlayer(ownerPlayerUuid).orElse(null);
            if (ownerAccountId == null) {
                return false;
            }
            CardValidationResult card = cardValidationService.validate(cardContainer.getItem(0));
            if (card.type() != CardValidationResultType.VALID) {
                return false;
            }
            String key = "shop-card-payout:" + commercialBlockId + ":" + player.getUUID() + ":" + stableRequestId(requestId);
            var result = accountFinancialService.transfer(player.getUUID(), ownerAccountId, card.accountId(), amount, card.cardId(), key);
            return result.type() == FinancialOperationResultType.COMPLETED || result.type() == FinancialOperationResultType.DUPLICATE_COMPLETED;
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao pagar cliente com cartao.", exception);
            return false;
        }
    }

    private String stableRequestId(UUID requestId) {
        return (requestId == null ? UUID.randomUUID() : requestId).toString();
    }
}
