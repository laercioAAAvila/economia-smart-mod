package br.com.economiamod.server.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.network.ChatChannelPayload;
import br.com.economiamod.common.network.ShareLocationPayload;
import br.com.economiamod.server.group.GroupChatService;
import java.sql.SQLException;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ChatChannelPayloadHandler {
    private ChatChannelPayloadHandler() {}

    public static void handle(ChatChannelPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> {
                try { GroupChatService.INSTANCE.select(player, payload.channel()); }
                catch (SQLException exception) { EconomiaMod.LOGGER.warn("Falha ao trocar canal.", exception); }
            });
        }
    }

    public static void handleShare(ShareLocationPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> {
                try { GroupChatService.INSTANCE.share(player, payload.locationId(), payload.channel()); }
                catch (SQLException exception) { EconomiaMod.LOGGER.warn("Falha ao compartilhar localização.", exception); }
            });
        }
    }
}
