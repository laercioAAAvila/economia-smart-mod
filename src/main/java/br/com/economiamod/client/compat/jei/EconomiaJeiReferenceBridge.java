package br.com.economiamod.client.compat.jei;

import java.util.Optional;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

public final class EconomiaJeiReferenceBridge {
    private static IJeiRuntime runtime;

    private EconomiaJeiReferenceBridge() {
    }

    static void setRuntime(IJeiRuntime runtime) {
        EconomiaJeiReferenceBridge.runtime = runtime;
    }

    static void clearRuntime() {
        runtime = null;
    }

    public static ItemStack itemUnderMouse() {
        if (runtime == null) {
            return ItemStack.EMPTY;
        }
        Optional<ITypedIngredient<?>> ingredient = runtime.getIngredientListOverlay().getIngredientUnderMouse();
        return ingredient.flatMap(ITypedIngredient::getItemStack)
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }
}
