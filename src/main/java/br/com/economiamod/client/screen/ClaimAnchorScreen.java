package br.com.economiamod.client.screen;

import br.com.economiamod.common.group.GroupType;
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
    private enum Page { INFO, ANCHOR, SALE, MEMBER }

    private final List<AbstractWidget> widgets = new ArrayList<>();
    private Page page = Page.INFO;
    private EditBox textInput;
    private EditBox valueInput;

    public ClaimAnchorScreen(ClaimAnchorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 276;
        imageHeight = 190;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        for (AbstractWidget widget : widgets) removeWidget(widget);
        widgets.clear();
        textInput = null;
        valueInput = null;
        ClaimAnchorMenuState state = menu.state();
        if (page == Page.INFO) {
            if (!state.active()) {
                Button claim = button(38, 128, 96, "screen.economia.claim.claim", () -> send(ClaimAnchorAction.CLAIM, "", 0L, 0));
                claim.active = state.canClaim();
                button(142, 128, 96, "screen.economia.claim.exit", this::onClose);
            } else {
                if (state.canManage()) {
                    button(18, 128, 72, "screen.economia.claim.anchor", () -> open(Page.ANCHOR));
                    button(98, 128, 78, "screen.economia.claim.sell", () -> open(Page.SALE));
                    if (state.groupType() == GroupType.PRIVATE_PROPERTY) {
                        button(184, 128, 74, "screen.economia.claim.invite", () -> open(Page.MEMBER));
                    }
                }
                if (state.canManage()) {
                    button(18, 153, 120, "screen.economia.claim.reissue", () -> send(
                            ClaimAnchorAction.REISSUE_INVOICES, "", 0L, 0));
                }
                button(184, 153, 72, "screen.economia.claim.exit", this::onClose);
            }
        } else if (page == Page.ANCHOR) {
            int availableDays = Math.max(0, state.maxAnchorDays() - state.anchorDaysRemaining());
            valueInput = input(146, 86, 90, Integer.toString(Math.min(state.defaultAnchorDays(), availableDays)));
            valueInput.setFilter(value -> value.matches("[0-9]*"));
            Button generate = button(64, 119, 112, "screen.economia.claim.generate_invoice", this::sendAnchor);
            generate.active = availableDays > 0;
            back();
        } else if (page == Page.SALE) {
            textInput = input(90, 58, 146, "");
            valueInput = input(90, 86, 146, Long.toString(state.suggestedSalePrice()));
            valueInput.setFilter(value -> value.matches("[0-9]*"));
            button(64, 119, 112, "screen.economia.claim.generate_invoice", this::sendSale);
            back();
        } else {
            textInput = input(78, 72, 158, "");
            button(64, 105, 112, "screen.economia.claim.invite", () -> send(
                    ClaimAnchorAction.INVITE_MEMBER, textInput.getValue(), 0L, 0));
            back();
        }
    }

    private void sendAnchor() {
        try {
            send(ClaimAnchorAction.ANCHOR_INVOICE, "", 0L, Integer.parseInt(valueInput.getValue()));
        } catch (NumberFormatException ignored) {
        }
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
        if (page == Page.INFO) {
            graphics.drawString(font, Component.translatable("screen.economia.claim.location",
                    state.blockX(), state.blockY(), state.blockZ()), leftPos + 22, topPos + 45, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.land_price", state.landPrice()),
                    leftPos + 22, topPos + 65, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.territories",
                    state.territoryCount(), state.territoryLimit()), leftPos + 22, topPos + 85, color, false);
            if (state.active()) graphics.drawString(font, Component.translatable(
                    "screen.economia.claim.land_debt", state.landDebt()), leftPos + 22, topPos + 105, color, false);
        } else if (page == Page.ANCHOR) {
            graphics.drawString(font, Component.translatable("screen.economia.claim.land_price", state.landPrice()),
                    leftPos + 24, topPos + 44, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.anchor_price", state.anchorPrice()),
                    leftPos + 24, topPos + 59, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.days_current", state.anchorDaysRemaining()),
                    leftPos + 24, topPos + 74, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.days_requested"),
                    leftPos + 24, topPos + 92, color, false);
        } else if (page == Page.SALE) {
            graphics.drawString(font, Component.translatable("screen.economia.claim.buyer"), leftPos + 24, topPos + 64, color, false);
            graphics.drawString(font, Component.translatable("screen.economia.claim.value"), leftPos + 24, topPos + 92, color, false);
        } else {
            graphics.drawString(font, Component.translatable("screen.economia.claim.player"), leftPos + 24, topPos + 78, color, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component pageTitle = switch (page) {
            case ANCHOR -> Component.translatable("screen.economia.claim.anchor_title");
            case SALE -> Component.translatable("screen.economia.claim.sale_title");
            case MEMBER -> Component.translatable("screen.economia.claim.member_title");
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
}
