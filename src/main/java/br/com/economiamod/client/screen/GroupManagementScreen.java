package br.com.economiamod.client.screen;

import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.TerritoryPermission;
import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.common.menu.GroupManagementMenu;
import br.com.economiamod.common.network.GroupAction;
import br.com.economiamod.common.network.GroupActionPayload;
import br.com.economiamod.common.network.GroupStatePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GroupManagementScreen extends AbstractContainerScreen<GroupManagementMenu> {
    private enum Page { LOGIN, HOME, CREATE, INVITES, INVITE, MEMBERS, PERMISSIONS, BANK, SETTINGS, UPGRADE }

    private final List<AbstractWidget> pageWidgets = new ArrayList<>();
    private GroupStatePayload state;
    private Page page = Page.LOGIN;
    private EditBox textInput;
    private EditBox amountInput;
    private int listPage;
    private DirectPaymentMethod upgradePaymentMethod = DirectPaymentMethod.DEBIT;
    private boolean upgradePurchasePending;

    public GroupManagementScreen(GroupManagementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 316;
        imageHeight = 236;
        inventoryLabelY = 144;
    }

    @Override
    protected void init() {
        super.init();
        rebuildPage();
        PacketDistributor.sendToServer(GroupActionPayload.simple(GroupAction.REFRESH));
    }

    public void applyState(GroupStatePayload payload) {
        state = payload;
        upgradePurchasePending = false;
        if (!payload.authenticated()) {
            page = Page.LOGIN;
        } else if (!payload.hasGroup() && page != Page.CREATE && page != Page.INVITES) {
            page = Page.HOME;
        } else if (payload.hasGroup() && (page == Page.LOGIN || page == Page.CREATE || page == Page.INVITES)) {
            page = Page.HOME;
        }
        rebuildPage();
    }

    private void rebuildPage() {
        for (AbstractWidget widget : pageWidgets) {
            removeWidget(widget);
        }
        pageWidgets.clear();
        textInput = null;
        amountInput = null;
        if (page == Page.LOGIN || state == null || !state.authenticated()) {
            addButton(196, 34, 96, "screen.economia.group.login", () -> send(GroupAction.AUTHENTICATE));
            addButton(212, 132, 80, "screen.economia.common.exit", this::onClose);
            return;
        }
        if (!state.hasGroup()) {
            if (state.groupType() == br.com.economiamod.common.group.GroupType.PRIVATE_PROPERTY) {
                addButton(212, 132, 80, "screen.economia.common.exit", this::onClose);
                return;
            }
            if (page == Page.CREATE) {
                textInput = addInput(34, 50, 210, "screen.economia.group.name");
                addButton(252, 50, 54, "screen.economia.common.create", () -> sendText(GroupAction.CREATE));
                addBack();
            } else if (page == Page.INVITES) {
                addInviteButtons();
                addBack();
            } else {
                addButton(34, 48, 118, "screen.economia.common.create", () -> open(Page.CREATE));
                addButton(166, 48, 118, "screen.economia.group.invites", () -> open(Page.INVITES));
                addButton(212, 132, 80, "screen.economia.common.exit", this::onClose);
            }
            return;
        }
        switch (page) {
            case HOME -> buildHome();
            case INVITE -> { buildInvite(); addBack(); }
            case MEMBERS -> { addMemberButtons(false); addBack(); }
            case PERMISSIONS -> { addMemberButtons(true); addBack(); }
            case BANK -> { buildBank(); addBack(); }
            case SETTINGS -> { buildSettings(); addBack(); }
            case UPGRADE -> { buildUpgrade(); addBack(); }
            default -> page = Page.HOME;
        }
    }

    private void buildHome() {
        if (state.groupType() == br.com.economiamod.common.group.GroupType.CLAN) {
            addButton(24, 48, 126, "screen.economia.group.members", () -> open(Page.MEMBERS));
        }
        addButton(166, 48, 126, "screen.economia.group.bank", () -> open(Page.BANK));
        if (state.groupType() == br.com.economiamod.common.group.GroupType.CLAN) {
            addButton(24, 74, 126, "screen.economia.group.invite", () -> open(Page.INVITE));
        }
        if (state.groupType() == br.com.economiamod.common.group.GroupType.CLAN && state.role().leadsClan()) {
            addButton(166, 74, 126, "screen.economia.group.permissions", () -> open(Page.PERMISSIONS));
        }
        addButton(24, 100, 126, "screen.economia.group.settings", () -> open(Page.SETTINGS));
        if (state.role() == GroupRole.OWNER || state.role().leadsClan()) {
            addButton(166, 100, 126, "screen.economia.group.upgrades", () -> open(Page.UPGRADE));
        }
        addButton(212, 132, 80, "screen.economia.common.exit", this::onClose);
    }

    private void buildInvite() {
        textInput = addInput(24, 50, 196, "screen.economia.group.player_name");
        addButton(226, 50, 66, "screen.economia.group.send", () -> sendText(GroupAction.INVITE));
    }

    private void buildUpgrade() {
        if (state.upgradeMaximumReached() || !state.upgradeConfigurationValid()) {
            return;
        }
        addRawButton(24, 106, 80, paymentLabel(DirectPaymentMethod.DEBIT),
                () -> selectUpgradePayment(DirectPaymentMethod.DEBIT));
        addRawButton(110, 106, 80, paymentLabel(DirectPaymentMethod.CREDIT),
                () -> selectUpgradePayment(DirectPaymentMethod.CREDIT));
        addRawButton(196, 106, 80, paymentLabel(DirectPaymentMethod.CASH),
                () -> selectUpgradePayment(DirectPaymentMethod.CASH));
        Button buy = addButton(166, 132, 126, "screen.economia.group.buy", this::buyUpgrade);
        buy.active = !upgradePurchasePending;
    }

    private void addInviteButtons() {
        int start = listPage * 4;
        int row = 0;
        for (int index = start; index < state.invites().size() && row < 4; index++) {
            GroupStatePayload.InviteSummary invite = state.invites().get(index);
            int y = 48 + row * 24;
            addButton(176, y, 56, "screen.economia.group.accept", () -> sendTarget(GroupAction.ACCEPT_INVITE, invite.inviteId()));
            addButton(238, y, 56, "screen.economia.group.decline", () -> sendTarget(GroupAction.DECLINE_INVITE, invite.inviteId()));
            row++;
        }
        addPagination(state.invites().size());
    }

    private void addMemberButtons(boolean permissions) {
        int start = listPage * 4;
        int row = 0;
        for (int index = start; index < state.members().size() && row < 4; index++) {
            GroupStatePayload.MemberSummary member = state.members().get(index);
            int y = 42 + row * 20;
            if (permissions && member.role() == GroupRole.MEMBER) {
                addPermissionButton(188, y, "U", member, TerritoryPermission.USE);
                addPermissionButton(216, y, "D", member, TerritoryPermission.DESTROY);
                addPermissionButton(244, y, "C", member, TerritoryPermission.PLACE);
            } else if (!permissions && state.role().leadsClan() && member.role() != GroupRole.LEADER) {
                if (state.groupType() == br.com.economiamod.common.group.GroupType.CLAN
                        && state.role() == GroupRole.LEADER) {
                    addButton(188, y, 52, "screen.economia.group.vice", () -> sendTarget(GroupAction.SET_VICE_LEADER,
                            member.playerUuid()));
                }
                addButton(244, y, 54, "screen.economia.group.remove", () -> sendTarget(GroupAction.REMOVE_MEMBER,
                        member.playerUuid()));
            }
            row++;
        }
        addPagination(state.members().size());
    }

    private void addPagination(int size) {
        if (listPage > 0) {
            addRawButton(112, 132, 40, "<", () -> { listPage--; rebuildPage(); });
        }
        if ((listPage + 1) * 4 < size) {
            addRawButton(158, 132, 40, ">", () -> { listPage++; rebuildPage(); });
        }
    }

    private void addPermissionButton(int x, int y, String label, GroupStatePayload.MemberSummary member,
                                     TerritoryPermission permission) {
        int nextMask = member.permissionMask() ^ permission.bit();
        Button button = addRawButton(x, y, 24, label, () -> sendPermissions(member.playerUuid(), nextMask));
        button.active = true;
    }

    private void buildBank() {
        amountInput = addInput(34, 70, 110, "screen.economia.group.amount");
        amountInput.setFilter(value -> value.matches("[0-9]*"));
        addButton(154, 44, 68, "screen.economia.group.deposit", () -> sendMoney(GroupAction.DEPOSIT, false));
        addButton(228, 44, 68, "screen.economia.group.withdraw", () -> sendMoney(GroupAction.WITHDRAW, false));
        if (state.groupType() == br.com.economiamod.common.group.GroupType.CLAN) {
            addButton(154, 72, 68, "screen.economia.group.fund_in", () -> sendMoney(GroupAction.DEPOSIT, true));
            addButton(228, 72, 68, "screen.economia.group.fund_out", () -> sendMoney(GroupAction.WITHDRAW, true));
        }
    }

    private void buildSettings() {
        textInput = addInput(24, 46, 184, "screen.economia.group.name");
        textInput.setValue(state.groupName());
        addButton(214, 46, 80, "screen.economia.group.rename", () -> sendText(GroupAction.RENAME));
        if (state.role() == GroupRole.LEADER || state.role() == GroupRole.OWNER) {
            addRawButton(24, 74, 132, visitorLabel("buy", state.visitorBuyShop()),
                    () -> sendVisitor(!state.visitorBuyShop(), state.visitorSellShop()));
            addRawButton(162, 74, 132, visitorLabel("sell", state.visitorSellShop()),
                    () -> sendVisitor(state.visitorBuyShop(), !state.visitorSellShop()));
            addButton(162, 102, 132, "screen.economia.group.close", () -> send(GroupAction.CLOSE));
        }
        if (state.role() != GroupRole.LEADER && state.role() != GroupRole.OWNER) {
            addButton(24, 102, 132, "screen.economia.group.leave", () -> send(GroupAction.LEAVE));
        }
    }

    private void addBack() {
        addButton(24, 132, 80, "screen.economia.group.back", () -> open(Page.HOME));
    }

    private EditBox addInput(int x, int y, int width, String hintKey) {
        EditBox input = new EditBox(font, leftPos + x, topPos + y, width, 20, Component.translatable(hintKey));
        input.setMaxLength(64);
        input.setHint(Component.translatable(hintKey));
        pageWidgets.add(addRenderableWidget(input));
        return input;
    }

    private Button addButton(int x, int y, int width, String key, Runnable action) {
        return addRawButton(x, y, width, Component.translatable(key).getString(), action);
    }

    private Button addRawButton(int x, int y, int width, String label, Runnable action) {
        Button button = Button.builder(Component.literal(label), ignored -> action.run())
                .bounds(leftPos + x, topPos + y, width, 18).build();
        pageWidgets.add(addRenderableWidget(button));
        return button;
    }

    private void open(Page next) {
        if (page == Page.UPGRADE && next != Page.UPGRADE) {
            send(GroupAction.CLOSE_UPGRADE_PAYMENT);
        }
        page = next;
        listPage = 0;
        if (next == Page.UPGRADE) {
            upgradePaymentMethod = DirectPaymentMethod.DEBIT;
            sendUpgradePaymentMode(false);
        }
        rebuildPage();
    }

    private void send(GroupAction action) { PacketDistributor.sendToServer(GroupActionPayload.simple(action)); }

    private void sendText(GroupAction action) {
        PacketDistributor.sendToServer(new GroupActionPayload(action, textInput == null ? "" : textInput.getValue(),
                new UUID(0L, 0L), 0, 0L, false, false, UUID.randomUUID()));
    }

    private void sendTarget(GroupAction action, UUID target) {
        PacketDistributor.sendToServer(new GroupActionPayload(action, "", target, 0, 0L, false, false, UUID.randomUUID()));
    }

    private void sendPermissions(UUID target, int mask) {
        PacketDistributor.sendToServer(new GroupActionPayload(GroupAction.SET_PERMISSIONS, "", target,
                mask, 0L, false, false, UUID.randomUUID()));
    }

    private void sendMoney(GroupAction action, boolean support) {
        try {
            long amount = Long.parseLong(amountInput.getValue());
            PacketDistributor.sendToServer(new GroupActionPayload(action, "", new UUID(0L, 0L),
                    0, amount, support, false, UUID.randomUUID()));
        } catch (NumberFormatException ignored) {
        }
    }

    private void sendVisitor(boolean buy, boolean sell) {
        PacketDistributor.sendToServer(new GroupActionPayload(GroupAction.SET_VISITOR_SHOPS, "",
                new UUID(0L, 0L), 0, 0L, buy, sell, UUID.randomUUID()));
    }

    private void selectUpgradePayment(DirectPaymentMethod method) {
        upgradePaymentMethod = method;
        sendUpgradePaymentMode(method == DirectPaymentMethod.CASH);
        rebuildPage();
    }

    private void sendUpgradePaymentMode(boolean cash) {
        PacketDistributor.sendToServer(new GroupActionPayload(GroupAction.SET_UPGRADE_PAYMENT, "",
                new UUID(0L, 0L), 0, 0L, cash, false, UUID.randomUUID()));
    }

    private void buyUpgrade() {
        if (upgradePurchasePending || state == null) {
            return;
        }
        upgradePurchasePending = true;
        PacketDistributor.sendToServer(new GroupActionPayload(GroupAction.BUY_UPGRADE,
                upgradePaymentMethod.name(), new UUID(0L, 0L), state.claimLimit(), state.upgradePrice(),
                upgradePaymentMethod == DirectPaymentMethod.CASH, false, UUID.randomUUID()));
        rebuildPage();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF171B1D);
        graphics.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + 30, 0xFF2F4B5B);
        if (page == Page.LOGIN || state == null || !state.authenticated()) {
            InventorySlotRenderer.draw(graphics, leftPos + 80, topPos + 36);
        }
        drawRows(graphics);
        drawInventory(graphics);
    }

    private void drawRows(GuiGraphics graphics) {
        if (state == null || !state.authenticated()) return;
        if (state.hasGroup()) {
            String heading = state.groupName() + " — " + roleName(state.role());
            graphics.drawString(font, font.plainSubstrByWidth(heading, imageWidth - 32),
                    leftPos + 16, topPos + 34, 0xFFFFFFFF, false);
        } else if (state.groupType() == br.com.economiamod.common.group.GroupType.PRIVATE_PROPERTY) {
            graphics.drawWordWrap(font, Component.translatable("screen.economia.group.private_property_claim_first"),
                    leftPos + 24, topPos + 54, imageWidth - 48, 0xFFE1E8EB);
        }
        if (page == Page.MEMBERS || page == Page.PERMISSIONS) {
            int start = listPage * 4;
            int row = 0;
            for (int index = start; index < state.members().size() && row < 4; index++) {
                GroupStatePayload.MemberSummary member = state.members().get(index);
                String memberLabel = member.displayName() + "  " + roleName(member.role());
                graphics.drawString(font, font.plainSubstrByWidth(memberLabel, 152),
                        leftPos + 24, topPos + 47 + row * 20, 0xFFE1E8EB, false);
                row++;
            }
        }
        if (page == Page.INVITES) {
            int start = listPage * 4;
            int row = 0;
            for (int index = start; index < state.invites().size() && row < 4; index++) {
                GroupStatePayload.InviteSummary invite = state.invites().get(index);
                graphics.drawString(font, invite.groupName(), leftPos + 24, topPos + 53 + row * 24, 0xFFE1E8EB, false);
                row++;
            }
        }
        if (page == Page.BANK) {
            graphics.drawString(font, Component.translatable("screen.economia.group.balance", state.balance()),
                    leftPos + 34, topPos + 46, 0xFFFFFFFF, false);
            if (state.groupType() == br.com.economiamod.common.group.GroupType.CLAN) {
                graphics.drawString(font, Component.translatable("screen.economia.group.support_balance", state.supportBalance()),
                        leftPos + 34, topPos + 58, 0xFFFFFFFF, false);
            }
        }
        if (page == Page.UPGRADE) {
            graphics.drawString(font, Component.translatable("screen.economia.group.current_limit", state.claimLimit()),
                    leftPos + 34, topPos + 50, 0xFFFFFFFF, false);
            graphics.drawString(font, Component.translatable("screen.economia.group.next_limit",
                            state.upgradeMaximumReached() ? state.claimLimit() : state.claimLimit() + 1),
                    leftPos + 34, topPos + 66, 0xFFFFFFFF, false);
            if (!state.upgradeConfigurationValid()) {
                graphics.drawString(font, Component.translatable("screen.economia.group.upgrade_invalid_config"),
                        leftPos + 34, topPos + 84, 0xFFFF6B6B, false);
            } else if (state.upgradeMaximumReached()) {
                graphics.drawString(font, Component.translatable("screen.economia.group.upgrade_maximum",
                                state.claimLimit(), state.upgradeMaxLimit()),
                        leftPos + 34, topPos + 84, 0xFFFFB74D, false);
            } else {
                graphics.drawString(font, Component.translatable("screen.economia.group.upgrade_percentage",
                                percentage(state.upgradePercentageBasisPoints())),
                        leftPos + 34, topPos + 82, 0xFFFFFFFF, false);
                graphics.drawString(font, Component.translatable("screen.economia.group.upgrade_price", state.upgradePrice()),
                        leftPos + 34, topPos + 94, 0xFFFFFFFF, false);
                if (upgradePaymentMethod == DirectPaymentMethod.CASH) {
                    for (int slot = 0; slot < 6; slot++) {
                        InventorySlotRenderer.draw(graphics, leftPos + 206 + (slot % 3) * 18,
                                topPos + 48 + (slot / 3) * 18);
                    }
                } else {
                    graphics.drawString(font, Component.translatable("screen.economia.group.authenticated_card"),
                            leftPos + 196, topPos + 56, 0xFF80DEEA, false);
                }
            }
        }
    }

    private void drawInventory(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            InventorySlotRenderer.draw(graphics, leftPos + 8 + col * 18, topPos + 156 + row * 18);
        for (int col = 0; col < 9; col++)
            InventorySlotRenderer.draw(graphics, leftPos + 8 + col * 18, topPos + 214);
    }

    private String roleName(GroupRole role) {
        return Component.translatable("screen.economia.group.role."
                + role.name().toLowerCase(Locale.ROOT)).getString();
    }

    private String visitorLabel(String shop, boolean enabled) {
        return Component.translatable("screen.economia.group.visitor_" + shop,
                enabled ? "ON" : "OFF").getString();
    }

    private String paymentLabel(DirectPaymentMethod method) {
        String key = "screen.economia.payment." + method.name().toLowerCase(Locale.ROOT);
        String label = Component.translatable(key).getString();
        return method == upgradePaymentMethod ? "[" + label + "]" : label;
    }

    private String percentage(int basisPoints) {
        return "%d,%02d%%".formatted(basisPoints / 100, Math.abs(basisPoints % 100));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 12, 0xFFFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
