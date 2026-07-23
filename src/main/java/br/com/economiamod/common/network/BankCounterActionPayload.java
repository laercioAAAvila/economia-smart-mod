package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BankCounterActionPayload(int action, int amount, int unit, UUID requestId) implements CustomPacketPayload {
    public static final int ACTION_MINT = 0;
    public static final int ACTION_REDEEM = 1;
    public static final int ACTION_REFRESH_PRICES = 2;
    public static final int UNIT_NUGGET = 0;
    public static final int UNIT_INGOT = 1;
    public static final int UNIT_BLOCK = 2;

    public static final Type<BankCounterActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "bank_counter_action"));

    public BankCounterActionPayload(int action, int amount, int unit) {
        this(action, amount, unit, UUID.randomUUID());
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, BankCounterActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BankCounterActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new BankCounterActionPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BankCounterActionPayload payload) {
            buffer.writeVarInt(payload.action());
            buffer.writeVarInt(payload.amount());
            buffer.writeVarInt(payload.unit());
            buffer.writeUUID(payload.requestId());
        }
    };

    @Override
    public Type<BankCounterActionPayload> type() {
        return TYPE;
    }
}
