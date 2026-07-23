package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ShopReferencePayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<ShopReferencePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "shop_reference"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopReferencePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ShopReferencePayload decode(RegistryFriendlyByteBuf buffer) {
            return new ShopReferencePayload(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ShopReferencePayload payload) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.stack());
        }
    };

    @Override
    public Type<ShopReferencePayload> type() {
        return TYPE;
    }
}
