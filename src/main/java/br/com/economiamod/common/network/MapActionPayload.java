package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapActionPayload(MapAction action, String name, String dimension, int x, int y, int z, UUID targetId)
        implements CustomPacketPayload {
    public static final Type<MapActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "map_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MapActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new MapActionPayload(MapAction.byId(buffer.readVarInt()), buffer.readUtf(64), buffer.readUtf(255),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MapActionPayload payload) {
            buffer.writeVarInt(payload.action().ordinal());
            buffer.writeUtf(payload.name() == null ? "" : payload.name(), 64);
            buffer.writeUtf(payload.dimension() == null ? "" : payload.dimension(), 255);
            buffer.writeInt(payload.x());
            buffer.writeInt(payload.y());
            buffer.writeInt(payload.z());
            buffer.writeUUID(payload.targetId() == null ? new UUID(0L, 0L) : payload.targetId());
        }
    };

    public static MapActionPayload refresh() {
        return new MapActionPayload(MapAction.REFRESH, "", "", 0, 0, 0, new UUID(0L, 0L));
    }

    @Override
    public Type<MapActionPayload> type() {
        return TYPE;
    }
}
