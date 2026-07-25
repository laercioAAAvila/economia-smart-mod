package br.com.economiamod.server.commercial;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.block.CommercialBlockEntity;
import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class CommercialBlockLifecycleService {
    private final CommercialBlockResolver blockResolver = new CommercialBlockResolver();
    private final CommercialBlockSqlService sqlService = new CommercialBlockSqlService();
    private final CommercialBlockDropService dropService = new CommercialBlockDropService();

    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!isCommercialBlock(event.getPlacedBlock())) {
            return;
        }

        if (!EconomyDatabaseState.isAvailable()) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("block.economia.commercial.sql_unavailable"), true);
            }
            return;
        }

        if (!event.getPlacedBlock().is(ModBlocks.BANK_COUNTER.get())) {
            registerPlacedBlock(event);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player && player.createCommandSourceStack().hasPermission(2)) {
            registerPlacedBlock(event);
            return;
        }

        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("block.economia.bank_counter.place_denied"), true);
        }
    }

    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!isCommercialBlock(event.getState())) {
            return;
        }

        if (!EconomyDatabaseState.isAvailable()) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("block.economia.commercial.sql_unavailable"), true);
            return;
        }

        if (!canBreakCommercialBlock(event)) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(breakDeniedMessage(event.getState()), true);
            return;
        }

        markRemoved(event);
    }

    private Component breakDeniedMessage(BlockState state) {
        if (state.is(ModBlocks.BANK_COUNTER.get())) {
            return Component.translatable("block.economia.bank_counter.break_denied");
        }
        return Component.translatable("block.economia.commercial.break_denied");
    }

    private boolean canBreakCommercialBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer().isCreative()) {
            return true;
        }

        if (event.getState().is(ModBlocks.BANK_COUNTER.get())) {
            return false;
        }

        CommercialBlockType blockType = blockResolver.typeOf(event.getState()).orElse(null);
        if (blockType == null) {
            return false;
        }

        if (blockType == CommercialBlockType.ATM
                || blockType == CommercialBlockType.SELL_SHOP
                || blockType == CommercialBlockType.BUY_SHOP
                || blockType == CommercialBlockType.MAIL) {
            return playerPlaced(event);
        }

        return false;
    }

    private boolean playerPlaced(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof CommercialBlockEntity blockEntity)) {
            return false;
        }
        try {
            return new CommercialOwnerRepository().placedBy(blockEntity.commercialBlockId())
                    .map(event.getPlayer().getUUID()::equals)
                    .orElse(false);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao verificar colocador do bloco comercial.", exception);
            return false;
        }
    }

    public boolean isCommercialBlock(BlockState state) {
        return state.is(ModBlocks.ATM.get())
                || state.is(ModBlocks.SELL_SHOP.get())
                || state.is(ModBlocks.BUY_SHOP.get())
                || state.is(ModBlocks.BANK_COUNTER.get())
                || state.is(ModBlocks.MAIL.get());
    }

    private void registerPlacedBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof CommercialBlockEntity blockEntity)) {
            return;
        }

        CommercialBlockType blockType = blockResolver.typeOf(event.getPlacedBlock()).orElse(null);
        if (blockType == null) {
            return;
        }

        UUID ownerUuid = blockType == CommercialBlockType.ATM
                || blockType == CommercialBlockType.SELL_SHOP
                || blockType == CommercialBlockType.BUY_SHOP
                || blockType == CommercialBlockType.MAIL
                ? player.getUUID()
                : null;

        try {
            sqlService.registerPlacedBlock(
                    blockEntity.commercialBlockId(),
                    blockType,
                    ownerUuid,
                    player.getUUID(),
                    level.dimension().location(),
                    event.getPos()
            );
        } catch (SQLException exception) {
            event.setCanceled(true);
            EconomiaMod.LOGGER.warn("Falha ao registrar bloco comercial no SQL.", exception);
            player.displayClientMessage(Component.translatable("block.economia.commercial.sql_unavailable"), true);
        }
    }

    private void markRemoved(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof CommercialBlockEntity blockEntity)) {
            return;
        }

        try {
            dropService.dropStoredItems(level, event.getPos(), blockEntity.commercialBlockId());
            sqlService.markRemoved(blockEntity.commercialBlockId());
        } catch (SQLException exception) {
            event.setCanceled(true);
            EconomiaMod.LOGGER.warn("Falha ao marcar bloco comercial como removido no SQL.", exception);
            event.getPlayer().displayClientMessage(Component.translatable("block.economia.commercial.sql_unavailable"), true);
        }
    }
}
