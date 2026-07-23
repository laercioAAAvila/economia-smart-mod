package br.com.economiamod.client.compat.jei;

import br.com.economiamod.client.screen.BuyShopScreen;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

final class BuyShopReferenceGhostIngredientHandler implements IGhostIngredientHandler<BuyShopScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(BuyShopScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> stack = ingredient.getItemStack();
        if (!screen.acceptsJeiReference() || stack.isEmpty() || stack.get().isEmpty()) {
            return List.of();
        }
        return List.of(new ReferenceTarget<>(screen, stack.get()));
    }

    @Override
    public void onComplete() {
    }

    private record ReferenceTarget<I>(BuyShopScreen screen, ItemStack stack) implements Target<I> {
        @Override
        public Rect2i getArea() {
            return screen.referenceSlotArea();
        }

        @Override
        public void accept(I ingredient) {
            screen.acceptJeiReference(stack);
        }
    }
}
