package br.com.economiamod.server.commercial;

import br.com.economiamod.common.menu.AtmMenu;
import br.com.economiamod.common.menu.BankCounterMenu;
import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.block.CommercialBlockEntity;
import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.common.menu.BuyShopMenu;
import br.com.economiamod.common.menu.MailMenu;
import br.com.economiamod.common.menu.SellShopMenu;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import br.com.economiamod.server.mail.MailBlockRecord;
import br.com.economiamod.server.mail.MailBlockRepository;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.state.BlockState;

public final class CommercialMenuOpenService {
    private final CommercialOwnerRepository ownerRepository = new CommercialOwnerRepository();
    private final CommercialBlockTypeRepository blockTypeRepository = new CommercialBlockTypeRepository();
    private final MailBlockRepository mailBlockRepository = new MailBlockRepository();

    public void open(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!EconomyDatabaseState.isAvailable()) {
            player.displayClientMessage(Component.translatable("commands.economia.unavailable"), true);
            return;
        }

        if (state.is(ModBlocks.ATM.get())) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new AtmMenu(containerId, inventory, pos),
                    Component.translatable("screen.economia.atm.title")
            ));
            return;
        }

        if (state.is(ModBlocks.BANK_COUNTER.get())) {
            if (!player.createCommandSourceStack().hasPermission(2)) {
                player.displayClientMessage(Component.translatable("block.economia.bank_counter.place_denied"), true);
                return;
            }
            UUID commercialBlockId;
            try {
                commercialBlockId = bankCounterId(level, pos, player);
            } catch (SQLException exception) {
                EconomiaMod.LOGGER.warn("Falha ao carregar bancada bancaria.", exception);
                player.displayClientMessage(Component.translatable("commands.economia.unavailable"), true);
                return;
            }
            if (commercialBlockId == null) {
                player.displayClientMessage(Component.translatable("block.economia.commercial.registration_missing"), true);
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new BankCounterMenu(containerId, inventory, pos, commercialBlockId),
                    Component.translatable("screen.economia.bank_counter.title")
            ));
            return;
        }

        if (state.is(ModBlocks.SELL_SHOP.get()) || state.is(ModBlocks.BUY_SHOP.get())) {
            openShop(level, pos, state, player);
            return;
        }

        if (state.is(ModBlocks.MAIL.get())) {
            openMail(level, pos, player);
        }
    }

    private UUID bankCounterId(ServerLevel level, BlockPos pos, ServerPlayer player) throws SQLException {
        if (!(level.getBlockEntity(pos) instanceof CommercialBlockEntity blockEntity)) {
            return null;
        }

        UUID commercialBlockId = blockEntity.commercialBlockId();
        if (blockTypeRepository.findType(commercialBlockId).orElse(null) == CommercialBlockType.BANK_COUNTER) {
            return commercialBlockId;
        }

        return null;
    }

    private void openShop(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (!(level.getBlockEntity(pos) instanceof CommercialBlockEntity blockEntity)) {
            return;
        }

        UUID commercialBlockId = blockEntity.commercialBlockId();
        UUID ownerUuid;
        try {
            ownerUuid = ownerRepository.owner(commercialBlockId).orElse(null);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao carregar dono da loja.", exception);
            player.displayClientMessage(Component.translatable("commands.economia.unavailable"), true);
            return;
        }
        if (ownerUuid == null) {
            player.displayClientMessage(Component.translatable("block.economia.commercial.registration_missing"), true);
            return;
        }
        final UUID menuOwnerUuid = ownerUuid;
        final boolean owner = player.getUUID().equals(menuOwnerUuid);

        if (state.is(ModBlocks.SELL_SHOP.get())) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new SellShopMenu(containerId, inventory, commercialBlockId, menuOwnerUuid, pos, ModBlocks.SELL_SHOP.get(), owner),
                    Component.translatable("screen.economia.sell_shop.title")
            ));
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new BuyShopMenu(containerId, inventory, commercialBlockId, menuOwnerUuid, pos, ModBlocks.BUY_SHOP.get(), owner),
                Component.translatable("screen.economia.buy_shop.title")
        ));
    }

    private void openMail(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!(level.getBlockEntity(pos) instanceof CommercialBlockEntity blockEntity)) {
            return;
        }

        UUID commercialBlockId = blockEntity.commercialBlockId();
        MailBlockRecord record;
        try {
            record = mailBlockRepository.findActive(commercialBlockId).orElse(null);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao carregar Correio.", exception);
            player.displayClientMessage(Component.translatable("block.economia.commercial.registration_missing"), true);
            return;
        }
        if (record == null) {
            player.displayClientMessage(Component.translatable("commands.economia.unavailable"), true);
            return;
        }
        MailBlockRecord menuRecord = record;
        boolean owner = player.getUUID().equals(menuRecord.ownerPlayerUuid());
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new MailMenu(
                        containerId,
                        inventory,
                        menuRecord.id(),
                        menuRecord.ownerPlayerUuid(),
                        menuRecord.ownerAccountId(),
                        menuRecord.dimension(),
                        menuRecord.name(),
                        menuRecord.x(),
                        menuRecord.y(),
                        menuRecord.z(),
                        pos,
                        ModBlocks.MAIL.get(),
                        owner,
                        menuRecord.named()
                ),
                Component.translatable("screen.economia.mail.title")
        ), data -> MailMenu.writeOpeningData(data, menuRecord, pos, owner));
    }

}
