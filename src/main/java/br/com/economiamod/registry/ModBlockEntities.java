package br.com.economiamod.registry;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.block.CommercialBlockEntity;
import br.com.economiamod.common.block.GroupChestBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EconomiaMod.MOD_ID);

    public static final Supplier<BlockEntityType<CommercialBlockEntity>> COMMERCIAL_BLOCK =
            BLOCK_ENTITY_TYPES.register("commercial_block", () -> BlockEntityType.Builder.of(
                    CommercialBlockEntity::new,
                    ModBlocks.ATM.get(),
                    ModBlocks.SELL_SHOP.get(),
                    ModBlocks.BUY_SHOP.get(),
                    ModBlocks.BANK_COUNTER.get(),
                    ModBlocks.MAIL.get()
            ).build(null));

    public static final Supplier<BlockEntityType<GroupChestBlockEntity>> GROUP_CHEST =
            BLOCK_ENTITY_TYPES.register("group_chest", () -> BlockEntityType.Builder.of(
                    GroupChestBlockEntity::new,
                    ModBlocks.CLAN_CHEST.get(),
                    ModBlocks.PRIVATE_PROPERTY_CHEST.get()
            ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
