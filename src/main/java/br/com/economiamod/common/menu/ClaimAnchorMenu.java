package br.com.economiamod.common.menu;

import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.money.MoneyStackCalculator;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.server.claim.ClaimAnchorMenuState;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class ClaimAnchorMenu extends AbstractContainerMenu {
    private static final int CARD_SLOT = 0;
    private static final int CASH_START = 1;
    private static final int CASH_END = 7;
    private static final int PLAYER_START = CASH_END;
    private static final int PLAYER_END = PLAYER_START + 36;
    private final ClaimAnchorMenuState state;
    private final BlockPos accessPos;
    private final Block expectedBlock;
    private final SimpleContainer cardContainer = new SimpleContainer(1);
    private final SimpleContainer cashContainer = new SimpleContainer(6);
    private final CardItemDataService cardData = new CardItemDataService();
    private final CardValidationService cardValidation = new CardValidationService(cardData);
    private final AccountQueryService accountQuery = new AccountQueryService();
    private int authenticatedFlag;
    private int paymentOpenFlag;
    private int cashModeFlag;

    public ClaimAnchorMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, readState(data));
    }

    public ClaimAnchorMenu(int containerId, Inventory inventory, ClaimAnchorMenuState state) {
        super(ModMenus.CLAIM_ANCHOR.get(), containerId);
        this.state = state;
        this.accessPos = new BlockPos(state.blockX(), state.blockY(), state.blockZ());
        this.expectedBlock = state.groupType() == GroupType.CLAN
                ? ModBlocks.CLAN_CLAIM_BLOCK.get() : ModBlocks.PRIVATE_PROPERTY_CLAIM_BLOCK.get();
        addPaymentSlots();
        addPlayerInventory(inventory);
        addStateSlots();
    }

    public ClaimAnchorMenuState state() {
        return state;
    }

    public boolean paymentOpen() {
        return paymentOpenFlag == 1;
    }

    public boolean authenticate(net.minecraft.server.level.ServerPlayer player) throws SQLException {
        CardValidationResult card = cardValidation.validate(cardContainer.getItem(0));
        authenticatedFlag = card.type() == CardValidationResultType.VALID
                && accountQuery.playerOwnsActiveAccount(player.getUUID(), card.accountId()) ? 1 : 0;
        broadcastChanges();
        return authenticated();
    }

    public boolean authenticated() {
        return authenticatedFlag == 1;
    }

    public ItemStack authenticatedCard() {
        return authenticated() ? cardContainer.getItem(0).copy() : ItemStack.EMPTY;
    }

    public boolean cashMode() {
        return cashModeFlag == 1;
    }

    public void setPaymentMode(net.minecraft.server.level.ServerPlayer player, boolean open, boolean cashMode) {
        paymentOpenFlag = open ? 1 : 0;
        cashModeFlag = open && cashMode ? 1 : 0;
        if (!open || cashMode) {
            clearContainer(player, cardContainer);
        }
        if (!open || !cashMode) {
            clearContainer(player, cashContainer);
        }
        broadcastChanges();
    }

    public ItemStack paymentCard() {
        return cardContainer.getItem(0);
    }

    public SimpleContainer paymentCash() {
        return cashContainer;
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, ClaimAnchorMenuState state) {
        buffer.writeUUID(state.anchorId());
        buffer.writeBoolean(state.territoryId() != null);
        if (state.territoryId() != null) buffer.writeUUID(state.territoryId());
        buffer.writeVarInt(state.groupType().ordinal());
        buffer.writeUtf(state.dimension(), 255);
        buffer.writeBlockPos(new BlockPos(state.blockX(), state.blockY(), state.blockZ()));
        buffer.writeLong(state.landPrice());
        buffer.writeLong(state.landDebt());
        buffer.writeVarInt(state.territoryCount());
        buffer.writeVarInt(state.territoryLimit());
        buffer.writeBoolean(state.active());
        buffer.writeBoolean(state.canManage());
        buffer.writeBoolean(state.canClaim());
        buffer.writeLong(state.anchorPrice());
        buffer.writeVarInt(state.anchorDaysRemaining());
        buffer.writeVarInt(state.defaultAnchorDays());
        buffer.writeVarInt(state.maxAnchorDays());
        buffer.writeLong(state.suggestedSalePrice());
        buffer.writeVarInt(state.chunkCount());
        buffer.writeVarInt(state.chunkLimit());
        buffer.writeLong(state.nextChunkPrice());
        buffer.writeBoolean(state.canBuyChunk());
        buffer.writeLong(state.currentTax());
        buffer.writeLong(state.totalTax());
        buffer.writeVarInt(state.taxCount());
        buffer.writeVarInt(state.taxPeriodDays());
        buffer.writeVarInt(state.privateMembers().size());
        for (var member : state.privateMembers()) {
            buffer.writeUUID(member.playerUuid());
            buffer.writeUtf(member.playerName(), 64);
            buffer.writeVarInt(member.permissionMask());
        }
    }

    private static ClaimAnchorMenuState readState(FriendlyByteBuf buffer) {
        UUID anchorId = buffer.readUUID();
        UUID territoryId = buffer.readBoolean() ? buffer.readUUID() : null;
        GroupType type = GroupType.values()[buffer.readVarInt()];
        String dimension = buffer.readUtf(255);
        BlockPos pos = buffer.readBlockPos();
        long landPrice = buffer.readLong();
        long landDebt = buffer.readLong();
        int territoryCount = buffer.readVarInt();
        int territoryLimit = buffer.readVarInt();
        boolean active = buffer.readBoolean();
        boolean canManage = buffer.readBoolean();
        boolean canClaim = buffer.readBoolean();
        long anchorPrice = buffer.readLong();
        int anchorDays = buffer.readVarInt();
        int defaultDays = buffer.readVarInt();
        int maxDays = buffer.readVarInt();
        long salePrice = buffer.readLong();
        int chunkCount = buffer.readVarInt();
        int chunkLimit = buffer.readVarInt();
        long nextChunkPrice = buffer.readLong();
        boolean canBuyChunk = buffer.readBoolean();
        long currentTax = buffer.readLong();
        long totalTax = buffer.readLong();
        int taxCount = buffer.readVarInt();
        int taxPeriodDays = buffer.readVarInt();
        int memberCount = buffer.readVarInt();
        java.util.List<br.com.economiamod.server.claim.PrivatePropertyMemberView> members = new java.util.ArrayList<>();
        for (int index = 0; index < memberCount; index++) {
            members.add(new br.com.economiamod.server.claim.PrivatePropertyMemberView(
                    buffer.readUUID(), buffer.readUtf(64), buffer.readVarInt()));
        }
        return new ClaimAnchorMenuState(anchorId, territoryId, type, dimension, pos.getX(), pos.getY(), pos.getZ(),
                landPrice, landDebt, territoryCount, territoryLimit, active, canManage, canClaim, anchorPrice,
                anchorDays, defaultDays, maxDays, salePrice, chunkCount, chunkLimit, nextChunkPrice, canBuyChunk,
                currentTax, totalTax, taxCount, taxPeriodDays, java.util.List.copyOf(members));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if ((!paymentOpen() && authenticated()) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(current, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if ((!authenticated() || paymentOpen() && !cashMode()) && cardData.isValidCardItem(current)) {
            if (!moveItemStackTo(current, CARD_SLOT, CARD_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (cashMode() && MoneyStackCalculator.isBanknote(current)) {
            if (!moveItemStackTo(current, CASH_START, CASH_END, false)) {
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
    public boolean stillValid(Player player) {
        return CommercialMenuAccess.stillValid(player, accessPos, expectedBlock);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        paymentOpenFlag = 0;
        cashModeFlag = 0;
        authenticatedFlag = 0;
        if (!player.level().isClientSide()) {
            clearContainer(player, cardContainer);
            clearContainer(player, cashContainer);
        }
    }

    private void addPaymentSlots() {
        addSlot(new Slot(cardContainer, 0, 129, 88) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return cardData.isValidCardItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return !authenticated() || paymentOpen() && !cashMode();
            }
        });
        for (int slot = 0; slot < 6; slot++) {
            addSlot(new Slot(cashContainer, slot, 111 + (slot % 3) * 18, 82 + (slot / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return MoneyStackCalculator.isBanknote(stack);
                }

                @Override
                public boolean isActive() {
                    return paymentOpen() && cashMode();
                }
            });
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 58 + column * 18, 156 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return !authenticated() || paymentOpen();
                    }
                });
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 58 + column * 18, 214) {
                @Override
                public boolean isActive() {
                    return !authenticated() || paymentOpen();
                }
            });
        }
    }

    private void addStateSlots() {
        addDataSlot(flag(() -> authenticatedFlag, value -> authenticatedFlag = value));
        addDataSlot(flag(() -> paymentOpenFlag, value -> paymentOpenFlag = value));
        addDataSlot(flag(() -> cashModeFlag, value -> cashModeFlag = value));
    }

    private DataSlot flag(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt();
            }

            @Override
            public void set(int value) {
                setter.accept(value);
            }
        };
    }
}
