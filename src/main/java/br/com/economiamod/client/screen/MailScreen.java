package br.com.economiamod.client.screen;

import br.com.economiamod.common.menu.MailMenu;
import br.com.economiamod.common.network.MailAction;
import br.com.economiamod.common.network.MailActionPayload;
import br.com.economiamod.common.network.MailStatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MailScreen extends AbstractContainerScreen<MailMenu> implements MailStateAwareScreen {
    private static final int ROWS = 3;

    private EditBox nameInput;
    private EditBox searchInput;
    private Button confirmNameButton;
    private Button cancelNameButton;
    private Button addButton;
    private Button changeRecipientButton;
    private Button paymentButton;
    private Button payCashButton;
    private Button payDebitButton;
    private Button payCreditButton;
    private Button closePaymentButton;
    private Button warningBackButton;
    private Button warningConfirmButton;
    private final List<Button> rowSendButtons = new ArrayList<>();
    private final List<Button> rowDeleteButtons = new ArrayList<>();
    private boolean setupCancelled;
    private boolean sendMode;
    private boolean paymentModal;
    private boolean closePaymentOnNextState;

    public MailScreen(MailMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 320;
        imageHeight = 244;
        inventoryLabelY = 146;
    }

    @Override
    protected void init() {
        super.init();
        nameInput = addRenderableWidget(new EditBox(font, leftPos + 28, topPos + 64, 180, 18, Component.translatable("screen.economia.mail.name")));
        nameInput.setMaxLength(64);
        nameInput.setHint(Component.translatable("screen.economia.mail.name"));
        searchInput = addRenderableWidget(new EditBox(font, leftPos + 18, topPos + 42, 132, 18, Component.translatable("screen.economia.mail.search")));
        searchInput.setMaxLength(64);
        searchInput.setHint(Component.translatable("screen.economia.mail.search"));

        confirmNameButton = addRenderableWidget(button(leftPos + 218, topPos + 62, 78, "screen.economia.mail.confirm", this::confirmName));
        cancelNameButton = addRenderableWidget(button(leftPos + 218, topPos + 86, 78, "screen.economia.mail.cancel", () -> setupCancelled = true));
        addButton = addRenderableWidget(button(leftPos + 158, topPos + 40, 74, "screen.economia.mail.add", this::addRecipient));
        changeRecipientButton = addRenderableWidget(button(leftPos + 196, topPos + 40, 96, "screen.economia.mail.change_recipient", this::showRecipients));
        paymentButton = addRenderableWidget(button(leftPos + 196, topPos + 64, 96, "screen.economia.mail.payment", this::openPayment));
        payCashButton = addRenderableWidget(button(leftPos + 238, topPos + 50, 70, "screen.economia.mail.cash", () -> pay(MailAction.PAY_CASH, false)));
        payDebitButton = addRenderableWidget(button(leftPos + 238, topPos + 74, 70, "screen.economia.mail.debit", () -> pay(MailAction.PAY_CARD, false)));
        payCreditButton = addRenderableWidget(button(leftPos + 238, topPos + 98, 70, "screen.economia.mail.credit", () -> pay(MailAction.PAY_CARD, true)));
        closePaymentButton = addRenderableWidget(button(leftPos + 238, topPos + 122, 70, "screen.economia.mail.back", this::closePayment));
        warningBackButton = addRenderableWidget(button(leftPos + 72, topPos + 136, 76, "screen.economia.mail.back", this::returnToPayment));
        warningConfirmButton = addRenderableWidget(button(leftPos + 172, topPos + 136, 86, "screen.economia.mail.confirm", () -> pay(MailAction.CONFIRM_CHANGE_TO_OWNER, false)));

        for (int row = 0; row < ROWS; row++) {
            int index = row;
            rowSendButtons.add(addRenderableWidget(button(leftPos + 214, topPos + 72 + row * 14, 46, "screen.economia.mail.ship", () -> selectRecipient(index))));
            rowDeleteButtons.add(addRenderableWidget(button(leftPos + 266, topPos + 72 + row * 14, 38, "screen.economia.mail.delete", () -> deleteRecipient(index))));
        }

        PacketDistributor.sendToServer(new MailActionPayload(MailAction.REFRESH, ""));
        updateControls();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateControls();
    }

    @Override
    public void applyMailState(MailStatePayload payload) {
        menu.applyState(payload);
        if (payload.changeWarning()) {
            paymentModal = false;
            closePaymentOnNextState = false;
        } else if (closePaymentOnNextState) {
            paymentModal = false;
            closePaymentOnNextState = false;
        }
        updateControls();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF171B1D);
        graphics.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + 28, 0xFF2F4B5B);
        graphics.fill(leftPos + 10, topPos + 34, leftPos + imageWidth - 10, topPos + 146, 0xFF20262A);
        if (menu.changeWarning()) {
            graphics.fill(leftPos + 48, topPos + 44, leftPos + 304, topPos + 202, 0xEE111416);
            graphics.fill(leftPos + 56, topPos + 52, leftPos + 296, topPos + 194, 0xFF20262A);
            return;
        }
        if (paymentModal) {
            drawSlotFrame(graphics, 198, 76);
            for (int slot = 0; slot < 6; slot++) {
                drawSlotFrame(graphics, 132 + (slot % 3) * 18, 104 + (slot / 3) * 18);
            }
            drawInventoryFrames(graphics);
            return;
        }
        if (sendMode && menu.selectedRecipient() != null) {
            drawSlotGrid(graphics, 22, 76, 2);
        } else if (menu.named()) {
            drawSlotGrid(graphics, 22, 108, 2);
        }
        if (menu.named()) {
            drawInventoryFrames(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 12, 0xFFFFFF, false);
        if (setupVisible()) {
            graphics.drawString(font, Component.translatable("screen.economia.mail.initial_name"), 28, 48, 0xD7E8E1, false);
            return;
        }
        if (!menu.named()) {
            graphics.drawString(font, Component.translatable("screen.economia.mail.waiting_name"), 18, 46, 0xD7E8E1, false);
            return;
        }
        if (menu.changeWarning()) {
            renderChangeWarning(graphics);
            return;
        }
        if (paymentModal) {
            renderPaymentModal(graphics);
            return;
        }
        if (sendMode && menu.selectedRecipient() != null) {
            graphics.drawString(font, Component.translatable("screen.economia.mail.to", trim(menu.selectedRecipient().ownerName()), trim(menu.selectedRecipient().mailName())), 18, 40, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable("screen.economia.mail.total", menu.shipmentTotal()), 196, 92, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable("screen.economia.mail.send_slots"), 22, 66, 0xB8C7C2, false);
            return;
        }
        graphics.drawString(font, Component.translatable("screen.economia.mail.recipients"), 18, 64, 0xB8C7C2, false);
        graphics.drawString(font, Component.translatable("screen.economia.mail.received_slots"), 22, 98, 0xB8C7C2, false);
        int rows = Math.min(ROWS, menu.recipients().size());
        for (int row = 0; row < rows; row++) {
            MailStatePayload.RecipientSummary recipient = menu.recipients().get(row);
            int y = 74 + row * 14;
            graphics.drawString(font, trim(recipient.ownerName()), 18, y, 0xD7E8E1, false);
            graphics.drawString(font, trim(recipient.mailName()), 112, y, 0xD7E8E1, false);
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
        if ((isFocused(nameInput) || isFocused(searchInput)) && minecraft != null && minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return false;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (slot.isActive()) {
            super.renderSlot(graphics, slot);
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot == null || slot.isActive()) {
            super.slotClicked(slot, slotId, mouseButton, type);
        }
    }

    private void renderPaymentModal(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("screen.economia.mail.payment"), 22, 52, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.economia.mail.total", menu.shipmentTotal()), 22, 84, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.mail.card_slot"), 196, 66, 0xB8C7C2, false);
        graphics.drawString(font, Component.translatable("screen.economia.mail.cash_slots"), 132, 94, 0xB8C7C2, false);
    }

    private void renderChangeWarning(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("screen.economia.mail.change_warning_title"), 66, 64, 0xFFFFFF, false);
        graphics.drawWordWrap(font, Component.translatable("screen.economia.mail.change_warning"), 66, 84, 220, 0xD7E8E1);
    }

    private void updateControls() {
        boolean setup = setupVisible();
        boolean main = menu.named() && !sendMode && !paymentModal && !menu.changeWarning();
        boolean send = menu.named() && sendMode && menu.selectedRecipient() != null && !paymentModal && !menu.changeWarning();
        nameInput.visible = setup;
        nameInput.active = setup;
        confirmNameButton.visible = setup;
        cancelNameButton.visible = setup;
        searchInput.visible = main;
        searchInput.active = main;
        addButton.visible = main;
        addButton.active = main;
        changeRecipientButton.visible = send;
        paymentButton.visible = send;
        payCashButton.visible = paymentModal;
        payDebitButton.visible = paymentModal;
        payCreditButton.visible = paymentModal;
        closePaymentButton.visible = paymentModal;
        warningBackButton.visible = menu.changeWarning();
        warningConfirmButton.visible = menu.changeWarning();
        for (int row = 0; row < ROWS; row++) {
            boolean rowVisible = main && row < menu.recipients().size();
            rowSendButtons.get(row).visible = rowVisible;
            rowDeleteButtons.get(row).visible = rowVisible && menu.ownerMode();
        }
        menu.setClientSlotVisibility(
                send && menu.selectedRecipient() != null,
                main,
                paymentModal,
                menu.named() && !setup && !menu.changeWarning()
        );
    }

    private boolean setupVisible() {
        return menu.ownerMode() && !menu.named() && !setupCancelled;
    }

    private void confirmName() {
        PacketDistributor.sendToServer(new MailActionPayload(MailAction.SET_NAME, nameInput.getValue()));
    }

    private void addRecipient() {
        PacketDistributor.sendToServer(new MailActionPayload(MailAction.ADD_RECIPIENT, searchInput.getValue()));
    }

    private void selectRecipient(int index) {
        if (index >= menu.recipients().size()) {
            return;
        }
        UUID id = menu.recipients().get(index).destinationBlockId();
        PacketDistributor.sendToServer(new MailActionPayload(MailAction.SELECT_RECIPIENT, id));
        menu.applyState(new MailStatePayload(menu.named(), menu.mailName(), id, false, false, menu.recipients()));
        sendMode = true;
    }

    private void deleteRecipient(int index) {
        if (index < menu.recipients().size()) {
            PacketDistributor.sendToServer(new MailActionPayload(MailAction.DELETE_RECIPIENT, menu.recipients().get(index).destinationBlockId()));
        }
    }

    private void showRecipients() {
        sendMode = false;
        paymentModal = false;
        closePaymentOnNextState = false;
        PacketDistributor.sendToServer(new MailActionPayload(MailAction.CLOSE_PAYMENT, ""));
        PacketDistributor.sendToServer(new MailActionPayload(MailAction.SELECT_RECIPIENT, new UUID(0L, 0L)));
    }

    private void openPayment() {
        paymentModal = true;
        closePaymentOnNextState = false;
        PacketDistributor.sendToServer(new MailActionPayload(MailAction.OPEN_PAYMENT, ""));
    }

    private void closePayment() {
        paymentModal = false;
        closePaymentOnNextState = false;
        PacketDistributor.sendToServer(new MailActionPayload(MailAction.CLOSE_PAYMENT, ""));
    }

    private void returnToPayment() {
        paymentModal = true;
        closePaymentOnNextState = false;
        PacketDistributor.sendToServer(new MailActionPayload(MailAction.RETURN_TO_PAYMENT, ""));
    }

    private void pay(MailAction action, boolean creditPayment) {
        closePaymentOnNextState = true;
        PacketDistributor.sendToServer(new MailActionPayload(action, creditPayment));
    }

    private Button button(int x, int y, int width, String key, Runnable action) {
        return Button.builder(Component.translatable(key), ignored -> action.run())
                .bounds(x, y, width, 20)
                .build();
    }

    private boolean isFocused(EditBox box) {
        return box != null && box.visible && box.isFocused();
    }

    private Component trim(String value) {
        String text = value == null ? "-" : value;
        return Component.literal(text.length() > 14 ? text.substring(0, 13) + "." : text);
    }

    private void drawInventoryFrames(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotFrame(graphics, 22 + column * 18, 156 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlotFrame(graphics, 22 + column * 18, 214);
        }
    }

    private void drawSlotGrid(GuiGraphics graphics, int x, int y, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotFrame(graphics, x + column * 18, y + row * 18);
            }
        }
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF111416);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF2B3337);
    }
}
