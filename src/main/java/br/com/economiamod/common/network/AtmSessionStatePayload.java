package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AtmSessionStatePayload(boolean loggedIn, String username, String accountNumber, boolean showUsername) implements CustomPacketPayload {
    public static final Type<AtmSessionStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "atm_session_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AtmSessionStatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AtmSessionStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new AtmSessionStatePayload(buffer.readBoolean(), buffer.readUtf(64), buffer.readUtf(6), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AtmSessionStatePayload payload) {
            buffer.writeBoolean(payload.loggedIn());
            buffer.writeUtf(payload.username(), 64);
            buffer.writeUtf(payload.accountNumber(), 6);
            buffer.writeBoolean(payload.showUsername());
        }
    };

    @Override
    public Type<AtmSessionStatePayload> type() {
        return TYPE;
    }
}
