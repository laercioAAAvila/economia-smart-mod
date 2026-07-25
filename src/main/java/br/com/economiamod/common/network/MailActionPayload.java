package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MailActionPayload(MailAction action, String text, UUID targetId, boolean creditPayment, UUID requestId) implements CustomPacketPayload {
    public static final Type<MailActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "mail_action"));

    public MailActionPayload(MailAction action, String text) {
        this(action, text, new UUID(0L, 0L), false, UUID.randomUUID());
    }

    public MailActionPayload(MailAction action, UUID targetId) {
        this(action, "", targetId, false, UUID.randomUUID());
    }

    public MailActionPayload(MailAction action, boolean creditPayment) {
        this(action, "", new UUID(0L, 0L), creditPayment, UUID.randomUUID());
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MailActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MailActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new MailActionPayload(
                    MailAction.byId(buffer.readVarInt()),
                    buffer.readUtf(64),
                    buffer.readUUID(),
                    buffer.readBoolean(),
                    buffer.readUUID()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MailActionPayload payload) {
            buffer.writeVarInt(payload.action().id());
            buffer.writeUtf(payload.text() == null ? "" : payload.text(), 64);
            buffer.writeUUID(payload.targetId() == null ? new UUID(0L, 0L) : payload.targetId());
            buffer.writeBoolean(payload.creditPayment());
            buffer.writeUUID(payload.requestId() == null ? UUID.randomUUID() : payload.requestId());
        }
    };

    @Override
    public Type<MailActionPayload> type() {
        return TYPE;
    }
}
