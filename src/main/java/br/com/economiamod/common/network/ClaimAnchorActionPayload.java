package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClaimAnchorActionPayload(ClaimAnchorAction action, String text, long amount, int days,
                                       UUID requestId) implements CustomPacketPayload {
    public static final Type<ClaimAnchorActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "claim_anchor_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimAnchorActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ClaimAnchorActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ClaimAnchorActionPayload(ClaimAnchorAction.byId(buffer.readVarInt()),
                    buffer.readUtf(64), buffer.readLong(), buffer.readVarInt(), buffer.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ClaimAnchorActionPayload payload) {
            buffer.writeVarInt(payload.action().ordinal());
            buffer.writeUtf(payload.text() == null ? "" : payload.text(), 64);
            buffer.writeLong(payload.amount());
            buffer.writeVarInt(Math.max(0, payload.days()));
            buffer.writeUUID(payload.requestId() == null ? UUID.randomUUID() : payload.requestId());
        }
    };

    @Override
    public Type<ClaimAnchorActionPayload> type() {
        return TYPE;
    }
}
