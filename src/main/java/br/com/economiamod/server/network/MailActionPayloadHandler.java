package br.com.economiamod.server.network;

import br.com.economiamod.common.menu.MailMenu;
import br.com.economiamod.common.network.MailActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MailActionPayloadHandler {
    private MailActionPayloadHandler() {
    }

    public static void handle(MailActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.containerMenu instanceof MailMenu mailMenu)) {
            return;
        }

        switch (payload.action()) {
            case SET_NAME -> mailMenu.setInitialName(serverPlayer, payload.text());
            case ADD_RECIPIENT -> mailMenu.addRecipient(serverPlayer, payload.text());
            case DELETE_RECIPIENT -> mailMenu.deleteRecipient(serverPlayer, payload.targetId());
            case SELECT_RECIPIENT -> mailMenu.selectRecipient(payload.targetId());
            case PAY_CASH -> mailMenu.payCash(serverPlayer, payload.requestId(), false);
            case PAY_CARD -> mailMenu.payCard(serverPlayer, payload.requestId(), payload.creditPayment());
            case CONFIRM_CHANGE_TO_OWNER -> mailMenu.payCash(serverPlayer, payload.requestId(), true);
            case SEND -> mailMenu.sendShipment(serverPlayer);
            case REFRESH -> mailMenu.syncState(serverPlayer);
            case OPEN_PAYMENT -> mailMenu.openPayment();
            case CLOSE_PAYMENT -> mailMenu.closePayment();
            case RETURN_TO_PAYMENT -> mailMenu.returnToPayment(serverPlayer);
        }
    }
}
