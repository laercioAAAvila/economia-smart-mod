package br.com.economiamod.server.group;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.ChatChannel;
import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.server.location.PlayerLocation;
import br.com.economiamod.server.location.PlayerLocationRepository;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;

public final class GroupChatService {
    public static final GroupChatService INSTANCE = new GroupChatService();
    private final Map<UUID, ChatChannel> selected = new ConcurrentHashMap<>();
    private final GroupRepository groups = new GroupRepository();
    private final PlayerLocationRepository locations = new PlayerLocationRepository();
    private final PrivatePropertyAccessService privateProperties = new PrivatePropertyAccessService();

    private GroupChatService() {}

    public ChatChannel selected(UUID playerUuid) {
        return selected.getOrDefault(playerUuid, ChatChannel.GENERAL);
    }

    public boolean select(ServerPlayer player, ChatChannel channel) throws SQLException {
        if (channel == ChatChannel.CLAN && groups.membership(player.getUUID(), GroupType.CLAN).isEmpty()) return false;
        if (channel == ChatChannel.PRIVATE_PROPERTY && !privateProperties.hasAccess(player.getUUID())) return false;
        selected.put(player.getUUID(), channel);
        player.displayClientMessage(Component.translatable("chat.economia.channel." + channel.translationSuffix()), true);
        return true;
    }

    public void logout(UUID playerUuid) {
        selected.remove(playerUuid);
    }

    public void onChat(ServerChatEvent event) {
        ChatChannel channel = selected(event.getPlayer().getUUID());
        if (channel == ChatChannel.GENERAL) {
            return;
        }
        event.setCanceled(true);
        try {
            if (channel == ChatChannel.PRIVATE_PROPERTY) {
                sendPrivateMessage(event);
                return;
            }
            GroupType type = channel == ChatChannel.CLAN ? GroupType.CLAN : GroupType.PRIVATE_PROPERTY;
            GroupMembership senderMembership = groups.membership(event.getPlayer().getUUID(), type).orElse(null);
            if (senderMembership == null) {
                selected.put(event.getPlayer().getUUID(), ChatChannel.GENERAL);
                return;
            }
            Component message = Component.literal("[CLÃ] ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(event.getUsername() + ": ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(event.getRawText()));
            for (ServerPlayer target : event.getPlayer().getServer().getPlayerList().getPlayers()) {
                if (groups.membership(target.getUUID(), senderMembership.groupId()).isPresent()) {
                    target.sendSystemMessage(message);
                }
            }
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao enviar chat de grupo.", exception);
        }
    }

    public void share(ServerPlayer sender, UUID locationId, ChatChannel channel) throws SQLException {
        PlayerLocation location = locations.find(locationId).filter(value -> value.playerUuid().equals(sender.getUUID())).orElse(null);
        if (location == null) return;
        if (channel == ChatChannel.CLAN && groups.membership(sender.getUUID(), GroupType.CLAN).isEmpty()) return;
        if (channel == ChatChannel.PRIVATE_PROPERTY && !privateProperties.hasAccess(sender.getUUID())) return;
        Component message = Component.literal("[Localização] " + location.name() + "\nX: " + location.x() + " Z: " + location.z())
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/economia _localizacao " + location.id())));
        if (channel == ChatChannel.GENERAL) {
            sender.getServer().getPlayerList().broadcastSystemMessage(message, false);
            return;
        }
        if (channel == ChatChannel.PRIVATE_PROPERTY) {
            sendToPrivateRelations(sender, message);
            return;
        }
        GroupType type = channel == ChatChannel.CLAN ? GroupType.CLAN : GroupType.PRIVATE_PROPERTY;
        GroupMembership membership = groups.membership(sender.getUUID(), type).orElse(null);
        if (membership == null) return;
        for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
            if (groups.membership(target.getUUID(), membership.groupId()).isPresent()) target.sendSystemMessage(message);
        }
    }

    private void sendPrivateMessage(ServerChatEvent event) throws SQLException {
        if (!privateProperties.hasAccess(event.getPlayer().getUUID())) {
            selected.put(event.getPlayer().getUUID(), ChatChannel.GENERAL);
            return;
        }
        Component message = Component.literal("[PRIVADO] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(event.getUsername() + ": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(event.getRawText()));
        sendToPrivateRelations(event.getPlayer(), message);
    }

    private void sendToPrivateRelations(ServerPlayer sender, Component message) throws SQLException {
        var recipients = privateProperties.relatedPlayers(sender.getUUID());
        for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
            if (recipients.contains(target.getUUID())) {
                target.sendSystemMessage(message);
            }
        }
    }
}
