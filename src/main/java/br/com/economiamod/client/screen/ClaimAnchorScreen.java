package br.com.economiamod.client.screen;

import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.group.TerritoryPermission;
import br.com.economiamod.common.menu.ClaimAnchorMenu;
import br.com.economiamod.common.network.ClaimAnchorAction;
import br.com.economiamod.common.network.ClaimAnchorActionPayload;
import br.com.economiamod.server.claim.ClaimAnchorMenuState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClaimAnchorScreen extends AbstractContainerScreen<ClaimAnchorMenu> {
    private enum Page { INFO, PAYMENT, TAXES, SALE, MEMBER }

    private final List<AbstractWidget> widgets = new ArrayList<>();
    private Page page = Page.INFO;
    private EditBox textInput;
    private EditBox valueInput;
    private DirectPaymentMethod paymentMethod;
    private boolean lastAuthenticated;

    public ClaimAnchorScreen(ClaimAnchorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 276;
        imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();
        lastAuthenticated = menu.authenticated();
        rebuild();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (lastAuthenticated != menu.authenticated()) {
            lastAuthenticated = menu.authenticated();
            page = Page.INFO;
            rebuild();
        }
    }

    private void rebuild() {
        for (AbstractWidget widget : widgets) removeWidget(widget);
        widgets.clear();
        textInput = null;
        valueInput = null;
        ClaimAnchorMenuState state = menu.state();
        if (!menu.authenticated()) {
            button(158, 54, 96, "screen.economia.claim.login", () -> send(
                    ClaimAnchorAction.AUTHENTICATE, "", 0L, 0));
            button(184, 128, 72, "screen.economia.claim.exit", this::onClose);
            return;
        }
        if (page == Page.INFO) {
            if (!state.active()) {
                Button claim = button(38, 128, 96, "screen.economia.claim.claim", this::openPayment);
                claim.active = state.canClaim();
                button(142, 128, 96, "screen.economia.claim.exit", this::onClose);
            } else {
                if (state.canManage()) {
                    button(18, 151, 72, "screen.economia.claim.taxes", () -> open(Page.TAXES));
                    button(98, 151, 78, "screen.economia.claim.sell", () -> open(Page.SALE));
                    if (state.groupType() == GroupType.PRIVATE_PROPERTY) {
                        button(184, 151, 74, "screen.economia.claim.invite", () -> open(Page.MEMBER));
                    }
                    Button buyChunk = button(18, 176, 120, "screen.economia.claim.buy_chunk", () -> send(
                            ClaimAnchorAction.OPEN_CHUNK_MAP, "", 0L, 0));
                    buyChunk.active = state.canBuyChunk();
                    button(184, 201, 72, "screen.economia.claim.exit", this::onClose);
                } else {
                    button(184, 153, 72, "screen.economia.claim.exit", this::onClose);
                }
            }
        } else if (page == Page.PAYMENT) {
            paymentButton(18, DirectPaymentMethod.DEBIT, "screen.economia.payment.debit");
            paymentButton(98, DirectPaymentMethod.CREDIT, "screen.economia.payment.credit");
            paymentButton(178, DirectPaymentMethod.CASH, "screen.economia.payment.cash");
            Button pay = button(48, 124, 112, "screen.economia.claim.pay", () -> send(
                    ClaimAnchorAction.PAY_CLAIM, paymentMethod.name(), state.landPrice(), 0));
            pay.active = paymentMethod != null;
            button(184, 124, 72, "screen.economia.claim.back", this::closePayment);
        } else if (page == Page.TAXES) {
            button(24, 112, 108, "screen.economia.claim.print_current_tax", () -> send(
                    ClaimAnchorAction.CURRENT_TAX_INVOICE, "", 0L, 0));
            button(140, 112, 112, "screen.economia.claim.print_all_taxes", () -> send(
                    ClaimAnchorAction.ALL_TAXES_INVOICE, "", 0L, 0));
            back();
        } else if (page == Page.SALE) {
            textInput = input(90, 58, 146, "");
            valueInput = input(90, 86, 146, Long.toString(state.suggestedSalePrice()));
            valueInput.setFilter(value -> value.matches("[0-9]*"));
            button(64, 119, 112, "screen.economia.claim.generate_invoice", this::sendSale);
            back();
        } else {
            textInput = input(24, 42, 146, "");
            button(176, 42, 76, "screen.economia.claim.invite", () -> send(
                    ClaimAnchorAction.INVITE_MEMBER, textInput.getValue(), 0L, 0));
            int row = 0;
            for (var member : state.privateMembers()) {
                if (row >= 3) break;
                int y = 72 + row * 24;
                permissionButton(142, y, "U", member, TerritoryPermission.USE);
                permissionButton(168, y, "C", member, TerritoryPermission.PLACE);
                permissionButton(194, y, "R", member, TerritoryPermission.DESTROY);
                button(220, y, 32, "screen.economia.claim.remove_short", () -> send(
                        ClaimAnchorAction.REMOVE_MEMBER, member.playerUuid().toString(), 0L, 0));
                row++;
            }
            back();
        }
    }

    private void paymentButton(int x, DirectPaymentMethod method, String key) {
        Button button = button(x, 48, 76, key, () -> selectPayment(method));
        button.active = paymentMethod != method;
    }

    private void openPayment() {
        paymentMethod = null;
        page = Page.PAYMENT;
        send(ClaimAnchorAction.OPEN_PAYMENT, "", 0L, 0);
        rebuild();
    }

    private void selectPayment(DirectPaymentMethod method) {
        paymentMethod = method;
        send(ClaimAnchorAction.SET_PAYMENT_MODE, method.name(), 0L, 0);
        rebuild();
    }

    private void closePayment() {
        send(ClaimAnchorAction.CLOSE_PAYMENT, "", 0L, 0);
        page = Page.INFO;
        rebuild();
    }

    private void permissionButton(int x, int y, String label,
                                  br.com.economiamod.server.claim.PrivatePropertyMemberView member,
                                  TerritoryPermission permission) {
        int nextMask = member.permissionMask() ^ permission.bit();
        Button permissionButton = Button.builder(Component.literal(
                        permission.presentIn(member.permissionMask()) ? "[" + label + "]" : label), ignored -> send(
                        ClaimAnchorAction.UPDATE_MEMBER_PERMISSIONS, member.playerUuid().toString(), nextMask, 0))
                .bounds(leftPos + x, topPos + y, 22, 20).build();
        widgets.add(addRenderableWidget(permissionButton));
    }

    private void sendSale() {
        try {
            send(ClaimAnchorAction.SALE_INVOICE, textInput.getValue(), Long.parseLong(valueInput.getValue()), 0);
        } catch (NumberFormatException ignored) {
        }
    }

    private void send(ClaimAnchorAction action, String text, long amount, int days) {
        PacketDistributor.sendToServer(new ClaimAnchorActionPayload(action, text, amount, days, UUID.randomUUID()));
    }

    private void open(Page next) {
        page = next;
        rebuild();
    }

    private void back() {
        button(184, 151, 72, "screen.economia.claim.back", () -> open(Page.INFO));
    }

    private Button button(int x, int y, int width, String key, Runnable action) {
        Button button = Button.builder(Component.translatable(key), ignored -> action.run())
                .bounds(leftPos + x, topPos + y, width, 20).build();
        widgets.add(addRenderableWidget(button));
        return button;
    }

    private EditBox input(int x, int y, int width, String initial) {
        EditBox input = new EditBox(font, leftPos + x, topPos + y, width, 20, Component.empty());
        input.setMaxLength(64);
        input.setValue(initial);
        widgets.add(addRenderableWidget(input));
        return input;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF171B1D);
        graphics.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + 31, 0xFF2F4B5B);
        ClaimAnchorMenuState state = menu.state();
        int color = 0xFFE1E8EB;
        if (!menu.authenticated()) {
            graphics.drawString(font, Component.translatable("screen.economia.claim.login_instruction"),
                    leftPos + 22, topPos + 47, color, false);
            InventorySlotRenderer.draw(graphics, leftPos + 129, topPos + 88);
            drawPlayerInventory(graphics);
            return;
        }
        if (page == Page.INFO) {
            graphics.drawString(font, Component.translatable("screen.economia.claim.location",
                    state.blockX(), state.blockY(), state.blockZ()), leftPos + 22, topPos + 45, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.land_price", state.landPrice()),
                    leftPos + 22, topPos + 65, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.territories",
                    state.territoryCount(), state.territoryLimit()), leftPos + 22, topPos + 85, color, false);
            if (state.active()) {
                graphics.drawString(font, Component.translatable("screen.economia.claim.chunks",
                        state.chunkCount(), state.chunkLimit()), leftPos + 22, topPos + 101, color, false);
                graphics.drawString(font, Component.translatable(
                        "screen.economia.claim.land_debt", state.landDebt()), leftPos + 22, topPos + 117, color, false);
                graphics.drawString(font, Component.translatable("screen.economia.claim.next_chunk_price",
                        state.nextChunkPrice()), leftPos + 22, topPos + 133, color, false);
            }
        } else if (page == Page.PAYMENT) {
            graphics.drawString(font, Component.translatable("screen.economia.claim.payment_value", state.landPrice()),
                    leftPos + 22, topPos + 36, color, false);
            if (paymentMethod == DirectPaymentMethod.CASH) {
                graphics.drawString(font, Component.translatable("screen.economia.claim.cash_exact"),
                        leftPos + 22, topPos + 72, color, false);
                for (int slot = 0; slot < 6; slot++) {
                    InventorySlotRenderer.draw(graphics, leftPos + 111 + (slot % 3) * 18,
                            topPos + 82 + (slot / 3) * 18);
                }
            } else {
                graphics.drawString(font, Component.translatable("screen.economia.claim.card_required"),
                        leftPos + 22, topPos + 76, color, false);
                InventorySlotRenderer.draw(graphics, leftPos + 129, topPos + 88);
            }
            drawPlayerInventory(graphics);
        } else if (page == Page.TAXES) {
            graphics.drawString(font, Component.translatable("screen.economia.claim.current_tax", state.currentTax()),
                    leftPos + 24, topPos + 48, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.total_tax", state.totalTax()),
                    leftPos + 24, topPos + 66, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.tax_count", state.taxCount()),
                    leftPos + 24, topPos + 84, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.tax_period", state.taxPeriodDays()),
                    leftPos + 24, topPos + 100, color, false);
        } else if (page == Page.SALE) {
            graphics.drawString(font, Component.translatable("screen.economia.claim.buyer"), leftPos + 24, topPos + 64, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.value"), leftPos + 24, topPos + 92, color, false);
        } else {
            int row = 0;
            for (var member : state.privateMembers()) {
                if (row >= 3) break;
                graphics.drawString(font, font.plainSubstrByWidth(member.playerName(), 108),
                        leftPos + 24, topPos + 78 + row * 24, color, false);
                row++;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component pageTitle = switch (page) {
            case TAXES -> Component.translatable("screen.economia.claim.taxes_title");
            case SALE -> Component.translatable("screen.economia.claim.sale_title");
            case MEMBER -> Component.translatable("screen.economia.claim.member_title");
            case PAYMENT -> Component.translatable("screen.economia.claim.payment_title");
            default -> title;
        };
        graphics.drawString(font, pageTitle, 12, 12, 0xFFFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawPlayerInventory(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                InventorySlotRenderer.draw(graphics, leftPos + 58 + column * 18, topPos + 156 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            InventorySlotRenderer.draw(graphics, leftPos + 58 + column * 18, topPos + 214);
        }
    }
}
