package br.com.economiamod.common.menu;

import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.money.MoneyStackCalculator;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.server.claim.ClaimAnchorMenuState;
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
    }

    private static ClaimAnchorMenuState readState(FriendlyByteBuf buffer) {
        UUID anchorId = buffer.readUUID();
        UUID territoryId = buffer.readBoolean() ? buffer.readUUID() : null;
        GroupType type = GroupType.values()[buffer.readVarInt()];
        String dimension = buffer.readUtf(255);
        BlockPos pos = buffer.readBlockPos();
        return new ClaimAnchorMenuState(anchorId, territoryId, type, dimension, pos.getX(), pos.getY(), pos.getZ(),
                buffer.readLong(), buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readLong(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readLong(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readLong(), buffer.readBoolean());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!paymentOpen() || index < 0 || index >= slots.size()) {
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
        } else if (!cashMode() && cardData.isValidCardItem(current)) {
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
                return paymentOpen() && !cashMode();
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
                        return paymentOpen();
                    }
                });
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 58 + column * 18, 214) {
                @Override
                public boolean isActive() {
                    return paymentOpen();
                }
            });
        }
    }

    private void addStateSlots() {
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
