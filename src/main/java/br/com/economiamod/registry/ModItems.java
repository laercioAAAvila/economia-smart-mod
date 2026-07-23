package br.com.economiamod.registry;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.item.EconomyCardItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EconomiaMod.MOD_ID);

    public static final DeferredItem<Item> BANKNOTE_1 = registerBanknote("banknote_1");
    public static final DeferredItem<Item> BANKNOTE_2 = registerBanknote("banknote_2");
    public static final DeferredItem<Item> BANKNOTE_5 = registerBanknote("banknote_5");
    public static final DeferredItem<Item> BANKNOTE_10 = registerBanknote("banknote_10");
    public static final DeferredItem<Item> BANKNOTE_20 = registerBanknote("banknote_20");
    public static final DeferredItem<Item> BANKNOTE_50 = registerBanknote("banknote_50");
    public static final DeferredItem<Item> BANKNOTE_100 = registerBanknote("banknote_100");
    public static final DeferredItem<Item> BANKNOTE_200 = registerBanknote("banknote_200");

    public static final DeferredItem<Item> DEBIT_CARD = registerCard("debit_card");
    public static final DeferredItem<Item> CREDIT_CARD = registerCard("credit_card");
    public static final DeferredItem<Item> DEBIT_CREDIT_CARD = registerCard("debit_credit_card");
    public static final DeferredItem<Item> CREDIT_INVOICE = ITEMS.registerSimpleItem("credit_invoice", new Item.Properties().stacksTo(1));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static DeferredItem<Item> registerBanknote(String name) {
        return ITEMS.registerSimpleItem(name, new Item.Properties().stacksTo(64));
    }

    private static DeferredItem<Item> registerCard(String name) {
        return ITEMS.register(name, () -> new EconomyCardItem(new Item.Properties().stacksTo(1)));
    }
}
