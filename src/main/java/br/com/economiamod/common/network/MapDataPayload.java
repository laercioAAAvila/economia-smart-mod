package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.group.ChatChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapDataPayload(List<LocationSummary> locations, List<ClaimSummary> claims,
                             boolean hasClan, boolean hasPrivateProperty, ChatChannel selectedChannel) implements CustomPacketPayload {
    private static final int MAX_LOCATIONS = 128;
    private static final int MAX_CLAIMS = 512;
    public static final Type<MapDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "map_data"));

    public MapDataPayload {
        locations = locations == null ? List.of() : List.copyOf(locations);
        claims = claims == null ? List.of() : List.copyOf(claims);
        selectedChannel = selectedChannel == null ? ChatChannel.GENERAL : selectedChannel;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MapDataPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MapDataPayload decode(RegistryFriendlyByteBuf buffer) {
            int locationCount = checkedSize(buffer.readVarInt(), MAX_LOCATIONS);
            List<LocationSummary> locations = new ArrayList<>(locationCount);
            for (int index = 0; index < locationCount; index++) {
                locations.add(new LocationSummary(buffer.readUUID(), buffer.readUtf(64), buffer.readUtf(255),
                        buffer.readInt(), buffer.readInt(), buffer.readInt()));
            }
            int claimCount = checkedSize(buffer.readVarInt(), MAX_CLAIMS);
            List<ClaimSummary> claims = new ArrayList<>(claimCount);
            for (int index = 0; index < claimCount; index++) {
                claims.add(new ClaimSummary(buffer.readUUID(), GroupType.values()[buffer.readVarInt()],
                        buffer.readUtf(255), buffer.readInt(), buffer.readInt()));
            }
            boolean hasClan = buffer.readBoolean();
            boolean hasPrivateProperty = buffer.readBoolean();
            ChatChannel selectedChannel = ChatChannel.byId(buffer.readVarInt());
            return new MapDataPayload(locations, claims, hasClan, hasPrivateProperty, selectedChannel);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MapDataPayload payload) {
            int locationCount = Math.min(MAX_LOCATIONS, payload.locations().size());
            buffer.writeVarInt(locationCount);
            for (int index = 0; index < locationCount; index++) {
                LocationSummary location = payload.locations().get(index);
                buffer.writeUUID(location.id());
                buffer.writeUtf(location.name(), 64);
                buffer.writeUtf(location.dimension(), 255);
                buffer.writeInt(location.x());
                buffer.writeInt(location.y());
                buffer.writeInt(location.z());
            }
            int claimCount = Math.min(MAX_CLAIMS, payload.claims().size());
            buffer.writeVarInt(claimCount);
            for (int index = 0; index < claimCount; index++) {
                ClaimSummary claim = payload.claims().get(index);
                buffer.writeUUID(claim.groupId());
                buffer.writeVarInt(claim.type().ordinal());
                buffer.writeUtf(claim.dimension(), 255);
                buffer.writeInt(claim.chunkX());
                buffer.writeInt(claim.chunkZ());
            }
            buffer.writeBoolean(payload.hasClan());
            buffer.writeBoolean(payload.hasPrivateProperty());
            buffer.writeVarInt(payload.selectedChannel().ordinal());
        }

        private int checkedSize(int value, int maximum) {
            if (value < 0 || value > maximum) {
                throw new IllegalArgumentException("Invalid map payload list size: " + value);
            }
            return value;
        }
    };

    @Override
    public Type<MapDataPayload> type() {
        return TYPE;
    }

    public record LocationSummary(UUID id, String name, String dimension, int x, int y, int z) {
    }

    public record ClaimSummary(UUID groupId, GroupType type, String dimension, int chunkX, int chunkZ) {
    }
}
