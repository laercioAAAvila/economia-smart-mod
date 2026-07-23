package br.com.economiamod.client.compat.jei;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.client.screen.BuyShopScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class EconomiaJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(BuyShopScreen.class, new BuyShopReferenceGhostIngredientHandler());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        EconomiaJeiReferenceBridge.setRuntime(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        EconomiaJeiReferenceBridge.clearRuntime();
    }
}
