package br.com.economiamod.server.commercial;

import br.com.economiamod.common.block.BlockProtectionPolicy;
import br.com.economiamod.registry.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CommercialBlockProtectionPolicy {
    private CommercialBlockProtectionPolicy() {
    }

    public static BlockProtectionPolicy of(BlockState state) {
        if (state.is(ModBlocks.CLAN_CLAIM_BLOCK.get()) || state.is(ModBlocks.PRIVATE_PROPERTY_CLAIM_BLOCK.get())) {
            return BlockProtectionPolicy.GROUP_PROTECTED;
        }
        if (state.is(ModBlocks.CLAN_CHEST.get()) || state.is(ModBlocks.PRIVATE_PROPERTY_CHEST.get())
                || state.is(ModBlocks.CLAN_MANAGEMENT_BLOCK.get()) || state.is(ModBlocks.PRIVATE_PROPERTY_MANAGEMENT_BLOCK.get())) {
            return BlockProtectionPolicy.GROUP_PROTECTED;
        }
        if (state.is(ModBlocks.BANK_COUNTER.get())) {
            return BlockProtectionPolicy.SYSTEM_PROTECTED;
        }
        if (state.is(ModBlocks.ATM.get()) || state.is(ModBlocks.SELL_SHOP.get())
                || state.is(ModBlocks.BUY_SHOP.get()) || state.is(ModBlocks.MAIL.get())) {
            return BlockProtectionPolicy.OWNER_PROTECTED;
        }
        return BlockProtectionPolicy.COMMON;
    }
}
