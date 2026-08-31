package br.com.economiamod.client.screen;

import br.com.economiamod.common.menu.AtmMenu;
import br.com.economiamod.common.menu.AtmMenu.CardSlotMode;
import br.com.economiamod.common.menu.AtmMenu.PlayerInventoryMode;
import br.com.economiamod.common.card.CardType;
import br.com.economiamod.common.network.AtmAccountSummaryPayload;
import br.com.economiamod.common.network.AtmCardsPayload;
import br.com.economiamod.common.network.AtmOperationHistoryPayload;
import br.com.economiamod.common.network.SecureAccountAction;
import br.com.economiamod.common.network.SecureAccountPayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AtmScreen extends AbstractContainerScreen<AtmMenu> implements AccountSummaryAwareScreen, AtmCardsAwareScreen, AtmOperationHistoryAwareScreen {
    private static final int GOLD_PRICE_REFRESH_TICKS = 200;
    private static final int BALANCE_REFRESH_TICKS = 40;
    private static final int CARD_LIST_VISIBLE_ROWS = 5;
    private static final int ACCOUNT_MASK_DELAY_TICKS = 40;
    private static final int CREDIT_REQUEST_COOLDOWN_TICKS = 200;
    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_NUMPAD_ENTER = 335;

    private AtmView view = AtmView.LOGIN;
    private PendingSensitiveAction pendingSensitiveAction = PendingSensitiveAction.NONE;
    private boolean loggedIn;
    private String sessionUsername = "";
    private String sessionAccountNumber = "";
    private boolean showSessionUsername;
    private long balance;
    private long availableBalance;
    private long configuredCreditLimit;
    private long creditDebt;
    private long creditAvailable;
    private List<AtmCardsPayload.CardSummary> atmCards = List.of();
    private List<AtmOperationHistoryPayload.Entry> operationHistory = List.of();
    private int cardListOffset;
    private int selectedCardIndex = -1;

    private Button loginTab;
    private Button createTab;
    private Button recoverTab;
    private Button cashTab;
    private Button cardsTab;
    private Button creditTab;
    private Button transferTab;
    private Button historyTab;
    private Button securityTab;
    private Button goldInfoTab;

    private EditBox username;
    private EditBox password;
    private EditBox newPassword;
    private EditBox securityPassword;
    private EditBox securityNewPassword;
    private EditBox withdrawAmount;
    private EditBox withdrawDenomination;
    private EditBox accountCreditLimit;
    private EditBox cardCreditLimit;
    private EditBox debitDailyLimit;
    private EditBox transferAccountNumber;
    private EditBox transferAmount;
    private EditBox modalPassword;

    private Button passwordLoginButton;
    private Button cardLoginButton;
    private Button createAccountButton;
    private Button recoverPasswordButton;
    private Button withdrawButton;
    private Button depositButton;
    private Button balanceButton;
    private Button debitCardButton;
    private Button creditCardButton;
    private Button comboCardButton;
    private Button updateCreditButton;
    private Button updateAccountCreditButton;
    private Button updateDebitDailyLimitButton;
    private Button payInvoiceButton;
    private Button issueInvoiceButton;
    private Button payAllInvoicesButton;
    private Button requestCreditButton;
    private Button cardListUpButton;
    private Button cardListDownButton;
    private Button blockListedCardButton;
    private Button disableListedCardButton;
    private Button transferButton;
    private Button changePasswordButton;
    private Button unblockCardButton;
    private Button webLoginTokenButton;
    private Button logoutButton;
    private Button modalConfirmButton;
    private Button modalCancelButton;
    private int goldPriceRefreshTicks;
    private int balanceRefreshTicks;
    private int accountMaskTicks;
    private int creditRequestCooldownTicks;
    private boolean applyingAccountMask;

    public AtmScreen(AtmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 352;
        imageHeight = 252;
        inventoryLabelY = 158;
    }

    @Override
    protected void init() {
        super.init();
        createNavigation();
        createFields();
        createButtons();
        selectView(loggedIn ? AtmView.CASH : AtmView.LOGIN);
        requestSessionState();
        requestAccountSummary();
        requestCards();
        requestOperationHistory();
    }

    public void applySessionState(boolean loggedIn, String username, String accountNumber, boolean showUsername) {
        this.loggedIn = loggedIn;
        this.sessionUsername = username == null ? "" : username;
        this.sessionAccountNumber = accountNumber == null ? "" : accountNumber;
        this.showSessionUsername = showUsername;
        menu.setSessionAccountNumber(this.sessionAccountNumber);
        if (loginTab != null) {
            selectView(loggedIn ? AtmView.CASH : AtmView.LOGIN);
        }
    }

    @Override
    public void applyAccountSummary(AtmAccountSummaryPayload payload) {
        if (!payload.available()) {
            balance = 0L;
            availableBalance = 0L;
            configuredCreditLimit = 0L;
            creditDebt = 0L;
            creditAvailable = 0L;
            return;
        }
        balance = payload.balance();
        availableBalance = payload.availableBalance();
        configuredCreditLimit = payload.configuredCreditLimit();
        creditDebt = payload.creditDebt();
        creditAvailable = payload.creditAvailable();
        if (accountCreditLimit != null && !accountCreditLimit.isFocused()) {
            accountCreditLimit.setValue(Long.toString(configuredCreditLimit));
        }
    }

    @Override
    public void applyAtmCards(AtmCardsPayload payload) {
        atmCards = payload.cards();
        int maxOffset = maxCardListOffset();
        cardListOffset = Math.max(0, Math.min(cardListOffset, maxOffset));
        if (atmCards.isEmpty()) {
            selectedCardIndex = -1;
        } else if (selectedCardIndex < 0 || selectedCardIndex >= atmCards.size()) {
            selectedCardIndex = cardListOffset;
        }
        updateCardListButtons();
    }

    @Override
    public void applyOperationHistory(AtmOperationHistoryPayload payload) {
        operationHistory = payload.entries();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF161A1D);
        graphics.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + 28, 0xFF225E54);
        int contentBottom = usesHotbarOnly() ? 220 : 164;
        graphics.fill(leftPos + 10, topPos + 56, leftPos + imageWidth - 10, topPos + contentBottom, 0xFF20262A);
        if (showsMenuInventory()) {
            if (view == AtmView.CREATE) {
                for (int slot = 0; slot < 6; slot++) {
                    drawSlotFrame(graphics, 190 + (slot % 3) * 18, 96 + (slot / 3) * 18);
                }
            }
            if (showsCardAndInventory()) {
            drawSlotFrame(graphics, 290, 84);
            if (view == AtmView.CREDIT) {
                drawSlotFrame(graphics, 244, 118);
            }
            }
            drawInventoryFrames(graphics, inventoryModeForView());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (passwordModalOpen()) {
            return;
        }
        graphics.drawString(font, title, 10, 11, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable(view.titleKey), 12, 58, 0xD7E8E1, false);
        if (loggedIn && !sessionAccountNumber.isBlank()) {
            drawRight(graphics, Component.translatable("screen.economia.atm.logged_account", sessionAccountNumber), imageWidth - 10, 8, 0x8EDBC8);
            if (showSessionUsername && !sessionUsername.isBlank()) {
                drawRight(graphics, Component.translatable("screen.economia.atm.logged_user", sessionUsername), imageWidth - 10, 18, 0x8EDBC8);
            }
        }
        if (view == AtmView.CASH) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.balance_value", balance, availableBalance), 14, 78, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.credit_available", creditAvailable), 14, 90, 0xD7E8E1, false);
        }
        if (view == AtmView.CARDS) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.card_type_header"), 20, 80, 0xB8C7C2, false);
            renderCardIssueValue(graphics, 20, 116, 74);
            renderCardIssueValue(graphics, 106, 116, 74);
            renderCardIssueValue(graphics, 192, 116, 74);
            graphics.drawString(font, Component.translatable("screen.economia.atm.card_slot"), 282, 72, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.debit_daily_limit"), 20, 126, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.card_list"), 20, 158, 0xB8C7C2, false);
            renderCardList(graphics);
        }
        if (view == AtmView.CREDIT) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.account_credit_limit_value", configuredCreditLimit), 14, 78, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.credit_available", creditAvailable), 14, 90, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.credit_debt", creditDebt), 14, 102, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.card_credit_limit"), 134, 104, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.card_slot"), 282, 72, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.invoice_value", creditDebt), 244, 106, 0xD7E8E1, false);
        }
        if (view == AtmView.TRANSFER) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.balance_value", balance, availableBalance), 14, 78, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.account_number"), 20, 92, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.transfer_amount"), 126, 92, 0xB8C7C2, false);
        }
        if (view == AtmView.SECURITY) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.current_password"), 24, 82, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.new_password"), 24, 112, 0xB8C7C2, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.password_rule"), 186, 132, 0x8EA09A, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.card_slot"), 282, 72, 0xB8C7C2, false);
        }
        if (view == AtmView.CREATE || view == AtmView.RECOVER) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.password_rule"), 20, 148, 0x8EA09A, false);
        }
        if (view == AtmView.CREATE) {
            graphics.drawString(font, Component.translatable(
                    "screen.economia.atm.account_opening_fee", menu.accountOpeningFee()), 190, 78, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable("screen.economia.atm.account_opening_cash"),
                    190, 88, 0xB8C7C2, false);
        }
        if (view == AtmView.GOLD_INFO) {
            renderGoldInfo(graphics);
        }
        if (view == AtmView.HISTORY) {
            renderOperationHistory(graphics);
        }
        if (!loggedIn && view == AtmView.LOGIN) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.card_slot"), 282, 72, 0xB8C7C2, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPasswordModal(graphics, mouseX, mouseY, partialTick);
        if (!passwordModalOpen()) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (passwordModalOpen()) {
            if (keyCode == KEY_ESCAPE) {
                closePasswordModal();
                return true;
            }
            if (keyCode == KEY_ENTER || keyCode == KEY_NUMPAD_ENTER) {
                confirmPasswordModal();
                return true;
            }
        }
        if (isTextInputFocused() && minecraft != null && minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickTransferAccountMask();
        tickCreditRequestCooldown();
        tickBalanceRefresh();
        tickGoldPriceRefresh();
    }

    private void tickGoldPriceRefresh() {
        if (view != AtmView.GOLD_INFO) {
            goldPriceRefreshTicks = 0;
            return;
        }
        goldPriceRefreshTicks++;
        if (goldPriceRefreshTicks >= GOLD_PRICE_REFRESH_TICKS) {
            goldPriceRefreshTicks = 0;
            requestGoldPrices();
        }
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!passwordModalOpen() && showsMenuInventory() && slot.isActive()) {
            super.renderSlot(graphics, slot);
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (passwordModalOpen()) {
            return;
        }
        if (showsMenuInventory() && (slot == null || slot.isActive())) {
            super.slotClicked(slot, slotId, mouseButton, type);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (view == AtmView.CARDS && mouseX >= leftPos + 20 && mouseX <= leftPos + 334 && mouseY >= topPos + 158 && mouseY <= topPos + 220) {
            scrollCardList(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (view == AtmView.CARDS && mouseX >= leftPos + 20 && mouseX <= leftPos + 334 && mouseY >= topPos + 170 && mouseY < topPos + 170 + CARD_LIST_VISIBLE_ROWS * 9) {
            int row = ((int) mouseY - topPos - 170) / 9;
            int index = cardListOffset + row;
            if (index >= 0 && index < atmCards.size()) {
                selectedCardIndex = index;
                updateCardListButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void createNavigation() {
        loginTab = addRenderableWidget(tabButton(leftPos + 14, topPos + 32, 80, "screen.economia.atm.login", AtmView.LOGIN));
        createTab = addRenderableWidget(tabButton(leftPos + 100, topPos + 32, 80, "screen.economia.atm.create_short", AtmView.CREATE));
        recoverTab = addRenderableWidget(tabButton(leftPos + 186, topPos + 32, 80, "screen.economia.atm.recover_short", AtmView.RECOVER));
        cashTab = addRenderableWidget(tabButton(leftPos + 14, topPos + 32, 48, "screen.economia.atm.tab.cash", AtmView.CASH));
        cardsTab = addRenderableWidget(tabButton(leftPos + 66, topPos + 32, 50, "screen.economia.atm.tab.cards", AtmView.CARDS));
        creditTab = addRenderableWidget(tabButton(leftPos + 120, topPos + 32, 50, "screen.economia.atm.tab.credit", AtmView.CREDIT));
        transferTab = addRenderableWidget(tabButton(leftPos + 174, topPos + 32, 62, "screen.economia.atm.tab.transfer", AtmView.TRANSFER));
        historyTab = addRenderableWidget(tabButton(leftPos + 240, topPos + 32, 42, "screen.economia.atm.tab.history", AtmView.HISTORY));
        securityTab = addRenderableWidget(tabButton(leftPos + 286, topPos + 32, 36, "screen.economia.atm.tab.security", AtmView.SECURITY));
        goldInfoTab = addRenderableWidget(tabButton(leftPos + 326, topPos + 32, 18, "screen.economia.atm.tab.gold", AtmView.GOLD_INFO));
    }

    private void createFields() {
        username = addRenderableWidget(textBox(leftPos + 20, topPos + 82, 150, "screen.economia.atm.username"));
        password = addRenderableWidget(passwordBox(leftPos + 20, topPos + 104, 150, "screen.economia.atm.password"));
        newPassword = addRenderableWidget(passwordBox(leftPos + 20, topPos + 104, 150, "screen.economia.atm.new_password"));
        securityPassword = addRenderableWidget(passwordBox(leftPos + 24, topPos + 94, 150, "screen.economia.atm.current_password"));
        securityNewPassword = addRenderableWidget(passwordBox(leftPos + 24, topPos + 124, 150, "screen.economia.atm.new_password"));
        withdrawAmount = addRenderableWidget(textBox(leftPos + 20, topPos + 104, 76, "screen.economia.atm.withdraw_amount"));
        withdrawDenomination = addRenderableWidget(textBox(leftPos + 104, topPos + 104, 48, "screen.economia.atm.withdraw_denomination"));
        withdrawDenomination.setMaxLength(3);
        withdrawDenomination.setFilter(value -> value.matches("[0-9]*"));
        accountCreditLimit = addRenderableWidget(textBox(leftPos + 24, topPos + 130, 96, "screen.economia.atm.account_credit_limit"));
        cardCreditLimit = addRenderableWidget(textBox(leftPos + 134, topPos + 116, 92, "screen.economia.atm.card_credit_limit"));
        debitDailyLimit = addRenderableWidget(textBox(leftPos + 20, topPos + 138, 82, "screen.economia.atm.debit_daily_limit"));
        debitDailyLimit.setFilter(value -> value.matches("[0-9]*"));
        transferAccountNumber = addRenderableWidget(textBox(leftPos + 20, topPos + 104, 90, "screen.economia.atm.account_number"));
        transferAccountNumber.setMaxLength(6);
        transferAccountNumber.setFilter(value -> value.matches("[0-9]*"));
        transferAccountNumber.setResponder(this::onTransferAccountChanged);
        transferAmount = addRenderableWidget(textBox(leftPos + 126, topPos + 104, 62, "screen.economia.atm.transfer_amount"));
        modalPassword = addRenderableWidget(passwordBox(leftPos + 94, topPos + 116, 164, "screen.economia.atm.password"));
    }

    private void createButtons() {
        passwordLoginButton = addRenderableWidget(button(leftPos + 20, topPos + 126, 150, "screen.economia.atm.login_password", () -> secure(SecureAccountAction.LOGIN)));
        cardLoginButton = addRenderableWidget(button(leftPos + 198, topPos + 110, 68, "screen.economia.atm.login_card", () -> secure(SecureAccountAction.CARD_LOGIN)));
        createAccountButton = addRenderableWidget(button(leftPos + 20, topPos + 126, 150, "screen.economia.atm.create_account", () -> secure(SecureAccountAction.CREATE_ACCOUNT)));
        recoverPasswordButton = addRenderableWidget(button(leftPos + 20, topPos + 126, 150, "screen.economia.atm.recover_password", () -> secure(SecureAccountAction.RECOVER_PASSWORD)));

        withdrawButton = addRenderableWidget(button(leftPos + 160, topPos + 104, 62, "screen.economia.atm.withdraw", () -> openPasswordModal(PendingSensitiveAction.WITHDRAW)));
        depositButton = addRenderableWidget(button(leftPos + 230, topPos + 104, 72, "screen.economia.atm.deposit", this::deposit));
        balanceButton = addRenderableWidget(button(leftPos + 20, topPos + 126, 90, "screen.economia.atm.balance", this::requestAccountSummary));

        debitCardButton = addRenderableWidget(button(leftPos + 20, topPos + 94, 74, "screen.economia.atm.debit_card", () -> issueCard(CardType.DEBIT)));
        creditCardButton = addRenderableWidget(button(leftPos + 106, topPos + 94, 74, "screen.economia.atm.credit_card", () -> issueCard(CardType.CREDIT)));
        comboCardButton = addRenderableWidget(button(leftPos + 192, topPos + 94, 74, "screen.economia.atm.combo_card", () -> issueCard(CardType.DEBIT_CREDIT)));
        updateAccountCreditButton = addRenderableWidget(button(leftPos + 24, topPos + 150, 96, "screen.economia.atm.update_credit", () -> updateAccountCredit()));
        updateCreditButton = addRenderableWidget(button(leftPos + 134, topPos + 138, 92, "screen.economia.atm.update_credit", () -> secure(SecureAccountAction.UPDATE_CARD_CREDIT)));
        updateDebitDailyLimitButton = addRenderableWidget(button(leftPos + 108, topPos + 138, 56, "screen.economia.atm.apply", () -> updateDebitDailyLimit()));
        payInvoiceButton = addRenderableWidget(button(leftPos + 266, topPos + 118, 72, "screen.economia.atm.pay_invoice", () -> openPasswordModal(PendingSensitiveAction.PAY_INVOICE)));
        issueInvoiceButton = addRenderableWidget(button(leftPos + 244, topPos + 142, 44, "screen.economia.atm.issue_invoice", this::issueInvoice));
        payAllInvoicesButton = addRenderableWidget(button(leftPos + 292, topPos + 142, 46, "screen.economia.atm.pay_all_invoices", () -> openPasswordModal(PendingSensitiveAction.PAY_ALL_INVOICES)));
        requestCreditButton = addRenderableWidget(button(leftPos + 24, topPos + 130, 96, "screen.economia.atm.request_credit", this::requestAccountCredit));
        cardListUpButton = addRenderableWidget(button(leftPos + 170, topPos + 138, 22, "screen.economia.atm.card_list_up", () -> scrollCardList(-1)));
        cardListDownButton = addRenderableWidget(button(leftPos + 196, topPos + 138, 22, "screen.economia.atm.card_list_down", () -> scrollCardList(1)));
        blockListedCardButton = addRenderableWidget(button(leftPos + 222, topPos + 138, 54, "screen.economia.atm.block_card", () -> openPasswordModal(PendingSensitiveAction.BLOCK_CARD)));
        disableListedCardButton = addRenderableWidget(button(leftPos + 280, topPos + 138, 54, "screen.economia.atm.delete_card", () -> openPasswordModal(PendingSensitiveAction.DISABLE_CARD)));
        transferButton = addRenderableWidget(button(leftPos + 204, topPos + 104, 82, "screen.economia.atm.transfer", () -> openPasswordModal(PendingSensitiveAction.TRANSFER)));
        changePasswordButton = addRenderableWidget(button(leftPos + 24, topPos + 144, 150, "screen.economia.atm.change_password", () -> secure(SecureAccountAction.CHANGE_PASSWORD)));
        unblockCardButton = addRenderableWidget(button(leftPos + 266, topPos + 112, 70, "screen.economia.atm.unblock_card", () -> secure(SecureAccountAction.UNBLOCK_CARD)));
        webLoginTokenButton = addRenderableWidget(button(leftPos + 266, topPos + 144, 70, "screen.economia.atm.web_token", () -> secure(SecureAccountAction.WEB_LOGIN_TOKEN)));
        logoutButton = addRenderableWidget(button(leftPos + 186, topPos + 144, 70, "screen.economia.atm.logout", this::logout));
        modalConfirmButton = addRenderableWidget(button(leftPos + 94, topPos + 140, 78, "screen.economia.atm.confirm", this::confirmPasswordModal));
        modalCancelButton = addRenderableWidget(button(leftPos + 180, topPos + 140, 78, "screen.economia.atm.cancel", this::closePasswordModal));
    }

    private Button button(int x, int y, int width, String key, Runnable action) {
        return Button.builder(Component.translatable(key), ignored -> action.run())
                .bounds(x, y, width, 20)
                .build();
    }

    private Button tabButton(int x, int y, int width, String key, AtmView targetView) {
        return Button.builder(Component.translatable(key), ignored -> selectView(targetView))
                .bounds(x, y, width, 20)
                .build();
    }

    private EditBox textBox(int x, int y, int width, String key) {
        EditBox box = new EditBox(font, x, y, width, 18, Component.translatable(key));
        box.setMaxLength(64);
        box.setHint(Component.translatable(key));
        return box;
    }

    private EditBox passwordBox(int x, int y, int width, String key) {
        EditBox box = textBox(x, y, width, key);
        box.setMaxLength(64);
        box.setFormatter((text, offset) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        return box;
    }

    private void secure(SecureAccountAction action) {
        String payloadUsername = username.getValue();
        String payloadPassword = password.getValue();
        String payloadNewPassword = newPassword.getValue();
        if (action == SecureAccountAction.UPDATE_CARD_CREDIT) {
            payloadNewPassword = cardCreditLimit.getValue();
        } else if (action == SecureAccountAction.CHANGE_PASSWORD && view == AtmView.SECURITY) {
            payloadPassword = securityPassword.getValue();
            payloadNewPassword = securityNewPassword.getValue();
        }

        PacketDistributor.sendToServer(new SecureAccountPayload(
                action,
                payloadUsername,
                payloadPassword,
                payloadNewPassword
        ));

        password.setValue("");
        newPassword.setValue("");
        if (action == SecureAccountAction.CHANGE_PASSWORD) {
            securityPassword.setValue("");
            securityNewPassword.setValue("");
        }
        if (action == SecureAccountAction.UPDATE_CARD_CREDIT) {
            requestAccountSummary();
        }
    }

    private void transfer(String accountPassword) {
        PacketDistributor.sendToServer(new SecureAccountPayload(
                SecureAccountAction.TRANSFER,
                transferAccountNumber.getValue(),
                transferAmount.getValue(),
                accountPassword
        ));
        requestAccountSummary();
    }

    private void withdraw(String accountPassword) {
        PacketDistributor.sendToServer(new SecureAccountPayload(
                SecureAccountAction.WITHDRAW,
                withdrawAmount.getValue(),
                accountPassword,
                withdrawDenomination.getValue()
        ));
        requestAccountSummary();
    }

    private void deposit() {
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.DEPOSIT, "", "", ""));
        requestAccountSummary();
    }

    private void issueCard(CardType cardType) {
        PacketDistributor.sendToServer(new SecureAccountPayload(
                SecureAccountAction.ISSUE_CARD,
                cardType.name(),
                "",
                ""
        ));
        requestAccountSummary();
        requestCards();
    }

    private void updateAccountCredit() {
        PacketDistributor.sendToServer(new SecureAccountPayload(
                SecureAccountAction.UPDATE_ACCOUNT_CREDIT,
                accountCreditLimit.getValue(),
                "",
                ""
        ));
        requestAccountSummary();
    }

    private void updateDebitDailyLimit() {
        PacketDistributor.sendToServer(new SecureAccountPayload(
                SecureAccountAction.UPDATE_DEBIT_DAILY_LIMIT,
                debitDailyLimit.getValue(),
                "",
                ""
        ));
    }

    private void payInvoice(String accountPassword) {
        PacketDistributor.sendToServer(new SecureAccountPayload(
                SecureAccountAction.PAY_INVOICE,
                "",
                accountPassword,
                ""
        ));
        requestAccountSummary();
    }

    private void payAllInvoices(String accountPassword) {
        PacketDistributor.sendToServer(new SecureAccountPayload(
                SecureAccountAction.PAY_ALL_INVOICES,
                "",
                accountPassword,
                ""
        ));
        requestAccountSummary();
    }

    private void issueInvoice() {
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.ISSUE_INVOICE, "", "", ""));
        requestAccountSummary();
    }

    private void requestAccountCredit() {
        if (creditRequestCooldownTicks > 0) {
            return;
        }
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.REQUEST_ACCOUNT_CREDIT, "", "", ""));
        creditRequestCooldownTicks = CREDIT_REQUEST_COOLDOWN_TICKS;
        updateCreditRequestButton();
        requestAccountSummary();
    }

    private void requestCards() {
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.REFRESH_CARDS, "", "", ""));
    }

    private void blockSelectedCard(String accountPassword) {
        AtmCardsPayload.CardSummary card = selectedCard();
        if (card == null) {
            return;
        }
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.BLOCK_CARD_BY_ID, card.cardId().toString(), accountPassword, ""));
    }

    private void disableSelectedCard(String accountPassword) {
        AtmCardsPayload.CardSummary card = selectedCard();
        if (card == null) {
            return;
        }
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.DISABLE_CARD_BY_ID, card.cardId().toString(), accountPassword, ""));
    }

    private void logout() {
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.LOGOUT, "", "", ""));
        loggedIn = false;
        selectView(AtmView.LOGIN);
    }

    private void requestSessionState() {
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.SESSION_STATE, "", "", ""));
    }

    private void requestAccountSummary() {
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.ACCOUNT_SUMMARY, "", "", ""));
    }

    private void requestGoldPrices() {
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.GOLD_PRICE_REFRESH, "", "", ""));
    }

    private void requestOperationHistory() {
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.OPERATION_HISTORY, "", "", ""));
    }

    private void syncCardSlotMode() {
        CardSlotMode mode = switch (view) {
            case CREDIT -> CardSlotMode.CREDIT;
            case SECURITY -> CardSlotMode.SECURITY;
            default -> CardSlotMode.LOGIN;
        };
        menu.setCardSlotMode(mode);
        PacketDistributor.sendToServer(new SecureAccountPayload(SecureAccountAction.SET_CARD_SLOT_MODE, mode.name(), "", ""));
    }

    private void selectView(AtmView nextView) {
        if (!loggedIn && nextView.requiresSession) {
            nextView = AtmView.LOGIN;
        }

        view = nextView;
        boolean opening = view == AtmView.CREATE;
        menu.setAccountOpeningVisible(opening);
        PacketDistributor.sendToServer(new SecureAccountPayload(
                SecureAccountAction.SET_ACCOUNT_OPENING_MODE, Boolean.toString(opening), "", ""));
        syncCardSlotMode();
        updateNavigation();
        updateContent();
        if (view == AtmView.GOLD_INFO) {
            requestGoldPrices();
        }
        if (view == AtmView.CARDS) {
            requestCards();
        }
        if (view == AtmView.HISTORY) {
            requestOperationHistory();
        }
    }

    private void updateNavigation() {
        boolean authVisible = !loggedIn;
        setVisible(authVisible, loginTab, createTab, recoverTab);
        setVisible(loggedIn, cashTab, cardsTab, creditTab, transferTab, historyTab, securityTab, goldInfoTab);

        loginTab.active = view != AtmView.LOGIN;
        createTab.active = view != AtmView.CREATE;
        recoverTab.active = view != AtmView.RECOVER;
        cashTab.active = view != AtmView.CASH;
        cardsTab.active = view != AtmView.CARDS;
        creditTab.active = view != AtmView.CREDIT;
        transferTab.active = view != AtmView.TRANSFER;
        historyTab.active = view != AtmView.HISTORY;
        securityTab.active = view != AtmView.SECURITY;
        goldInfoTab.active = view != AtmView.GOLD_INFO;
    }

    private void updateContent() {
        menu.setSlotsVisible(showsCardAndInventory());
        username.setHint(Component.translatable("screen.economia.atm.username"));
        setVisible(false,
                username,
                password,
                newPassword,
                securityPassword,
                securityNewPassword,
                withdrawAmount,
                withdrawDenomination,
                accountCreditLimit,
                cardCreditLimit,
                debitDailyLimit,
                transferAccountNumber,
                transferAmount,
                modalPassword,
                passwordLoginButton,
                cardLoginButton,
                createAccountButton,
                recoverPasswordButton,
                withdrawButton,
                depositButton,
                balanceButton,
                debitCardButton,
                creditCardButton,
                comboCardButton,
                updateAccountCreditButton,
                updateCreditButton,
                updateDebitDailyLimitButton,
                payInvoiceButton,
                issueInvoiceButton,
                payAllInvoicesButton,
                requestCreditButton,
                cardListUpButton,
                cardListDownButton,
                blockListedCardButton,
                disableListedCardButton,
                transferButton,
                changePasswordButton,
                unblockCardButton,
                webLoginTokenButton,
                logoutButton,
                modalConfirmButton,
                modalCancelButton
        );

        switch (view) {
            case LOGIN -> setVisible(true, username, password, passwordLoginButton, cardLoginButton);
            case CREATE -> setVisible(true, username, password, createAccountButton);
            case RECOVER -> {
                setVisible(true, username, newPassword, recoverPasswordButton);
            }
            case CASH -> setVisible(true, withdrawAmount, withdrawDenomination, withdrawButton, depositButton);
            case CARDS -> {
                setVisible(true, debitCardButton, creditCardButton, comboCardButton, debitDailyLimit, updateDebitDailyLimitButton, cardListUpButton, cardListDownButton, blockListedCardButton, disableListedCardButton);
                updateCardListButtons();
            }
            case CREDIT -> {
                setVisible(true, cardCreditLimit, updateCreditButton, requestCreditButton, payInvoiceButton, issueInvoiceButton, payAllInvoicesButton);
            }
            case TRANSFER -> setVisible(true, transferAccountNumber, transferAmount, transferButton);
            case HISTORY -> {
            }
            case SECURITY -> setVisible(true, securityPassword, securityNewPassword, changePasswordButton, unblockCardButton, webLoginTokenButton, logoutButton);
            case GOLD_INFO -> {
            }
        }
        updateModalControls();
        updateCreditRequestButton();
    }

    private void setVisible(boolean visible, AbstractWidget... widgets) {
        for (AbstractWidget widget : widgets) {
            widget.visible = visible;
            widget.active = visible;
            if (!visible && widget instanceof EditBox editBox) {
                editBox.setFocused(false);
            }
        }
    }

    private boolean isTextInputFocused() {
        return isFocused(username)
                || isFocused(password)
                || isFocused(newPassword)
                || isFocused(securityPassword)
                || isFocused(securityNewPassword)
                || isFocused(withdrawAmount)
                || isFocused(withdrawDenomination)
                || isFocused(accountCreditLimit)
                || isFocused(cardCreditLimit)
                || isFocused(debitDailyLimit)
                || isFocused(transferAccountNumber)
                || isFocused(transferAmount)
                || isFocused(modalPassword);
    }

    private boolean isFocused(EditBox editBox) {
        return editBox != null && editBox.visible && editBox.isFocused();
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF0D1012);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF2B3337);
    }

    private void drawRight(GuiGraphics graphics, Component component, int rightX, int y, int color) {
        graphics.drawString(font, component, rightX - font.width(component), y, color, false);
    }

    private void drawInventoryFrames(GuiGraphics graphics, PlayerInventoryMode mode) {
        if (mode == PlayerInventoryMode.FULL) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    drawSlotFrame(graphics, 94 + column * 18, 166 + row * 18);
                }
            }
        }

        if (mode != PlayerInventoryMode.NONE) {
            for (int column = 0; column < 9; column++) {
                drawSlotFrame(graphics, 94 + column * 18, 224);
            }
        }
    }

    private void renderCardIssueValue(GuiGraphics graphics, int x, int y, int width) {
        Component value = Component.translatable("screen.economia.atm.gold_money", menu.cardIssueFee());
        graphics.drawString(font, value, x + (width - font.width(value)) / 2, y, 0xD7E8E1, false);
    }

    private void renderCardList(GuiGraphics graphics) {
        if (atmCards.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.card_list_empty"), 20, 170, 0x8EA09A, false);
            return;
        }
        int end = Math.min(atmCards.size(), cardListOffset + CARD_LIST_VISIBLE_ROWS);
        for (int index = cardListOffset; index < end; index++) {
            int y = 170 + (index - cardListOffset) * 9;
            int color = index == selectedCardIndex ? 0x80E6A8 : 0xD7E8E1;
            graphics.drawString(font, cardListText(atmCards.get(index)), 20, y, color, false);
        }
    }

    private Component cardListText(AtmCardsPayload.CardSummary card) {
        String type = switch (card.cardType()) {
            case "DEBIT" -> "D";
            case "CREDIT" -> "C";
            case "DEBIT_CREDIT" -> "DC";
            default -> "?";
        };
        String status = switch (card.status()) {
            case "ACTIVE" -> "A";
            case "BLOCKED" -> "B";
            case "DISABLED" -> "D";
            case "EXPIRED" -> "E";
            default -> card.status();
        };
        String name = card.cardName().isBlank() ? "#" + card.cardId().toString().substring(0, 4) : card.cardName();
        if (name.length() > 13) {
            name = name.substring(0, 13);
        }
        return Component.literal(name + " " + type + "/" + status + " D:" + compact(card.debt()));
    }

    private String compact(long value) {
        if (value >= 1_000_000_000_000L) {
            return (value / 1_000_000_000_000L) + "T";
        }
        if (value >= 1_000_000_000L) {
            return (value / 1_000_000_000L) + "B";
        }
        if (value >= 1_000_000L) {
            return (value / 1_000_000L) + "M";
        }
        if (value >= 1_000L) {
            return (value / 1_000L) + "K";
        }
        return Long.toString(value);
    }

    private void renderGoldInfo(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("screen.economia.atm.gold_item"), 16, 80, 0xB8C7C2, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.gold_value"), 126, 80, 0xB8C7C2, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.gold_sell_percent"), 190, 80, 0xB8C7C2, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.gold_buy_percent"), 252, 80, 0xB8C7C2, false);
        renderGoldRow(graphics, 96, Items.GOLD_NUGGET.getDescription(), menu.goldBuyNuggetValue());
        renderGoldRow(graphics, 112, Items.GOLD_INGOT.getDescription(), menu.goldBuyIngotValue());
        renderGoldRow(graphics, 128, Items.GOLD_BLOCK.getDescription(), menu.goldBuyBlockValue());
    }

    private void renderGoldRow(GuiGraphics graphics, int y, Component itemName, long value) {
        graphics.drawString(font, itemName, 16, y, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.gold_money", value), 126, y, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.gold_percent", menu.goldSellPercent()), 190, y, 0xD7E8E1, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.gold_percent", menu.goldBuyPercent()), 252, y, 0xD7E8E1, false);
    }

    private void renderOperationHistory(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("screen.economia.atm.history.date"), 16, 80, 0xB8C7C2, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.history.operation"), 82, 80, 0xB8C7C2, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.history.direction"), 190, 80, 0xB8C7C2, false);
        drawRight(graphics, Component.translatable("screen.economia.atm.history.amount"), 332, 80, 0xB8C7C2);
        if (operationHistory.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.economia.atm.history.empty"), 16, 100, 0x8EA09A, false);
            return;
        }
        int rows = Math.min(operationHistory.size(), 8);
        for (int index = 0; index < rows; index++) {
            AtmOperationHistoryPayload.Entry entry = operationHistory.get(index);
            int y = 96 + index * 12;
            int color = entry.directionKey().endsWith(".in") ? 0x80E6A8 : 0xE68E8E;
            graphics.drawString(font, Component.literal(entry.occurredAt()), 16, y, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable(entry.operationKey()), 82, y, 0xD7E8E1, false);
            graphics.drawString(font, Component.translatable(entry.directionKey()), 190, y, color, false);
            drawRight(graphics, Component.translatable("screen.economia.atm.gold_money", entry.amount()), 332, y, color);
        }
    }

    private boolean showsCardAndInventory() {
        return view == AtmView.LOGIN || view == AtmView.CARDS || view == AtmView.CREDIT || view == AtmView.SECURITY;
    }

    private boolean showsMenuInventory() {
        return view == AtmView.CREATE || showsCardAndInventory();
    }

    private boolean usesHotbarOnly() {
        return view == AtmView.CARDS || view == AtmView.CREDIT;
    }

    private PlayerInventoryMode inventoryModeForView() {
        if (view == AtmView.CREATE) {
            return PlayerInventoryMode.FULL;
        }
        if (!showsCardAndInventory()) {
            return PlayerInventoryMode.NONE;
        }
        return usesHotbarOnly() ? PlayerInventoryMode.HOTBAR : PlayerInventoryMode.FULL;
    }

    private void openPasswordModal(PendingSensitiveAction action) {
        pendingSensitiveAction = action;
        modalPassword.setValue("");
        updateModalControls();
        modalPassword.setFocused(true);
    }

    private void closePasswordModal() {
        pendingSensitiveAction = PendingSensitiveAction.NONE;
        modalPassword.setValue("");
        updateContent();
    }

    private void confirmPasswordModal() {
        if (!passwordModalOpen()) {
            return;
        }
        String value = modalPassword.getValue();
        if (pendingSensitiveAction == PendingSensitiveAction.WITHDRAW) {
            withdraw(value);
        } else if (pendingSensitiveAction == PendingSensitiveAction.TRANSFER) {
            transfer(value);
        } else if (pendingSensitiveAction == PendingSensitiveAction.PAY_INVOICE) {
            payInvoice(value);
        } else if (pendingSensitiveAction == PendingSensitiveAction.PAY_ALL_INVOICES) {
            payAllInvoices(value);
        } else if (pendingSensitiveAction == PendingSensitiveAction.BLOCK_CARD) {
            blockSelectedCard(value);
        } else if (pendingSensitiveAction == PendingSensitiveAction.DISABLE_CARD) {
            disableSelectedCard(value);
        }
        closePasswordModal();
    }

    private boolean passwordModalOpen() {
        return pendingSensitiveAction != PendingSensitiveAction.NONE;
    }

    private void updateModalControls() {
        boolean open = passwordModalOpen();
        setVisible(open, modalPassword, modalConfirmButton, modalCancelButton);
        if (open) {
            setContentVisible(false);
            menu.setSlotsVisible(false);
            return;
        }
        menu.setSlotsVisible(showsCardAndInventory());
        menu.setPlayerInventoryMode(inventoryModeForView());
        setContentActive(!open);
    }

    private void renderPasswordModal(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!passwordModalOpen()) {
            return;
        }
        graphics.fill(leftPos + 82, topPos + 72, leftPos + 270, topPos + 168, 0xF014181A);
        graphics.fill(leftPos + 86, topPos + 76, leftPos + 266, topPos + 164, 0xFF20262A);
        graphics.drawString(font, Component.translatable(pendingSensitiveAction.titleKey), leftPos + 94, topPos + 88, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.economia.atm.password"), leftPos + 94, topPos + 104, 0xB8C7C2, false);
        modalPassword.render(graphics, mouseX, mouseY, partialTick);
        modalConfirmButton.render(graphics, mouseX, mouseY, partialTick);
        modalCancelButton.render(graphics, mouseX, mouseY, partialTick);
    }

    private void setContentActive(boolean active) {
        for (AbstractWidget widget : List.of(
                loginTab, createTab, recoverTab, cashTab, cardsTab, creditTab, transferTab, historyTab, securityTab, goldInfoTab,
                username, password, newPassword, securityPassword, securityNewPassword, withdrawAmount, withdrawDenomination,
                accountCreditLimit, cardCreditLimit, debitDailyLimit, transferAccountNumber, transferAmount,
                passwordLoginButton, cardLoginButton, createAccountButton, recoverPasswordButton,
                withdrawButton, depositButton, balanceButton, debitCardButton, creditCardButton, comboCardButton,
                updateAccountCreditButton, updateCreditButton, updateDebitDailyLimitButton, payInvoiceButton, issueInvoiceButton, payAllInvoicesButton,
                requestCreditButton, cardListUpButton, cardListDownButton, blockListedCardButton, disableListedCardButton,
                transferButton, changePasswordButton, unblockCardButton, logoutButton
        )) {
            if (widget != null && widget.visible) {
                widget.active = active;
            }
        }
        if (view == AtmView.CREDIT && accountCreditLimit != null && accountCreditLimit.visible) {
            accountCreditLimit.active = false;
        }
    }

    private void setContentVisible(boolean visible) {
        for (AbstractWidget widget : List.of(
                loginTab, createTab, recoverTab, cashTab, cardsTab, creditTab, transferTab, historyTab, securityTab, goldInfoTab,
                username, password, newPassword, securityPassword, securityNewPassword, withdrawAmount, withdrawDenomination,
                accountCreditLimit, cardCreditLimit, debitDailyLimit, transferAccountNumber, transferAmount,
                passwordLoginButton, cardLoginButton, createAccountButton, recoverPasswordButton,
                withdrawButton, depositButton, balanceButton, debitCardButton, creditCardButton, comboCardButton,
                updateAccountCreditButton, updateCreditButton, updateDebitDailyLimitButton, payInvoiceButton, issueInvoiceButton, payAllInvoicesButton,
                requestCreditButton, cardListUpButton, cardListDownButton, blockListedCardButton, disableListedCardButton,
                transferButton, changePasswordButton, unblockCardButton, logoutButton
        )) {
            if (widget != null) {
                widget.visible = visible;
                widget.active = visible;
                if (!visible && widget instanceof EditBox editBox) {
                    editBox.setFocused(false);
                }
            }
        }
    }

    private void onTransferAccountChanged(String value) {
        if (applyingAccountMask) {
            return;
        }
        accountMaskTicks = value.isBlank() ? 0 : ACCOUNT_MASK_DELAY_TICKS;
    }

    private void tickTransferAccountMask() {
        if (view != AtmView.TRANSFER || accountMaskTicks <= 0 || transferAccountNumber == null || passwordModalOpen()) {
            return;
        }
        accountMaskTicks--;
        if (accountMaskTicks > 0) {
            return;
        }
        String digits = transferAccountNumber.getValue();
        if (digits.isBlank() || digits.length() >= 6) {
            return;
        }
        applyingAccountMask = true;
        try {
            transferAccountNumber.setValue("%06d".formatted(Integer.parseInt(digits)));
        } catch (NumberFormatException ignored) {
            transferAccountNumber.setValue("");
        } finally {
            applyingAccountMask = false;
        }
    }

    private void tickCreditRequestCooldown() {
        if (creditRequestCooldownTicks <= 0) {
            return;
        }
        creditRequestCooldownTicks--;
        updateCreditRequestButton();
    }

    private void tickBalanceRefresh() {
        if (!loggedIn || view != AtmView.CASH || passwordModalOpen()) {
            balanceRefreshTicks = 0;
            return;
        }
        balanceRefreshTicks++;
        if (balanceRefreshTicks >= BALANCE_REFRESH_TICKS) {
            balanceRefreshTicks = 0;
            requestAccountSummary();
        }
    }

    private void updateCreditRequestButton() {
        if (requestCreditButton != null && requestCreditButton.visible) {
            requestCreditButton.active = creditRequestCooldownTicks <= 0 && !passwordModalOpen();
        }
    }

    private void scrollCardList(int delta) {
        cardListOffset = Math.max(0, Math.min(maxCardListOffset(), cardListOffset + delta));
        if (!atmCards.isEmpty() && (selectedCardIndex < cardListOffset || selectedCardIndex >= cardListOffset + CARD_LIST_VISIBLE_ROWS)) {
            selectedCardIndex = cardListOffset;
        }
        updateCardListButtons();
    }

    private int maxCardListOffset() {
        return Math.max(0, atmCards.size() - CARD_LIST_VISIBLE_ROWS);
    }

    private AtmCardsPayload.CardSummary selectedCard() {
        return selectedCardIndex >= 0 && selectedCardIndex < atmCards.size() ? atmCards.get(selectedCardIndex) : null;
    }

    private void updateCardListButtons() {
        if (cardListUpButton == null) {
            return;
        }
        boolean visible = view == AtmView.CARDS && !passwordModalOpen();
        AtmCardsPayload.CardSummary selected = selectedCard();
        boolean actionable = visible && selected != null && !"DISABLED".equals(selected.status()) && !"EXPIRED".equals(selected.status());
        cardListUpButton.active = visible && cardListOffset > 0;
        cardListDownButton.active = visible && cardListOffset < maxCardListOffset();
        blockListedCardButton.active = actionable && !"BLOCKED".equals(selected.status());
        disableListedCardButton.active = actionable;
    }

    private enum AtmView {
        LOGIN("screen.economia.atm.login_section", false),
        CREATE("screen.economia.atm.create_section", false),
        RECOVER("screen.economia.atm.recover_section", false),
        CASH("screen.economia.atm.cash_section", true),
        CARDS("screen.economia.atm.card_section", true),
        CREDIT("screen.economia.atm.credit_section", true),
        TRANSFER("screen.economia.atm.transfer_section", true),
        HISTORY("screen.economia.atm.history_section", true),
        SECURITY("screen.economia.atm.security_section", true),
        GOLD_INFO("screen.economia.atm.gold_info_section", true);

        private final String titleKey;
        private final boolean requiresSession;

        AtmView(String titleKey, boolean requiresSession) {
            this.titleKey = titleKey;
            this.requiresSession = requiresSession;
        }
    }

    private enum PendingSensitiveAction {
        NONE(""),
        WITHDRAW("screen.economia.atm.withdraw_password_title"),
        TRANSFER("screen.economia.atm.transfer_password_title"),
        PAY_INVOICE("screen.economia.atm.invoice_password_title"),
        PAY_ALL_INVOICES("screen.economia.atm.invoice_password_title"),
        BLOCK_CARD("screen.economia.atm.card_password_title"),
        DISABLE_CARD("screen.economia.atm.card_password_title");

        private final String titleKey;

        PendingSensitiveAction(String titleKey) {
            this.titleKey = titleKey;
        }
    }
}
