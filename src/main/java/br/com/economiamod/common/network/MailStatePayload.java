package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MailStatePayload(
        boolean named,
        String mailName,
        UUID selectedRecipientId,
        boolean paymentComplete,
        boolean changeWarning,
        List<RecipientSummary> recipients
) implements CustomPacketPayload {
    private static final int MAX_RECIPIENTS = 64;
    public static final Type<MailStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "mail_state"));

    public MailStatePayload {
        mailName = mailName == null ? "" : mailName;
        selectedRecipientId = selectedRecipientId == null ? new UUID(0L, 0L) : selectedRecipientId;
        recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MailStatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MailStatePayload decode(RegistryFriendlyByteBuf buffer) {
            boolean named = buffer.readBoolean();
            String mailName = buffer.readUtf(64);
            UUID selected = buffer.readUUID();
            boolean paymentComplete = buffer.readBoolean();
            boolean changeWarning = buffer.readBoolean();
            int declaredSize = Math.max(0, buffer.readVarInt());
            if (declaredSize > MAX_RECIPIENTS) {
                throw new IllegalArgumentException("Too many mail recipients in payload: " + declaredSize);
            }
            int size = declaredSize;
            List<RecipientSummary> recipients = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                recipients.add(new RecipientSummary(
                        buffer.readUUID(),
                        buffer.readUtf(64),
                        buffer.readUtf(64),
                        buffer.readUtf(255),
                        buffer.readInt(),
                        buffer.readInt(),
                        buffer.readInt()
                ));
            }
            return new MailStatePayload(named, mailName, selected, paymentComplete, changeWarning, recipients);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MailStatePayload payload) {
            buffer.writeBoolean(payload.named());
            buffer.writeUtf(payload.mailName(), 64);
            buffer.writeUUID(payload.selectedRecipientId());
            buffer.writeBoolean(payload.paymentComplete());
            buffer.writeBoolean(payload.changeWarning());
            int size = Math.min(MAX_RECIPIENTS, payload.recipients().size());
            buffer.writeVarInt(size);
            for (int index = 0; index < size; index++) {
                RecipientSummary recipient = payload.recipients().get(index);
                buffer.writeUUID(recipient.destinationBlockId());
                buffer.writeUtf(recipient.ownerName(), 64);
                buffer.writeUtf(recipient.mailName(), 64);
                buffer.writeUtf(recipient.dimension(), 255);
                buffer.writeInt(recipient.x());
                buffer.writeInt(recipient.y());
                buffer.writeInt(recipient.z());
            }
        }
    };

    @Override
    public Type<MailStatePayload> type() {
        return TYPE;
    }

    public record RecipientSummary(UUID destinationBlockId, String ownerName, String mailName, String dimension, int x, int y, int z) {
        public RecipientSummary {
            ownerName = ownerName == null || ownerName.isBlank() ? "-" : ownerName;
            mailName = mailName == null || mailName.isBlank() ? "-" : mailName;
            dimension = dimension == null ? "" : dimension;
        }
    }
}
