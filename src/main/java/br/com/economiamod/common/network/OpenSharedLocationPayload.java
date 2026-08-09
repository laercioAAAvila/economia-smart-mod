package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenSharedLocationPayload(String name, String dimension, int x, int y, int z) implements CustomPacketPayload {
    public static final Type<OpenSharedLocationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "open_shared_location"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSharedLocationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public OpenSharedLocationPayload decode(RegistryFriendlyByteBuf buffer) { return new OpenSharedLocationPayload(buffer.readUtf(64), buffer.readUtf(255), buffer.readInt(), buffer.readInt(), buffer.readInt()); }
        @Override public void encode(RegistryFriendlyByteBuf buffer, OpenSharedLocationPayload payload) { buffer.writeUtf(payload.name(), 64); buffer.writeUtf(payload.dimension(), 255); buffer.writeInt(payload.x()); buffer.writeInt(payload.y()); buffer.writeInt(payload.z()); }
    };
    @Override public Type<OpenSharedLocationPayload> type() { return TYPE; }
}
