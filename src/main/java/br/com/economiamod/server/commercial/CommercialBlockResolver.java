package br.com.economiamod.server.commercial;

import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.world.level.block.state.BlockState;

public final class CommercialBlockResolver {
    public Optional<CommercialBlockType> typeOf(BlockState state) {
        if (state.is(ModBlocks.ATM.get())) {
            return Optional.of(CommercialBlockType.ATM);
        }
        if (state.is(ModBlocks.SELL_SHOP.get())) {
            return Optional.of(CommercialBlockType.SELL_SHOP);
        }
        if (state.is(ModBlocks.BUY_SHOP.get())) {
            return Optional.of(CommercialBlockType.BUY_SHOP);
        }
        if (state.is(ModBlocks.BANK_COUNTER.get())) {
            return Optional.of(CommercialBlockType.BANK_COUNTER);
        }
        if (state.is(ModBlocks.MAIL.get())) {
            return Optional.of(CommercialBlockType.MAIL);
        }
        return Optional.empty();
    }
}
