package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GroupActionPayload(GroupAction action, String text, UUID targetId, int intValue,
                                 long amount, boolean firstFlag, boolean secondFlag, UUID requestId)
        implements CustomPacketPayload {
    public static final Type<GroupActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "group_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GroupActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GroupActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new GroupActionPayload(GroupAction.byId(buffer.readVarInt()), buffer.readUtf(64), buffer.readUUID(),
                    buffer.readVarInt(), buffer.readLong(), buffer.readBoolean(), buffer.readBoolean(), buffer.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GroupActionPayload payload) {
            buffer.writeVarInt(payload.action().ordinal());
            buffer.writeUtf(payload.text() == null ? "" : payload.text(), 64);
            buffer.writeUUID(payload.targetId() == null ? new UUID(0L, 0L) : payload.targetId());
            buffer.writeVarInt(Math.max(0, payload.intValue()));
            buffer.writeLong(payload.amount());
            buffer.writeBoolean(payload.firstFlag());
            buffer.writeBoolean(payload.secondFlag());
            buffer.writeUUID(payload.requestId() == null ? UUID.randomUUID() : payload.requestId());
        }
    };

    public static GroupActionPayload simple(GroupAction action) {
        return new GroupActionPayload(action, "", new UUID(0L, 0L), 0, 0L, false, false, UUID.randomUUID());
    }

    @Override
    public Type<GroupActionPayload> type() {
        return TYPE;
    }
}
