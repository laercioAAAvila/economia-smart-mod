package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SecureAccountPayload(
        SecureAccountAction action,
        String username,
        String password,
        String newPassword,
        UUID requestId
) implements CustomPacketPayload {
    public static final Type<SecureAccountPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "secure_account"));

    public SecureAccountPayload {
        username = username == null ? "" : username;
        password = password == null ? "" : password;
        newPassword = newPassword == null ? "" : newPassword;
        requestId = requestId == null ? UUID.randomUUID() : requestId;
    }

    public SecureAccountPayload(SecureAccountAction action, String username, String password, String newPassword) {
        this(action, username, password, newPassword, UUID.randomUUID());
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SecureAccountPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SecureAccountPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SecureAccountPayload(
                    SecureAccountAction.byId(buffer.readVarInt()),
                    buffer.readUtf(64),
                    buffer.readUtf(128),
                    buffer.readUtf(128),
                    buffer.readUUID()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SecureAccountPayload payload) {
            buffer.writeVarInt(payload.action().id());
            buffer.writeUtf(payload.username(), 64);
            buffer.writeUtf(payload.password(), 128);
            buffer.writeUtf(payload.newPassword(), 128);
            buffer.writeUUID(payload.requestId());
        }
    };

    @Override
    public Type<SecureAccountPayload> type() {
        return TYPE;
    }
}
