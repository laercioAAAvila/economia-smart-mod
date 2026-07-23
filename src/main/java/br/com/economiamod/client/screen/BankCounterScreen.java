package br.com.economiamod.client.screen;

import br.com.economiamod.common.menu.BankCounterMenu;
import br.com.economiamod.common.network.BankCounterActionPayload;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BankCounterScreen extends AbstractContainerScreen<BankCounterMenu> {
    private static final int PRICE_REFRESH_TICKS = 200;

    private EditBox redeemAmount;
    private Button mintButton;
    private Button nuggetButton;
    private Button ingotButton;
    private Button blockButton;
    private int priceRefreshTicks;

    public BankCounterScreen(BankCounterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 300;
        imageHeight = 292;
        inventoryLabelY = 194;
    }

    @Override
    protected void init() {
        super.init();
        redeemAmount = addRenderableWidget(new EditBox(font, leftPos + 34, topPos + 160, 54, 18, Component.translatable("screen.economia.bank_counter.gold_amount")));
        redeemAmount.setHint(Component.translatable("screen.economia.bank_counter.gold_amount"));
        redeemAmount.setResponder(ignored -> updateButtons());

        mintButton = addRenderableWidget(button(leftPos + 34, topPos + 70, 192, "screen.economia.bank_counter.mint", this::mintGold));
        nuggetButton = addRenderableWidget(button(leftPos + 96, topPos + 160, 48, "screen.economia.bank_counter.nugget", () -> redeem(GoldRedeemUnit.NUGGET)));
        ingotButton = addRenderableWidget(button(leftPos + 150, topPos + 160, 48, "screen.economia.bank_counter.ingot", () -> redeem(GoldRedeemUnit.INGOT)));
        blockButton = addRenderableWidget(button(leftPos + 204, topPos + 160, 48, "screen.economia.bank_counter.block", () -> redeem(GoldRedeemUnit.BLOCK)));
        updateButtons();
        refreshPrices();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
        priceRefreshTicks++;
        if (priceRefreshTicks >= PRICE_REFRESH_TICKS) {
            priceRefreshTicks = 0;
            refreshPrices();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF252728);
        graphics.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + 28, 0xFF9C7A2F);
        graphics.fill(leftPos + 22, topPos + 38, leftPos + imageWidth - 22, topPos + 190, 0xFF20262A);
        drawSlotFrame(graphics, 34, 50);
        for (int slot = 0; slot < 9; slot++) {
            drawSlotFrame(graphics, 72 + slot * 18, 50);
        }
        drawInventoryFrames(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 10, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.card_slot"), 34, 38, 0xF6E7B0, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.gold_section"), 72, 38, 0xF6E7B0, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.estimated_payment", money(menu.estimatedMintPayment())), 34, 96, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.buy_values"), 34, 108, 0xF6E7B0, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.nugget_value", money(menu.goldBuyNuggetValue())), 34, 120, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.ingot_value", money(menu.goldBuyIngotValue())), 34, 132, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.block_value", money(menu.goldBuyBlockValue())), 150, 120, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.sell_percent", menu.goldSellPercent()), 150, 132, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.buy_percent", menu.goldBuyPercent()), 34, 144, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.bank_counter.redeem"), 34, 150, 0xF6E7B0, false);
        graphics.drawString(font, Component.translatable(menu.hasCard() ? "screen.economia.bank_counter.card_ready" : "screen.economia.bank_counter.card_required"), 34, 182, menu.hasCard() ? 0x80E6A8 : 0xE68E8E, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Button button(int x, int y, int width, String key, Runnable action) {
        return Button.builder(Component.translatable(key), ignored -> action.run())
                .bounds(x, y, width, 20)
                .build();
    }

    private void mintGold() {
        PacketDistributor.sendToServer(new BankCounterActionPayload(BankCounterActionPayload.ACTION_MINT, 0, 0, UUID.randomUUID()));
        refreshPrices();
    }

    private void redeem(GoldRedeemUnit unit) {
        PacketDistributor.sendToServer(new BankCounterActionPayload(BankCounterActionPayload.ACTION_REDEEM, Math.toIntExact(Math.min(Integer.MAX_VALUE, amount())), unit.id, UUID.randomUUID()));
        refreshPrices();
        updateButtons();
    }

    private void refreshPrices() {
        PacketDistributor.sendToServer(new BankCounterActionPayload(BankCounterActionPayload.ACTION_REFRESH_PRICES, 0, 0, UUID.randomUUID()));
    }

    private void updateButtons() {
        if (nuggetButton == null) {
            return;
        }
        boolean hasCard = menu.hasCard();
        boolean hasGold = menu.goldInventoryNuggetUnits() > 0L;
        long amount = amount();
        mintButton.active = hasGold;
        nuggetButton.active = hasCard && canRedeem(amount, GoldRedeemUnit.NUGGET);
        ingotButton.active = hasCard && canRedeem(amount, GoldRedeemUnit.INGOT);
        blockButton.active = hasCard && canRedeem(amount, GoldRedeemUnit.BLOCK);
    }

    private boolean canRedeem(long amount, GoldRedeemUnit unit) {
        return amount > 0L;
    }

    private String money(long value) {
        if (value == Long.MAX_VALUE) {
            return "max";
        }
        if (value >= 1_000_000_000L) {
            return (value / 1_000_000_000L) + "B+";
        }
        if (value >= 1_000_000L) {
            return (value / 1_000_000L) + "M+";
        }
        return Long.toString(value);
    }

    private long amount() {
        try {
            return Math.max(0L, Long.parseLong(redeemAmount.getValue()));
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private void drawInventoryFrames(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotFrame(graphics, 49 + column * 18, 204 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawSlotFrame(graphics, 49 + column * 18, 262);
        }
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF111416);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF2B3337);
    }

    private enum GoldRedeemUnit {
        NUGGET(BankCounterActionPayload.UNIT_NUGGET),
        INGOT(BankCounterActionPayload.UNIT_INGOT),
        BLOCK(BankCounterActionPayload.UNIT_BLOCK);

        private final int id;

        GoldRedeemUnit(int id) {
            this.id = id;
        }
    }
}
