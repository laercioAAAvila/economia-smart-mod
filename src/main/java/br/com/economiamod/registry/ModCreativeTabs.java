package br.com.economiamod.registry;

import br.com.economiamod.EconomiaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EconomiaMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ECONOMIA_TAB = TABS.register("economia", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.economia.economia"))
            .icon(() -> new ItemStack(ModItems.BANKNOTE_100.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.BANKNOTE_1.get());
                output.accept(ModItems.BANKNOTE_2.get());
                output.accept(ModItems.BANKNOTE_5.get());
                output.accept(ModItems.BANKNOTE_10.get());
                output.accept(ModItems.BANKNOTE_20.get());
                output.accept(ModItems.BANKNOTE_50.get());
                output.accept(ModItems.BANKNOTE_100.get());
                output.accept(ModItems.BANKNOTE_200.get());
                output.accept(ModItems.DEBIT_CARD.get());
                output.accept(ModItems.CREDIT_CARD.get());
                output.accept(ModItems.DEBIT_CREDIT_CARD.get());
                output.accept(ModBlocks.ATM_ITEM.get());
                output.accept(ModBlocks.SELL_SHOP_ITEM.get());
                output.accept(ModBlocks.BUY_SHOP_ITEM.get());
                output.accept(ModBlocks.BANK_COUNTER_ITEM.get());
                output.accept(ModBlocks.MAIL_ITEM.get());
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
        eventBus.addListener(ModCreativeTabs::addVanillaTabContents);
    }

    private static void addVanillaTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.ATM_ITEM.get());
            event.accept(ModBlocks.SELL_SHOP_ITEM.get());
            event.accept(ModBlocks.BUY_SHOP_ITEM.get());
            event.accept(ModBlocks.BANK_COUNTER_ITEM.get());
            event.accept(ModBlocks.MAIL_ITEM.get());
        }

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BANKNOTE_1.get());
            event.accept(ModItems.BANKNOTE_2.get());
            event.accept(ModItems.BANKNOTE_5.get());
            event.accept(ModItems.BANKNOTE_10.get());
            event.accept(ModItems.BANKNOTE_20.get());
            event.accept(ModItems.BANKNOTE_50.get());
            event.accept(ModItems.BANKNOTE_100.get());
            event.accept(ModItems.BANKNOTE_200.get());
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.DEBIT_CARD.get());
            event.accept(ModItems.CREDIT_CARD.get());
            event.accept(ModItems.DEBIT_CREDIT_CARD.get());
        }
    }
}
