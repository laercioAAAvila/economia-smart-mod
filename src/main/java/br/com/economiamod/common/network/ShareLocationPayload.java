package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.ChatChannel;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShareLocationPayload(UUID locationId, ChatChannel channel) implements CustomPacketPayload {
    public static final Type<ShareLocationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "share_location"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShareLocationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ShareLocationPayload decode(RegistryFriendlyByteBuf buffer) { return new ShareLocationPayload(buffer.readUUID(), ChatChannel.byId(buffer.readVarInt())); }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ShareLocationPayload payload) { buffer.writeUUID(payload.locationId()); buffer.writeVarInt(payload.channel().ordinal()); }
    };
    @Override public Type<ShareLocationPayload> type() { return TYPE; }
}
