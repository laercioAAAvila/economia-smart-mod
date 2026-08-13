package br.com.economiamod.server.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.claim.ClaimRecord;
import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.network.MapActionPayload;
import br.com.economiamod.common.network.MapDataPayload;
import br.com.economiamod.server.claim.ClaimRepository;
import br.com.economiamod.server.claim.ClaimService;
import br.com.economiamod.server.claim.ClaimOperationResult;
import br.com.economiamod.server.claim.ClaimChunkCardPurchaseService;
import br.com.economiamod.server.group.GroupRepository;
import br.com.economiamod.server.group.GroupChatService;
import br.com.economiamod.server.group.PrivatePropertyAccessService;
import br.com.economiamod.server.location.PlayerLocation;
import br.com.economiamod.server.location.PlayerLocationRepository;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MapActionPayloadHandler {
    private static final PlayerLocationRepository LOCATIONS = new PlayerLocationRepository();
    private static final ClaimRepository CLAIMS = new ClaimRepository();
    private static final GroupRepository GROUPS = new GroupRepository();
    private static final ClaimService CLAIM_SERVICE = new ClaimService();
    private static final PrivatePropertyAccessService PRIVATE_PROPERTIES = new PrivatePropertyAccessService();
    private static final ClaimChunkCardPurchaseService CHUNK_PURCHASES = new ClaimChunkCardPurchaseService();

    private MapActionPayloadHandler() {
    }

    public static void handle(MapActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer) || !EconomyDatabaseState.isAvailable()) {
            return;
        }
        context.enqueueWork(() -> {
            try {
                int centerChunkX = serverPlayer.chunkPosition().x;
                int centerChunkZ = serverPlayer.chunkPosition().z;
                switch (payload.action()) {
                    case REFRESH -> {
                        centerChunkX = payload.x();
                        centerChunkZ = payload.z();
                    }
                    case SAVE_LOCATION -> {
                        LOCATIONS.save(serverPlayer.getUUID(), payload.name(), payload.dimension(),
                                payload.x(), payload.y(), payload.z());
                        centerChunkX = payload.x() >> 4;
                        centerChunkZ = payload.z() >> 4;
                    }
                    case UPDATE_LOCATION -> {
                        LOCATIONS.update(serverPlayer.getUUID(), payload.targetId(), payload.name(),
                                payload.dimension(), payload.x(), payload.y(), payload.z());
                        centerChunkX = payload.x() >> 4;
                        centerChunkZ = payload.z() >> 4;
                    }
                    case DELETE_LOCATION -> LOCATIONS.delete(serverPlayer.getUUID(), payload.targetId());
                    case TOGGLE_CLAIM -> {
                        toggleClaim(serverPlayer, payload.name(), payload.x(), payload.z());
                        centerChunkX = payload.x();
                        centerChunkZ = payload.z();
                    }
                    case PURCHASE_CLAIM -> {
                        purchaseClaim(serverPlayer, payload);
                        centerChunkX = payload.x();
                        centerChunkZ = payload.z();
                    }
                }
                sync(serverPlayer, centerChunkX, centerChunkZ);
            } catch (SQLException | IllegalArgumentException exception) {
                EconomiaMod.LOGGER.warn("Falha ao processar ação do mapa.", exception);
            }
        });
    }

    private static void purchaseClaim(ServerPlayer player, MapActionPayload payload) throws SQLException {
        String currentDimension = player.serverLevel().dimension().location().toString();
        if (!currentDimension.equals(payload.dimension())) {
            player.displayClientMessage(Component.translatable("claim.economia.error.invalid_dimension"), true);
            return;
        }
        ClaimOperationResult result = CHUNK_PURCHASES.purchase(player, payload.targetId(),
                currentDimension, payload.x(), payload.z());
        if (!result.success()) {
            player.displayClientMessage(Component.translatable("claim.economia.error." + result.code()), true);
            return;
        }
        player.displayClientMessage(Component.translatable("claim.economia.chunk_purchased"), true);
    }

    private static void toggleClaim(ServerPlayer player, String requestedChannel,
                                    int chunkX, int chunkZ) throws SQLException {
        GroupMembership clan = GROUPS.membership(player.getUUID(), GroupType.CLAN)
                .filter(member -> member.role() == GroupRole.LEADER).orElse(null);
        GroupMembership property = GROUPS.membership(player.getUUID(), GroupType.PRIVATE_PROPERTY)
                .filter(member -> member.role() == GroupRole.OWNER).orElse(null);
        GroupMembership controller = "PRIVATE_PROPERTY".equals(requestedChannel) ? property
                : "CLAN".equals(requestedChannel) ? clan : clan != null ? clan : property;
        if (controller == null) {
            player.displayClientMessage(Component.translatable("claim.economia.permission_denied"), true);
            return;
        }
        ClaimOperationResult result = CLAIM_SERVICE.toggleChunk(player.getUUID(), controller.groupId(),
                player.serverLevel().dimension().location().toString(), chunkX, chunkZ);
        player.displayClientMessage(Component.translatable(result.success()
                ? "claim.economia.map_updated" : "claim.economia.error." + result.code()), true);
    }

    private static void sync(ServerPlayer player, int centerChunkX, int centerChunkZ) throws SQLException {
        List<MapDataPayload.LocationSummary> locations = new ArrayList<>();
        for (PlayerLocation location : LOCATIONS.list(player.getUUID())) {
            locations.add(new MapDataPayload.LocationSummary(location.id(), location.name(), location.dimension(),
                    location.x(), location.y(), location.z()));
        }
        List<MapDataPayload.ClaimSummary> claims = new ArrayList<>();
        String dimension = player.serverLevel().dimension().location().toString();
        for (ClaimRecord claim : CLAIMS.claimsAround(dimension, centerChunkX, centerChunkZ, 64)) {
            claims.add(new MapDataPayload.ClaimSummary(claim.groupId(), claim.groupType(), claim.dimension(),
                    claim.chunkX(), claim.chunkZ()));
        }
        boolean hasClan = GROUPS.membership(player.getUUID(), GroupType.CLAN).isPresent();
        boolean hasPrivateProperty = PRIVATE_PROPERTIES.hasAccess(player.getUUID());
        var selectedChannel = GroupChatService.INSTANCE.selected(player.getUUID());
        if (selectedChannel == br.com.economiamod.common.group.ChatChannel.CLAN && !hasClan
                || selectedChannel == br.com.economiamod.common.group.ChatChannel.PRIVATE_PROPERTY
                && !hasPrivateProperty) {
            GroupChatService.INSTANCE.logout(player.getUUID());
            selectedChannel = br.com.economiamod.common.group.ChatChannel.GENERAL;
        }
        PacketDistributor.sendToPlayer(player, new MapDataPayload(locations, claims, hasClan, hasPrivateProperty,
                selectedChannel));
    }
}
