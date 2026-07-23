package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AtmCardsPayload(List<CardSummary> cards) implements CustomPacketPayload {
    private static final int MAX_CARDS = 64;
    public static final Type<AtmCardsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "atm_cards"));

    public AtmCardsPayload {
        cards = cards == null ? List.of() : List.copyOf(cards);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, AtmCardsPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AtmCardsPayload decode(RegistryFriendlyByteBuf buffer) {
            int declaredSize = Math.max(0, buffer.readVarInt());
            if (declaredSize > MAX_CARDS) {
                throw new IllegalArgumentException("Too many ATM cards in payload: " + declaredSize);
            }
            int size = declaredSize;
            List<CardSummary> cards = new ArrayList<>(size);
            for (int index = 0; index < declaredSize; index++) {
                CardSummary card = new CardSummary(
                        buffer.readUUID(),
                        buffer.readUtf(32),
                        buffer.readUtf(16),
                        buffer.readUtf(16),
                        buffer.readLong(),
                        buffer.readLong(),
                        buffer.readLong()
                );
                if (index < size) {
                    cards.add(card);
                }
            }
            return new AtmCardsPayload(cards);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AtmCardsPayload payload) {
            int size = Math.min(MAX_CARDS, payload.cards().size());
            buffer.writeVarInt(size);
            for (int index = 0; index < size; index++) {
                CardSummary card = payload.cards().get(index);
                buffer.writeUUID(card.cardId());
                buffer.writeUtf(card.cardName(), 32);
                buffer.writeUtf(card.cardType(), 16);
                buffer.writeUtf(card.status(), 16);
                buffer.writeLong(card.individualCreditLimit());
                buffer.writeLong(card.debt());
                buffer.writeLong(card.debitDailyLimit());
            }
        }
    };

    @Override
    public Type<AtmCardsPayload> type() {
        return TYPE;
    }

    public record CardSummary(
            UUID cardId,
            String cardName,
            String cardType,
            String status,
            long individualCreditLimit,
            long debt,
            long debitDailyLimit
    ) {
        public CardSummary {
            cardName = cardName == null ? "" : cardName;
            cardType = cardType == null ? "" : cardType;
            status = status == null ? "" : status;
        }
    }
}
