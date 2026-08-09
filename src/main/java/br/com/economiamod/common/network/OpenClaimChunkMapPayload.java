package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.GroupType;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenClaimChunkMapPayload(UUID anchorId, GroupType groupType, String dimension,
                                       int centerBlockX, int centerBlockZ, long initialChunkPrice)
        implements CustomPacketPayload {
    public static final Type<OpenClaimChunkMapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "open_claim_chunk_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenClaimChunkMapPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public OpenClaimChunkMapPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new OpenClaimChunkMapPayload(buffer.readUUID(), GroupType.values()[buffer.readVarInt()],
                            buffer.readUtf(255), buffer.readInt(), buffer.readInt(), buffer.readLong());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, OpenClaimChunkMapPayload payload) {
                    buffer.writeUUID(payload.anchorId());
                    buffer.writeVarInt(payload.groupType().ordinal());
                    buffer.writeUtf(payload.dimension(), 255);
                    buffer.writeInt(payload.centerBlockX());
                    buffer.writeInt(payload.centerBlockZ());
                    buffer.writeLong(payload.initialChunkPrice());
                }
            };

    @Override
    public Type<OpenClaimChunkMapPayload> type() {
        return TYPE;
    }
}

