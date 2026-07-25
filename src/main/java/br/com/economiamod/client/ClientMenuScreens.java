package br.com.economiamod.client;

import br.com.economiamod.client.screen.AtmScreen;
import br.com.economiamod.client.screen.BankCounterScreen;
import br.com.economiamod.client.screen.BuyShopScreen;
import br.com.economiamod.client.screen.MailScreen;
import br.com.economiamod.client.screen.SellShopScreen;
import br.com.economiamod.registry.ModMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ClientMenuScreens {
    private ClientMenuScreens() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ATM.get(), AtmScreen::new);
        event.register(ModMenus.BANK_COUNTER.get(), BankCounterScreen::new);
        event.register(ModMenus.SELL_SHOP.get(), SellShopScreen::new);
        event.register(ModMenus.BUY_SHOP.get(), BuyShopScreen::new);
        event.register(ModMenus.MAIL.get(), MailScreen::new);
    }
}
