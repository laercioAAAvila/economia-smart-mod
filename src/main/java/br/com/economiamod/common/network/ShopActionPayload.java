package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShopActionPayload(ShopAction action, long price, int quantity, boolean active, UUID requestId) implements CustomPacketPayload {
    public static final Type<ShopActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "shop_action"));

    public ShopActionPayload(ShopAction action, long price, int quantity, boolean active) {
        this(action, price, quantity, active, UUID.randomUUID());
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ShopActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ShopActionPayload(ShopAction.byId(buffer.readVarInt()), buffer.readLong(), buffer.readVarInt(), buffer.readBoolean(), buffer.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ShopActionPayload payload) {
            buffer.writeVarInt(payload.action().id());
            buffer.writeLong(payload.price());
            buffer.writeVarInt(payload.quantity());
            buffer.writeBoolean(payload.active());
            buffer.writeUUID(payload.requestId());
        }
    };

    @Override
    public Type<ShopActionPayload> type() {
        return TYPE;
    }
}
