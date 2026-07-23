package br.com.economiamod.registry;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.block.HorizontalCommercialBlock;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EconomiaMod.MOD_ID);
    private static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(EconomiaMod.MOD_ID);

    public static final DeferredBlock<Block> ATM = registerCommercialBlock("atm", metalBlock(3.5F));
    public static final DeferredBlock<Block> SELL_SHOP = registerCommercialBlock("sell_shop", woodBlock(2.5F));
    public static final DeferredBlock<Block> BUY_SHOP = registerCommercialBlock("buy_shop", woodBlock(2.5F));
    public static final DeferredBlock<Block> BANK_COUNTER = registerCommercialBlock("bank_counter", metalBlock(4.0F));

    public static final DeferredItem<BlockItem> ATM_ITEM = registerBlockItem("atm", ATM);
    public static final DeferredItem<BlockItem> SELL_SHOP_ITEM = registerBlockItem("sell_shop", SELL_SHOP);
    public static final DeferredItem<BlockItem> BUY_SHOP_ITEM = registerBlockItem("buy_shop", BUY_SHOP);
    public static final DeferredItem<BlockItem> BANK_COUNTER_ITEM = registerBlockItem("bank_counter", BANK_COUNTER);

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
    }

    private static DeferredBlock<Block> registerCommercialBlock(String name, Supplier<BlockBehaviour.Properties> properties) {
        return BLOCKS.register(name, () -> new HorizontalCommercialBlock(properties.get()));
    }

    private static DeferredItem<BlockItem> registerBlockItem(String name, DeferredBlock<Block> block) {
        return BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static Supplier<BlockBehaviour.Properties> metalBlock(float strength) {
        return () -> BlockBehaviour.Properties.of()
                .strength(strength, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private static Supplier<BlockBehaviour.Properties> woodBlock(float strength) {
        return () -> BlockBehaviour.Properties.of()
                .strength(strength, 3.0F)
                .sound(SoundType.WOOD);
    }
}

