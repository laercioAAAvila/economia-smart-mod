package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AtmAccountSummaryPayload(
        boolean available,
        long balance,
        long availableBalance,
        long configuredCreditLimit,
        long creditDebt,
        long creditAvailable
) implements CustomPacketPayload {
    public static final Type<AtmAccountSummaryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "atm_account_summary"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AtmAccountSummaryPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AtmAccountSummaryPayload decode(RegistryFriendlyByteBuf buffer) {
            return new AtmAccountSummaryPayload(
                    buffer.readBoolean(),
                    buffer.readLong(),
                    buffer.readLong(),
                    buffer.readLong(),
                    buffer.readLong(),
                    buffer.readLong()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AtmAccountSummaryPayload payload) {
            buffer.writeBoolean(payload.available());
            buffer.writeLong(payload.balance());
            buffer.writeLong(payload.availableBalance());
            buffer.writeLong(payload.configuredCreditLimit());
            buffer.writeLong(payload.creditDebt());
            buffer.writeLong(payload.creditAvailable());
        }
    };

    @Override
    public Type<AtmAccountSummaryPayload> type() {
        return TYPE;
    }
}
