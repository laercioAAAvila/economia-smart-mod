package br.com.economiamod.registry;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.menu.AtmMenu;
import br.com.economiamod.common.menu.BankCounterMenu;
import br.com.economiamod.common.menu.BuyShopMenu;
import br.com.economiamod.common.menu.SellShopMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, EconomiaMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AtmMenu>> ATM =
            MENUS.register("atm", () -> IMenuTypeExtension.create(AtmMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BankCounterMenu>> BANK_COUNTER =
            MENUS.register("bank_counter", () -> IMenuTypeExtension.create(BankCounterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SellShopMenu>> SELL_SHOP =
            MENUS.register("sell_shop", () -> IMenuTypeExtension.create(SellShopMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BuyShopMenu>> BUY_SHOP =
            MENUS.register("buy_shop", () -> IMenuTypeExtension.create(BuyShopMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
