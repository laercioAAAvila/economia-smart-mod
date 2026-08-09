package br.com.economiamod.server.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.invoice.ClaimInvoiceItemDataService;
import br.com.economiamod.common.menu.ClaimAnchorMenu;
import br.com.economiamod.common.network.ClaimAnchorActionPayload;
import br.com.economiamod.common.network.OpenClaimChunkMapPayload;
import br.com.economiamod.server.claim.ClaimAnchorMenuState;
import br.com.economiamod.server.claim.ClaimAnchorMenuStateService;
import br.com.economiamod.server.claim.ClaimDirectPaymentService;
import br.com.economiamod.server.claim.ClaimInvoiceRecord;
import br.com.economiamod.server.claim.ClaimInvoiceResult;
import br.com.economiamod.server.claim.ClaimInvoiceService;
import br.com.economiamod.server.claim.ClaimOperationResult;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.config.EconomyServerConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClaimAnchorActionPayloadHandler {
    private static final ClaimDirectPaymentService DIRECT_PAYMENTS = new ClaimDirectPaymentService();
    private static final ClaimInvoiceService INVOICES = new ClaimInvoiceService();
    private static final ClaimAnchorMenuStateService STATES = new ClaimAnchorMenuStateService();
    private static final ClaimInvoiceItemDataService ITEMS = new ClaimInvoiceItemDataService();

    private ClaimAnchorActionPayloadHandler() {
    }

    public static void handle(ClaimAnchorActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ClaimAnchorMenu menu)) {
            return;
        }
        context.enqueueWork(() -> process(player, menu, payload));
    }

    private static void process(ServerPlayer player, ClaimAnchorMenu menu, ClaimAnchorActionPayload payload) {
        try {
            ClaimAnchorMenuState fresh = STATES.state(player.getUUID(), menu.state().anchorId());
            switch (payload.action()) {
                case CLAIM, OPEN_PAYMENT -> openPayment(player, menu, fresh);
                case SET_PAYMENT_MODE -> setPaymentMode(player, menu, payload.text());
                case PAY_CLAIM -> payClaim(player, menu, fresh, payload);
                case CLOSE_PAYMENT -> menu.setPaymentMode(player, false, false);
                case ANCHOR_INVOICE -> anchorInvoice(player, fresh, payload.days());
                case SALE_INVOICE -> saleInvoice(player, fresh, payload.text(), payload.amount());
                case INVITE_MEMBER -> inviteMember(player, fresh, payload.text());
                case REISSUE_INVOICES -> reissueInvoices(player, fresh);
                case OPEN_CHUNK_MAP -> openChunkMap(player, fresh);
            }
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao processar menu do claim.", exception);
            player.displayClientMessage(Component.translatable("commands.economia.unavailable"), true);
        }
    }

    private static void openChunkMap(ServerPlayer player, ClaimAnchorMenuState state) {
        if (!state.active() || !state.canManage() || !state.canBuyChunk()) {
            denied(player, state.chunkCount() >= state.chunkLimit() ? "claim_limit" : "owner_required");
            return;
        }
        player.closeContainer();
        PacketDistributor.sendToPlayer(player, new OpenClaimChunkMapPayload(state.anchorId(), state.groupType(),
                state.dimension(), state.blockX(), state.blockZ(), state.nextChunkPrice()));
    }

    private static void openPayment(ServerPlayer player, ClaimAnchorMenu menu, ClaimAnchorMenuState state) {
        if (!state.canClaim()) {
            denied(player, "claim_limit");
            return;
        }
        menu.setPaymentMode(player, true, false);
    }

    private static void setPaymentMode(ServerPlayer player, ClaimAnchorMenu menu, String value) {
        if (!menu.paymentOpen()) {
            denied(player, "payment_closed");
            return;
        }
        DirectPaymentMethod method = DirectPaymentMethod.parse(value);
        menu.setPaymentMode(player, true, method == DirectPaymentMethod.CASH);
    }

    private static void payClaim(ServerPlayer player, ClaimAnchorMenu menu, ClaimAnchorMenuState state,
                                 ClaimAnchorActionPayload payload) throws SQLException {
        if (!menu.paymentOpen()) {
            denied(player, "payment_closed");
            return;
        }
        DirectPaymentMethod method = DirectPaymentMethod.parse(payload.text());
        if ((method == DirectPaymentMethod.CASH) != menu.cashMode()) {
            denied(player, "payment_mode_invalid");
            return;
        }
        ClaimOperationResult result = DIRECT_PAYMENTS.payAndActivate(player, state.anchorId(), method,
                menu.paymentCard(), menu.paymentCash(), payload.amount(), payload.requestId());
        if (!result.success()) {
            denied(player, result.code());
            if ("payment_price_changed".equals(result.code())) {
                player.closeContainer();
            }
            return;
        }
        player.displayClientMessage(Component.translatable("claim.economia.activated"), true);
        player.closeContainer();
    }

    private static void anchorInvoice(ServerPlayer player, ClaimAnchorMenuState state, int days) throws SQLException {
        if (!state.active() || state.territoryId() == null) {
            denied(player, "claim_required");
            return;
        }
        ClaimInvoiceResult result = INVOICES.generateAnchorInvoice(player.getUUID(), state.territoryId(), days);
        handleGenerated(player, result);
    }

    private static void saleInvoice(ServerPlayer seller, ClaimAnchorMenuState state,
                                    String buyerName, long amount) throws SQLException {
        if (!state.active() || state.territoryId() == null || seller.getServer() == null) {
            denied(seller, "claim_required");
            return;
        }
        ServerPlayer buyer = seller.getServer().getPlayerList().getPlayerByName(buyerName == null ? "" : buyerName.strip());
        if (buyer == null) {
            denied(seller, "buyer_offline");
            return;
        }
        ClaimInvoiceResult result = INVOICES.generateSaleInvoice(
                seller.getUUID(), state.territoryId(), buyer.getUUID(), amount);
        if (!result.success()) {
            denied(seller, result.code());
            return;
        }
        giveInvoice(buyer, result.invoiceId());
        seller.displayClientMessage(Component.translatable("claim.economia.sale_invoice_generated"), true);
        buyer.displayClientMessage(Component.translatable("claim.economia.sale_invoice_received"), true);
    }

    private static void inviteMember(ServerPlayer owner, ClaimAnchorMenuState state, String targetName) throws SQLException {
        if (!state.active() || state.groupType() != GroupType.PRIVATE_PROPERTY || !state.canManage()
                || owner.getServer() == null) {
            denied(owner, "owner_required");
            return;
        }
        ServerPlayer target = owner.getServer().getPlayerList().getPlayerByName(targetName == null ? "" : targetName.strip());
        if (target == null || target.getUUID().equals(owner.getUUID())) {
            denied(owner, "target_offline");
            return;
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement count = connection.prepareStatement("""
                     SELECT COUNT(*) FROM economy_private_property_members WHERE territory_id = ?
                     """)) {
            count.setObject(1, state.territoryId());
            try (var resultSet = count.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) >= EconomyServerConfig.PRIVATE_PROPERTY_MEMBER_LIMIT.get()) {
                    denied(owner, "member_limit");
                    return;
                }
            }
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO economy_private_property_members(
                         territory_id, player_uuid, invited_by_player_uuid, created_at
                     ) VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                     ON CONFLICT (territory_id, player_uuid) DO NOTHING
                     """)) {
            statement.setObject(1, state.territoryId());
            statement.setObject(2, target.getUUID());
            statement.setObject(3, owner.getUUID());
            statement.executeUpdate();
        }
        owner.displayClientMessage(Component.translatable("claim.economia.member_invited", target.getName()), true);
        target.displayClientMessage(Component.translatable("claim.economia.member_added", owner.getName()), true);
    }

    private static void reissueInvoices(ServerPlayer player, ClaimAnchorMenuState state) throws SQLException {
        if (!state.active() || state.territoryId() == null || !state.canManage()) {
            denied(player, "owner_required");
            return;
        }
        int issued = 0;
        for (ClaimInvoiceRecord invoice : INVOICES.reissuePending(player.getUUID(), state.territoryId())) {
            ItemStack stack = ITEMS.create(invoice.id(), invoice.amount(), invoice.invoiceType());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            issued++;
        }
        player.displayClientMessage(Component.translatable(
                issued == 0 ? "claim.economia.no_pending_invoices" : "claim.economia.invoices_reissued", issued), true);
    }

    private static void handleGenerated(ServerPlayer player, ClaimInvoiceResult result) throws SQLException {
        if (!result.success()) {
            denied(player, result.code());
            return;
        }
        giveInvoice(player, result.invoiceId());
        player.displayClientMessage(Component.translatable("claim.economia.invoice_generated", result.amount()), true);
    }

    private static void giveInvoice(ServerPlayer player, UUID invoiceId) throws SQLException {
        ClaimInvoiceRecord invoice = INVOICES.invoice(invoiceId);
        if (invoice == null) {
            return;
        }
        ItemStack stack = ITEMS.create(invoice.id(), invoice.amount(), invoice.invoiceType());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void denied(ServerPlayer player, String code) {
        player.displayClientMessage(Component.translatable("claim.economia.error." + code), true);
    }
}
