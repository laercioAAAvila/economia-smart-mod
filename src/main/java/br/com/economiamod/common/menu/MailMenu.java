package br.com.economiamod.common.menu;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.mail.MailPricing;
import br.com.economiamod.common.money.MoneyStackCalculator;
import br.com.economiamod.common.network.MailStatePayload;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.mail.MailBlockRecord;
import br.com.economiamod.server.mail.MailBlockRepository;
import br.com.economiamod.server.mail.MailInventoryService;
import br.com.economiamod.server.mail.MailRecipientRecord;
import br.com.economiamod.server.mail.MailRecipientRepository;
import br.com.economiamod.server.operation.EconomyOperationService;
import br.com.economiamod.server.operation.EconomyOperationState;
import br.com.economiamod.server.operation.EconomyOperationType;
import br.com.economiamod.server.operation.OperationStartResult;
import br.com.economiamod.server.operation.OperationStartType;
import br.com.economiamod.server.transaction.AccountFinancialService;
import br.com.economiamod.server.transaction.CardPaymentService;
import br.com.economiamod.server.transaction.CreditPurchaseResultType;
import br.com.economiamod.server.transaction.DebitPurchaseResultType;
import br.com.economiamod.server.transaction.FinancialOperationResultType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MailMenu extends AbstractContainerMenu {
    public static final int SEND_START = 0;
    public static final int SEND_END = SEND_START + 18;
    public static final int RECEIVE_START = SEND_END;
    public static final int RECEIVE_END = RECEIVE_START + 18;
    public static final int CARD_SLOT = RECEIVE_END;
    public static final int CASH_START = CARD_SLOT + 1;
    public static final int CASH_END = CASH_START + 6;
    public static final int PLAYER_START = CASH_END;
    public static final int PLAYER_END = PLAYER_START + 36;

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final SimpleContainer sendContainer = new SimpleContainer(18);
    private final SimpleContainer receiveContainer;
    private final SimpleContainer cardContainer = new SimpleContainer(1);
    private final SimpleContainer cashContainer = new SimpleContainer(6);
    private final MailBlockRepository blockRepository = new MailBlockRepository();
    private final MailRecipientRepository recipientRepository = new MailRecipientRepository();
    private final MailInventoryService inventoryService = new MailInventoryService();
    private final CardItemDataService cardItemDataService = new CardItemDataService();
    private final CardValidationService cardValidationService = new CardValidationService(cardItemDataService);
    private final CardPaymentService cardPaymentService = new CardPaymentService(cardValidationService);
    private final AccountFinancialService accountFinancialService = new AccountFinancialService();
    private final EconomyOperationService operationService = new EconomyOperationService();
    private final HolderLookup.Provider registries;
    private final BlockPos accessPos;
    private final Block expectedBlock;
    private final UUID mailBlockId;
    private final UUID ownerPlayerUuid;
    private final UUID ownerAccountId;
    private final String dimension;
    private final int originX;
    private final int originZ;
    private final boolean serverOwner;
    private final boolean serverSide;
    private final List<MailStatePayload.RecipientSummary> recipients = new ArrayList<>();
    private String mailName;
    private UUID selectedRecipientId = EMPTY_UUID;
    private int ownerFlag;
    private int namedFlag;
    private int paymentFlag;
    private int changeWarningFlag;
    private boolean paymentOpen;
    private boolean clientShowSendSlots = true;
    private boolean clientShowReceiveSlots = true;
    private boolean clientShowPaymentSlots = true;
    private boolean clientShowPlayerSlots = true;

    public MailMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null, null, "", "", 0, 0, 0, null, ModBlocks.MAIL.get(), false, false);
    }

    public MailMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(
                containerId,
                inventory,
                data.readUUID(),
                readNullableUuid(data),
                readNullableUuid(data),
                data.readUtf(255),
                data.readUtf(64),
                data.readInt(),
                data.readInt(),
                data.readInt(),
                data.readBlockPos(),
                ModBlocks.MAIL.get(),
                data.readBoolean(),
                data.readBoolean()
        );
        selectedRecipientId = data.readUUID();
    }

    public MailMenu(
            int containerId,
            Inventory inventory,
            UUID mailBlockId,
            UUID ownerPlayerUuid,
            UUID ownerAccountId,
            String dimension,
            String mailName,
            int originX,
            int originY,
            int originZ,
            BlockPos accessPos,
            Block expectedBlock,
            boolean owner,
            boolean named
    ) {
        super(ModMenus.MAIL.get(), containerId);
        this.registries = inventory.player.registryAccess();
        this.mailBlockId = mailBlockId;
        this.ownerPlayerUuid = ownerPlayerUuid;
        this.ownerAccountId = ownerAccountId;
        this.dimension = dimension == null ? "" : dimension;
        this.mailName = mailName == null ? "" : mailName;
        this.originX = originX;
        this.originZ = originZ;
        this.accessPos = accessPos;
        this.expectedBlock = expectedBlock;
        this.serverOwner = owner;
        this.serverSide = mailBlockId != null;
        this.ownerFlag = owner ? 1 : 0;
        this.namedFlag = named ? 1 : 0;
        this.receiveContainer = loadReceiveContainer();

        addDataSlots();
        addMailSlots();
        addPlayerInventorySlots(inventory);
        loadRecipients();
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, MailBlockRecord record, BlockPos pos, boolean owner) {
        buffer.writeUUID(record.id());
        writeNullableUuid(buffer, record.ownerPlayerUuid());
        writeNullableUuid(buffer, record.ownerAccountId());
        buffer.writeUtf(record.dimension(), 255);
        buffer.writeUtf(record.name() == null ? "" : record.name(), 64);
        buffer.writeInt(record.x());
        buffer.writeInt(record.y());
        buffer.writeInt(record.z());
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(owner);
        buffer.writeBoolean(record.named());
        buffer.writeUUID(EMPTY_UUID);
    }

    public boolean ownerMode() {
        return ownerFlag == 1;
    }

    public boolean named() {
        return namedFlag == 1;
    }

    public boolean paymentComplete() {
        return paymentFlag == 1;
    }

    public boolean changeWarning() {
        return changeWarningFlag == 1;
    }

    public String mailName() {
        return mailName;
    }

    public List<MailStatePayload.RecipientSummary> recipients() {
        return List.copyOf(recipients);
    }

    public UUID selectedRecipientId() {
        return selectedRecipientId;
    }

    public MailStatePayload.RecipientSummary selectedRecipient() {
        return recipients.stream()
                .filter(recipient -> recipient.destinationBlockId().equals(selectedRecipientId))
                .findFirst()
                .orElse(null);
    }

    public int occupiedSendSlots() {
        int count = 0;
        for (int slot = 0; slot < sendContainer.getContainerSize(); slot++) {
            if (!sendContainer.getItem(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public long shipmentDistance() {
        MailStatePayload.RecipientSummary selected = selectedRecipient();
        if (selected == null) {
            return 0L;
        }
        return MailPricing.horizontalDistance(originX, originZ, selected.x(), selected.z());
    }

    public long shipmentTotal() {
        return MailPricing.total(EconomyServerConfig.MAIL_PRICE_PER_OCCUPIED_SLOT.get(), occupiedSendSlots(), shipmentDistance());
    }

    public boolean visibleSlot(int menuSlot) {
        if (menuSlot >= SEND_START && menuSlot < SEND_END) {
            return selectedRecipient() != null;
        }
        if (menuSlot >= RECEIVE_START && menuSlot < RECEIVE_END) {
            return named();
        }
        if (menuSlot == CARD_SLOT || (menuSlot >= CASH_START && menuSlot < CASH_END)) {
            return selectedRecipient() != null && paymentOpen;
        }
        return true;
    }

    public boolean isPaymentSlot(int menuSlot) {
        return menuSlot == CARD_SLOT || (menuSlot >= CASH_START && menuSlot < CASH_END);
    }

    public boolean isSendSlot(int menuSlot) {
        return menuSlot >= SEND_START && menuSlot < SEND_END;
    }

    public boolean isReceiveSlot(int menuSlot) {
        return menuSlot >= RECEIVE_START && menuSlot < RECEIVE_END;
    }

    public void setClientSlotVisibility(boolean sendSlots, boolean receiveSlots, boolean paymentSlots, boolean playerSlots) {
        clientShowSendSlots = sendSlots;
        clientShowReceiveSlots = receiveSlots;
        clientShowPaymentSlots = paymentSlots;
        clientShowPlayerSlots = playerSlots;
    }

    public boolean clientSlotActive(int menuSlot) {
        if (isSendSlot(menuSlot)) {
            return clientShowSendSlots;
        }
        if (isReceiveSlot(menuSlot)) {
            return clientShowReceiveSlots;
        }
        if (isPaymentSlot(menuSlot)) {
            return clientShowPaymentSlots;
        }
        if (menuSlot >= PLAYER_START && menuSlot < PLAYER_END) {
            return clientShowPlayerSlots;
        }
        return visibleSlot(menuSlot);
    }

    public void applyState(MailStatePayload payload) {
        namedFlag = payload.named() ? 1 : 0;
        mailName = payload.mailName();
        selectedRecipientId = payload.selectedRecipientId();
        paymentFlag = payload.paymentComplete() ? 1 : 0;
        changeWarningFlag = payload.changeWarning() ? 1 : 0;
        recipients.clear();
        recipients.addAll(payload.recipients());
        broadcastChanges();
    }

    public void setInitialName(ServerPlayer player, String name) {
        if (!serverOwner || mailBlockId == null || name == null || name.isBlank()) {
            return;
        }
        try {
            blockRepository.rename(mailBlockId, name);
            mailName = name.trim();
            namedFlag = 1;
            syncState(player);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao nomear Correio.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.save_failed"));
        }
    }

    public void addRecipient(ServerPlayer player, String name) {
        if (mailBlockId == null || name == null || name.isBlank()) {
            return;
        }
        try {
            List<MailBlockRecord> matches = blockRepository.findByNameInDimension(name, dimension).stream()
                    .filter(record -> !record.id().equals(mailBlockId))
                    .toList();
            if (matches.isEmpty()) {
                player.sendSystemMessage(Component.translatable("commands.economia.mail.not_found"));
                return;
            }
            for (MailBlockRecord match : matches) {
                recipientRepository.add(mailBlockId, match.id(), player.getUUID());
            }
            loadRecipients();
            syncState(player);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao adicionar destinatario do Correio.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.save_failed"));
        }
    }

    public void deleteRecipient(ServerPlayer player, UUID destinationBlockId) {
        if (!serverOwner || mailBlockId == null || destinationBlockId == null) {
            return;
        }
        try {
            recipientRepository.delete(mailBlockId, destinationBlockId);
            if (destinationBlockId.equals(selectedRecipientId)) {
                selectedRecipientId = EMPTY_UUID;
                paymentFlag = 0;
            }
            loadRecipients();
            syncState(player);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao remover destinatario do Correio.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.save_failed"));
        }
    }

    public void selectRecipient(UUID destinationBlockId) {
        if (destinationBlockId == null) {
            selectedRecipientId = EMPTY_UUID;
            return;
        }
        selectedRecipientId = recipients.stream()
                .anyMatch(recipient -> recipient.destinationBlockId().equals(destinationBlockId))
                ? destinationBlockId
                : EMPTY_UUID;
        paymentFlag = 0;
        changeWarningFlag = 0;
        paymentOpen = false;
    }

    public void openPayment() {
        if (selectedRecipient() != null) {
            paymentOpen = true;
        }
    }

    public void closePayment() {
        paymentOpen = false;
    }

    public void returnToPayment(ServerPlayer player) {
        if (selectedRecipient() == null) {
            return;
        }
        changeWarningFlag = 0;
        paymentOpen = true;
        syncState(player);
    }

    public void payCash(ServerPlayer player, UUID requestId, boolean confirmOwnerChange) {
        if (!paymentOpen && !confirmOwnerChange) {
            return;
        }
        if (!shipmentReadyForPayment(player)) {
            return;
        }
        long total = shipmentTotal();
        long cash = moneyIn(cashContainer);
        if (total <= 0L || cash < total) {
            player.sendSystemMessage(Component.translatable("commands.economia.mail.insufficient_cash"));
            return;
        }
        long change = cash - total;
        if (change > 0L && cardContainer.getItem(0).isEmpty() && !confirmOwnerChange) {
            changeWarningFlag = 1;
            paymentOpen = false;
            syncState(player);
            return;
        }

        String requestToken = stableRequestId(requestId);
        String operationKey = "mail-payment:" + mailBlockId + ":" + player.getUUID() + ":" + requestToken;
        try {
            OperationStartResult start = operationService.begin(operationKey, EconomyOperationType.MAIL_PAYMENT,
                    player.getUUID(), shipmentOperationPayload(total, "cash", change));
            if (start.type() == OperationStartType.DUPLICATE_COMPLETED) {
                player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_success"));
                return;
            }
            if (start.type() != OperationStartType.CREATED) {
                player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
                return;
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao registrar pagamento do Correio.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
            return;
        }

        try {
            if (!operationService.mark(operationKey, EconomyOperationState.ITEMS_RESERVED)) {
                operationService.markReconciliationRequired(operationKey, "unable to reserve mail cash payment");
                player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
                return;
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao reservar pagamento em dinheiro do Correio.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
            return;
        }

        if (change > 0L && !creditChange(player, requestId, change, confirmOwnerChange)) {
            try {
                operationService.mark(operationKey, EconomyOperationState.ROLLED_BACK);
            } catch (SQLException ignored) {
            }
            player.sendSystemMessage(Component.translatable("commands.economia.mail.change_failed"));
            return;
        }
        try {
            if (!operationService.mark(operationKey, EconomyOperationState.SQL_COMMITTED)) {
                operationService.markReconciliationRequired(operationKey, change > 0L
                        ? "mail change credited but state transition failed"
                        : "mail cash payment could not enter committed state");
                player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
                return;
            }
            cashContainer.clearContent();
            returnCard(player);
            changeWarningFlag = 0;
            paymentOpen = false;
            if (!finishPaidShipment(player)) {
                operationService.markReconciliationRequired(operationKey, "cash payment consumed but shipment was not persisted");
                syncState(player);
                return;
            }
            operationService.mark(operationKey, EconomyOperationState.ITEMS_DELIVERED);
            operationService.mark(operationKey, EconomyOperationState.COMPLETED);
            syncState(player);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_success"));
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.error("Pagamento em dinheiro do Correio exige reconciliacao.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
        }
    }

    public void payCard(ServerPlayer player, UUID requestId, boolean creditPayment) {
        if (!paymentOpen) {
            return;
        }
        if (!shipmentReadyForPayment(player)) {
            return;
        }
        long total = shipmentTotal();
        if (total <= 0L || ownerAccountId == null) {
            player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
            return;
        }
        ItemStack card = cardContainer.getItem(0);
        if (card.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.economia.mail.no_card"));
            return;
        }
        String requestToken = stableRequestId(requestId);
        String operationKey = "mail-payment:" + mailBlockId + ":" + player.getUUID() + ":" + requestToken;
        try {
            OperationStartResult startResult = operationService.begin(operationKey, EconomyOperationType.MAIL_PAYMENT,
                    player.getUUID(), shipmentOperationPayload(total, creditPayment ? "credit" : "debit", 0L));
            if (startResult.type() == OperationStartType.DUPLICATE_COMPLETED) {
                player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_success"));
                return;
            }
            if (startResult.type() != OperationStartType.CREATED) {
                player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
                return;
            }

            if (!operationService.mark(operationKey, EconomyOperationState.ITEMS_RESERVED)) {
                operationService.markReconciliationRequired(operationKey, "unable to reserve mail card payment");
                player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
                return;
            }

            String key = "mail-card:" + mailBlockId + ":" + player.getUUID() + ":" + requestToken;
            boolean paid;
            if (creditPayment) {
                var result = cardPaymentService.creditPurchase(card, ownerAccountId, total, player.getUUID(), "Correio", key);
                paid = result.type() == CreditPurchaseResultType.COMPLETED;
            } else {
                var result = cardPaymentService.debitPurchase(card, ownerAccountId, total, player.getUUID(), key);
                paid = result.type() == DebitPurchaseResultType.COMPLETED;
            }
            if (!paid) {
                operationService.markReconciliationRequired(operationKey, "card payment was not a fresh completed transaction");
                player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
                return;
            }
            operationService.mark(operationKey, EconomyOperationState.SQL_COMMITTED);
            returnCard(player);
            changeWarningFlag = 0;
            paymentOpen = false;
            if (!finishPaidShipment(player)) {
                operationService.markReconciliationRequired(operationKey, "card charged but shipment was not persisted");
                syncState(player);
                return;
            }
            operationService.mark(operationKey, EconomyOperationState.ITEMS_DELIVERED);
            operationService.mark(operationKey, EconomyOperationState.COMPLETED);
            syncState(player);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_success"));
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao pagar Correio com cartao.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_failed"));
        }
    }

    public void sendShipment(ServerPlayer player) {
        if (selectedRecipient() == null) {
            return;
        }
        if (occupiedSendSlots() == 0) {
            player.sendSystemMessage(Component.translatable("commands.economia.mail.no_items"));
            return;
        }
        if (!paymentComplete()) {
            player.sendSystemMessage(Component.translatable("commands.economia.mail.payment_required"));
            return;
        }
        try {
            if (!inventoryService.insertShipment(selectedRecipientId, sendContainer, registries)) {
                player.sendSystemMessage(Component.translatable("commands.economia.mail.recipient_full"));
                return;
            }
            sendContainer.clearContent();
            paymentFlag = 0;
            syncState(player);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.send_success"));
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao enviar encomenda.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.send_failed"));
        }
    }

    public void syncState(ServerPlayer player) {
        loadRecipients();
        PacketDistributor.sendToPlayer(player, new MailStatePayload(named(), mailName, selectedRecipientId, paymentComplete(), changeWarning(), recipients));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
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
        } else if (cardItemDataService.isValidCardItem(current)) {
            if (!paymentOpen) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(current, CARD_SLOT, CARD_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MoneyStackCalculator.isBanknote(current)) {
            if (!paymentOpen) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(current, CASH_START, CASH_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedRecipient() != null) {
            if (!moveItemStackTo(current, SEND_START, SEND_END, false)) {
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
        if (slotId >= 0 && slotId < slots.size() && !visibleSlot(slotId)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (serverSide && player instanceof ServerPlayer serverPlayer) {
            try {
                saveReceivedIfAllowed(serverPlayer);
            } catch (SQLException exception) {
                EconomiaMod.LOGGER.warn("Falha ao salvar recebimentos do Correio.", exception);
                serverPlayer.sendSystemMessage(Component.translatable("commands.economia.mail.send_failed"));
            }
            inventoryService.giveContainer(serverPlayer, sendContainer);
            inventoryService.giveContainer(serverPlayer, cardContainer);
            if (!paymentComplete()) {
                inventoryService.giveContainer(serverPlayer, cashContainer);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return CommercialMenuAccess.stillValid(player, accessPos, expectedBlock);
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
                return namedFlag;
            }

            @Override
            public void set(int value) {
                namedFlag = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return paymentFlag;
            }

            @Override
            public void set(int value) {
                paymentFlag = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return changeWarningFlag;
            }

            @Override
            public void set(int value) {
                changeWarningFlag = value;
            }
        });
    }

    private void addMailSlots() {
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 9; column++) {
                int menuSlot = column + row * 9;
                addSlot(new Slot(sendContainer, menuSlot, 22 + column * 18, 76 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return clientSlotActive(menuSlot);
                    }
                });
            }
        }
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 9; column++) {
                int menuSlot = RECEIVE_START + column + row * 9;
                addSlot(new Slot(receiveContainer, column + row * 9, 22 + column * 18, 108 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return ownerMode();
                    }

                    @Override
                    public boolean isActive() {
                        return clientSlotActive(menuSlot);
                    }
                });
            }
        }
        addSlot(new Slot(cardContainer, 0, 198, 76) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return cardItemDataService.isValidCardItem(stack);
            }

            @Override
            public boolean isActive() {
                return clientSlotActive(CARD_SLOT);
            }
        });
        for (int slot = 0; slot < 6; slot++) {
            int menuSlot = CASH_START + slot;
            addSlot(new Slot(cashContainer, slot, 132 + (slot % 3) * 18, 104 + (slot / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return MoneyStackCalculator.isBanknote(stack);
                }

                @Override
                public boolean isActive() {
                    return clientSlotActive(menuSlot);
                }
            });
        }
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int menuSlot = PLAYER_START + column + row * 9;
                addSlot(new Slot(inventory, column + row * 9 + 9, 22 + column * 18, 156 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return clientSlotActive(menuSlot);
                    }
                });
            }
        }
        for (int column = 0; column < 9; column++) {
            int menuSlot = PLAYER_START + 27 + column;
            addSlot(new Slot(inventory, column, 22 + column * 18, 214) {
                @Override
                public boolean isActive() {
                    return clientSlotActive(menuSlot);
                }
            });
        }
    }

    private SimpleContainer loadReceiveContainer() {
        if (mailBlockId == null) {
            return new SimpleContainer(18);
        }
        try {
            return inventoryService.loadReceived(mailBlockId, registries);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao carregar recebimentos do Correio.", exception);
            return new SimpleContainer(18);
        }
    }

    private void saveReceivedIfAllowed(ServerPlayer player) throws SQLException {
        if (mailBlockId != null && player.getUUID().equals(ownerPlayerUuid)) {
            inventoryService.saveReceived(mailBlockId, receiveContainer, registries);
        }
    }

    private void loadRecipients() {
        if (mailBlockId == null) {
            return;
        }
        try {
            recipients.clear();
            for (MailRecipientRecord recipient : recipientRepository.list(mailBlockId)) {
                recipients.add(new MailStatePayload.RecipientSummary(
                        recipient.destinationBlockId(),
                        recipient.ownerName(),
                        recipient.mailName(),
                        recipient.dimension(),
                        recipient.x(),
                        recipient.y(),
                        recipient.z()
                ));
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao listar destinatarios do Correio.", exception);
        }
    }

    private long moneyIn(Container container) {
        long total = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            OptionalLong value = MoneyStackCalculator.banknoteValue(stack);
            if (value.isPresent()) {
                total += value.getAsLong() * stack.getCount();
            }
        }
        return total;
    }

    private boolean creditChange(ServerPlayer player, UUID requestId, long change, boolean confirmOwnerChange) {
        try {
            UUID destinationAccount = ownerAccountId;
            ItemStack card = cardContainer.getItem(0);
            if (!confirmOwnerChange && !card.isEmpty()) {
                CardValidationResult cardResult = cardValidationService.validate(card);
                if (cardResult.type() != CardValidationResultType.VALID) {
                    return false;
                }
                destinationAccount = cardResult.accountId();
            }
            if (destinationAccount == null) {
                return false;
            }
            String key = "mail-cash-change:" + mailBlockId + ":" + player.getUUID() + ":" + stableRequestId(requestId);
            var result = accountFinancialService.deposit(player.getUUID(), destinationAccount, change, key);
            return result.type() == FinancialOperationResultType.COMPLETED;
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao creditar troco do Correio.", exception);
            return false;
        }
    }

    private boolean shipmentReadyForPayment(ServerPlayer player) {
        if (selectedRecipient() == null) {
            return false;
        }
        if (occupiedSendSlots() == 0) {
            player.sendSystemMessage(Component.translatable("commands.economia.mail.no_items"));
            return false;
        }
        try {
            SimpleContainer received = inventoryService.loadReceived(selectedRecipientId, registries);
            if (!inventoryService.canInsertAll(received, sendContainer)) {
                player.sendSystemMessage(Component.translatable("commands.economia.mail.recipient_full"));
                return false;
            }
            return true;
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao validar espaco do destinatario do Correio.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.send_failed"));
            return false;
        }
    }

    private boolean finishPaidShipment(ServerPlayer player) {
        try {
            if (!inventoryService.insertShipment(selectedRecipientId, sendContainer, registries)) {
                player.sendSystemMessage(Component.translatable("commands.economia.mail.recipient_full"));
                return false;
            }
            sendContainer.clearContent();
            paymentFlag = 0;
            broadcastChanges();
            player.sendSystemMessage(Component.translatable("commands.economia.mail.send_success"));
            return true;
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao finalizar envio pago do Correio.", exception);
            player.sendSystemMessage(Component.translatable("commands.economia.mail.send_failed"));
            return false;
        }
    }

    private String shipmentOperationPayload(long total, String paymentMethod, long change) {
        StringBuilder payload = new StringBuilder(256)
                .append("recipient=").append(selectedRecipientId)
                .append(";total=").append(total)
                .append(";method=").append(paymentMethod)
                .append(";change=").append(change);
        for (int slot = 0; slot < sendContainer.getContainerSize(); slot++) {
            ItemStack stack = sendContainer.getItem(slot);
            if (!stack.isEmpty()) {
                payload.append(";slot").append(slot).append('=')
                        .append(stack.saveOptional(registries));
            }
        }
        return payload.toString();
    }

    private void returnCard(ServerPlayer player) {
        inventoryService.giveContainer(player, cardContainer);
    }

    private String stableRequestId(UUID requestId) {
        return (requestId == null ? UUID.randomUUID() : requestId).toString();
    }

    private static void writeNullableUuid(FriendlyByteBuf buffer, UUID value) {
        buffer.writeUUID(value == null ? EMPTY_UUID : value);
    }

    private static UUID readNullableUuid(FriendlyByteBuf buffer) {
        UUID value = buffer.readUUID();
        return EMPTY_UUID.equals(value) ? null : value;
    }
}
