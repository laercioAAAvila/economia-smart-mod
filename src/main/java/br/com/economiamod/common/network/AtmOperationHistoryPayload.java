package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AtmOperationHistoryPayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 16;
    public static final Type<AtmOperationHistoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "atm_operation_history"));

    public AtmOperationHistoryPayload {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, AtmOperationHistoryPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AtmOperationHistoryPayload decode(RegistryFriendlyByteBuf buffer) {
            int size = Math.max(0, buffer.readVarInt());
            if (size > MAX_ENTRIES) {
                throw new IllegalArgumentException("Too many ATM history entries in payload: " + size);
            }
            List<Entry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                entries.add(new Entry(
                        buffer.readUtf(64),
                        buffer.readUtf(64),
                        buffer.readLong(),
                        buffer.readUtf(16)
                ));
            }
            return new AtmOperationHistoryPayload(entries);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AtmOperationHistoryPayload payload) {
            int size = Math.min(MAX_ENTRIES, payload.entries().size());
            buffer.writeVarInt(size);
            for (int index = 0; index < size; index++) {
                Entry entry = payload.entries().get(index);
                buffer.writeUtf(entry.operationKey(), 64);
                buffer.writeUtf(entry.directionKey(), 64);
                buffer.writeLong(entry.amount());
                buffer.writeUtf(entry.occurredAt(), 16);
            }
        }
    };

    @Override
    public Type<AtmOperationHistoryPayload> type() {
        return TYPE;
    }

    public record Entry(String operationKey, String directionKey, long amount, String occurredAt) {
        public Entry {
            operationKey = operationKey == null ? "" : operationKey;
            directionKey = directionKey == null ? "" : directionKey;
            occurredAt = occurredAt == null ? "" : occurredAt;
        }
    }
}
