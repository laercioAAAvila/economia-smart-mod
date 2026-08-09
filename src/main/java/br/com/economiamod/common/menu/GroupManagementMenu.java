package br.com.economiamod.common.menu;

import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import java.sql.SQLException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class GroupManagementMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = 37;
    private final SimpleContainer cardContainer = new SimpleContainer(1);
    private final CardItemDataService cardData = new CardItemDataService();
    private final CardValidationService cardValidation = new CardValidationService(cardData);
    private final AccountQueryService accountQuery = new AccountQueryService();
    private final GroupType groupType;
    private final BlockPos accessPos;
    private final Block expectedBlock;
    private boolean authenticated;

    public GroupManagementMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, GroupType.values()[data.readVarInt()], data.readBlockPos());
    }

    public GroupManagementMenu(int containerId, Inventory inventory, GroupType groupType, BlockPos accessPos) {
        super(ModMenus.GROUP_MANAGEMENT.get(), containerId);
        this.groupType = groupType;
        this.accessPos = accessPos;
        this.expectedBlock = groupType == GroupType.CLAN
                ? ModBlocks.CLAN_MANAGEMENT_BLOCK.get()
                : ModBlocks.PRIVATE_PROPERTY_MANAGEMENT_BLOCK.get();
        addSlot(new Slot(cardContainer, 0, 80, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return cardData.isValidCardItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerInventory(inventory);
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, GroupType type, BlockPos pos) {
        buffer.writeVarInt(type.ordinal());
        buffer.writeBlockPos(pos);
    }

    public boolean authenticate(ServerPlayer player) throws SQLException {
        CardValidationResult card = cardValidation.validate(cardContainer.getItem(0));
        authenticated = card.type() == CardValidationResultType.VALID
                && accountQuery.findActiveAccountIdByPlayer(player.getUUID()).filter(card.accountId()::equals).isPresent();
        return authenticated;
    }

    public boolean authenticated() {
        return authenticated;
    }

    public GroupType groupType() {
        return groupType;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (slotIndex == 0) {
            if (!moveItemStackTo(current, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (cardData.isValidCardItem(current)) {
            if (!moveItemStackTo(current, 0, 1, false)) {
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
        authenticated = false;
        if (!player.level().isClientSide()) {
            clearContainer(player, cardContainer);
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 156 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 214));
        }
    }
}
