package br.com.economiamod.client.screen;

import br.com.economiamod.client.ModKeyMappings;
import br.com.economiamod.common.menu.PlayerShopMenu;
import br.com.economiamod.common.menu.ShopKind;
import br.com.economiamod.common.network.ShopAction;
import br.com.economiamod.common.network.ShopActionPayload;
import br.com.economiamod.common.network.ShopReferencePayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class PlayerShopScreen<T extends PlayerShopMenu> extends AbstractContainerScreen<T> {
    private EditBox price;
    private EditBox quantity;
    private EditBox tradeQuantity;
    private Button toggleButton;
    private Button tradeButton;
    private Button paymentModeButton;
    private boolean creditPaymentSelected;

    public PlayerShopScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 280;
        imageHeight = 244;
        inventoryLabelY = 148;
    }

    @Override
    protected void init() {
        super.init();
        price = addRenderableWidget(new EditBox(font, leftPos + 78, topPos + 56, 70, 18, Component.translatable("screen.economia.shop.unit_price")));
        price.setHint(Component.translatable("screen.economia.shop.unit_price"));
        price.setValue(String.valueOf(menu.price()));
        quantity = addRenderableWidget(new EditBox(font, leftPos + 166, topPos + 56, 58, 18, Component.translatable("screen.economia.shop.buy_limit")));
        quantity.setHint(Component.translatable("screen.economia.shop.buy_limit"));
        quantity.setValue(String.valueOf(menu.quantity()));
        tradeQuantity = addRenderableWidget(new EditBox(font, leftPos + 78, topPos + 122, 54, 18, Component.translatable("screen.economia.shop.buy_amount")));
        tradeQuantity.setHint(Component.translatable("screen.economia.shop.buy_amount_short"));
        tradeQuantity.setFilter(value -> value.matches("[0-9]*"));
        toggleButton = addRenderableWidget(button(leftPos + 190, topPos + 80, 78, toggleKey(), () -> save(!menu.active())));
        paymentModeButton = addRenderableWidget(button(leftPos + 188, topPos + 80, 80, paymentModeKey(), this::togglePaymentMode));
        tradeButton = addRenderableWidget(button(leftPos + 188, topPos + 104, 80, tradeKey(), this::trade));
        updateControls();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateControls();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF171B1D);
        graphics.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + 28, 0xFF40513B);
        graphics.fill(leftPos + 10, topPos + 40, leftPos + imageWidth - 10, topPos + 146, 0xFF20262A);
        drawSlotFrame(graphics, 22, 56);
        if (!menu.ownerMode()) {
            if (menu.shopKind() == ShopKind.BUY) {
                for (int slot = 0; slot < 9; slot++) {
                    drawSlotFrame(graphics, 58 + slot * 18, 56);
                }
            }
            drawSlotFrame(graphics, 228, 56);
        }
        if (menu.ownerMode()) {
            for (int slot = 0; slot < 9; slot++) {
                drawSlotFrame(graphics, 22 + slot * 18, 98);
                drawSlotFrame(graphics, 22 + slot * 18, 124);
            }
        }
        drawInventoryFrames(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 12, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable(menu.ownerMode() ? "screen.economia.shop.owner" : "screen.economia.shop.customer"), 12, 32, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.shop.item"), 20, 46, 0xB8C7C2, false);
        if (!menu.ownerMode()) {
            graphics.drawString(font, Component.translatable("screen.economia.shop.unit_price_value", menu.price()), 76, 80, 0xD7E8E1, false);
            if (menu.shopKind() == ShopKind.BUY) {
                graphics.drawString(font, Component.translatable("screen.economia.shop.buy_limit_value", menu.quantity()), 76, 92, 0xD7E8E1, false);
            } else {
                graphics.drawString(font, Component.translatable("screen.economia.shop.stock_value", menu.availableStock()), 76, 92, 0xD7E8E1, false);
                graphics.drawString(font, Component.translatable("screen.economia.shop.buy_amount"), 78, 112, 0xB8C7C2, false);
            }
            if (menu.shopKind() == ShopKind.BUY) {
                graphics.drawString(font, Component.translatable("screen.economia.shop.input_items"), 58, 46, 0xB8C7C2, false);
            }
            graphics.drawString(font, Component.translatable("screen.economia.shop.payment_method_short"), 226, 46, 0xB8C7C2, false);
            if (menu.shopKind() == ShopKind.SELL) {
                graphics.drawString(font, Component.translatable("screen.economia.shop.payment_method_short"), 188, 70, 0xB8C7C2, false);
            }
        } else {
            graphics.drawString(font, Component.translatable("screen.economia.shop.unit_price"), 78, 46, 0xB8C7C2, false);
            if (menu.shopKind() == ShopKind.BUY) {
                graphics.drawString(font, Component.translatable("screen.economia.shop.buy_limit"), 166, 46, 0xB8C7C2, false);
                graphics.drawString(font, Component.translatable("screen.economia.shop.unit_price_value", menu.price()), 78, 78, 0xD7E8E1, false);
                graphics.drawString(font, Component.translatable("screen.economia.shop.buy_limit_value", menu.quantity()), 78, 90, 0xD7E8E1, false);
            } else {
                graphics.drawString(font, Component.translatable("screen.economia.shop.current_sell_offer", menu.price()), 78, 78, 0xD7E8E1, false);
            }
            graphics.drawString(font, Component.translatable("screen.economia.shop.cash"), 22, 88, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable(menu.shopKind() == ShopKind.BUY ? "screen.economia.shop.purchased_stock" : "screen.economia.shop.stock"), 22, 114, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable(menu.active() ? "screen.economia.shop.active" : "screen.economia.shop.inactive"), 190, 106, menu.active() ? 0x80E6A8 : 0xE68E8E, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((isFocused(price) || isFocused(quantity) || isFocused(tradeQuantity)) && minecraft != null && minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return false;
        }
        if (!isFocused(price) && !isFocused(quantity) && !isFocused(tradeQuantity) && ModKeyMappings.SHOP_REFERENCE_FROM_JEI.matches(keyCode, scanCode) && acceptHoveredJeiReference()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (menu.visibleSlot(menu.slots.indexOf(slot))) {
            super.renderSlot(graphics, slot);
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot == null || menu.visibleSlot(slotId)) {
            super.slotClicked(slot, slotId, mouseButton, type);
        }
    }

    public boolean acceptsJeiReference() {
        return menu.shopKind() == ShopKind.BUY && !menu.active();
    }

    public Rect2i referenceSlotArea() {
        return new Rect2i(leftPos + 22, topPos + 56, 16, 16);
    }

    public void acceptJeiReference(ItemStack stack) {
        if (!acceptsJeiReference() || stack.isEmpty()) {
            return;
        }
        ItemStack reference = stack.copy();
        reference.setCount(1);
        PacketDistributor.sendToServer(new ShopReferencePayload(reference));
    }

    private boolean acceptHoveredJeiReference() {
        if (!acceptsJeiReference() || !net.neoforged.fml.ModList.get().isLoaded("jei")) {
            return false;
        }
        try {
            Class<?> bridge = Class.forName("br.com.economiamod.client.compat.jei.EconomiaJeiReferenceBridge");
            Object result = bridge.getMethod("itemUnderMouse").invoke(null);
            if (!(result instanceof ItemStack stack) || stack.isEmpty()) {
                return false;
            }
            acceptJeiReference(stack);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private void updateControls() {
        boolean owner = menu.ownerMode();
        price.visible = owner;
        price.active = owner && !menu.active();
        quantity.visible = owner && menu.shopKind() == ShopKind.BUY;
        quantity.active = owner && menu.shopKind() == ShopKind.BUY && !menu.active();
        tradeQuantity.visible = !owner && menu.shopKind() == ShopKind.SELL;
        tradeQuantity.active = !owner && menu.shopKind() == ShopKind.SELL && !Screen.hasShiftDown();
        toggleButton.visible = owner;
        toggleButton.active = owner;
        toggleButton.setMessage(Component.translatable(toggleKey()));
        paymentModeButton.visible = !owner && menu.shopKind() == ShopKind.SELL;
        paymentModeButton.active = paymentModeButton.visible;
        paymentModeButton.setMessage(Component.translatable(paymentModeKey()));
        tradeButton.visible = !owner;
        tradeButton.active = !owner;
    }

    private void save(boolean active) {
        long value;
        try {
            value = Long.parseLong(price.getValue());
        } catch (NumberFormatException exception) {
            value = 0L;
        }
        int amount = 1;
        try {
            if (menu.shopKind() == ShopKind.BUY) {
                amount = Integer.parseInt(quantity.getValue());
            }
        } catch (NumberFormatException exception) {
            amount = 1;
        }
        PacketDistributor.sendToServer(new ShopActionPayload(ShopAction.SAVE_CONFIG, value, Math.max(1, amount), active, UUID.randomUUID()));
    }

    private void trade() {
        PacketDistributor.sendToServer(new ShopActionPayload(ShopAction.TRADE, 0L, requestedTradeQuantity(), creditPaymentSelected, UUID.randomUUID()));
    }

    private void togglePaymentMode() {
        creditPaymentSelected = !creditPaymentSelected;
        updateControls();
    }

    private int requestedTradeQuantity() {
        if (Screen.hasShiftDown()) {
            return 0;
        }
        if (menu.shopKind() != ShopKind.SELL || tradeQuantity.getValue().isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(tradeQuantity.getValue()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private String tradeKey() {
        return menu.shopKind() == ShopKind.SELL ? "screen.economia.shop.buy" : "screen.economia.shop.sell";
    }

    private String paymentModeKey() {
        return creditPaymentSelected ? "screen.economia.shop.pay_credit" : "screen.economia.shop.pay_debit";
    }

    private String toggleKey() {
        return menu.active() ? "screen.economia.shop.enabled" : "screen.economia.shop.disabled";
    }

    private boolean isFocused(EditBox box) {
        return box != null && box.visible && box.isFocused();
    }

    private Button button(int x, int y, int width, String key, Runnable action) {
        return Button.builder(Component.translatable(key), ignored -> action.run())
                .bounds(x, y, width, 20)
                .build();
    }

    private void drawInventoryFrames(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotFrame(graphics, 22 + column * 18, 158 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlotFrame(graphics, 22 + column * 18, 216);
        }
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF111416);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF2B3337);
    }
}
