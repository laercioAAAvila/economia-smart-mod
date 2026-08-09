package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.ChatChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatChannelPayload(ChatChannel channel) implements CustomPacketPayload {
    public static final Type<ChatChannelPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "chat_channel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatChannelPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ChatChannelPayload decode(RegistryFriendlyByteBuf buffer) { return new ChatChannelPayload(ChatChannel.byId(buffer.readVarInt())); }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ChatChannelPayload payload) { buffer.writeVarInt(payload.channel().ordinal()); }
    };
    @Override public Type<ChatChannelPayload> type() { return TYPE; }
}
