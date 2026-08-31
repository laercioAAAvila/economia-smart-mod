package br.com.economiamod.common.item;

import br.com.economiamod.common.card.CardItemDataService;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class EconomyCardItem extends Item {
    private final CardItemDataService cardItemDataService = new CardItemDataService();

    public EconomyCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        cardItemDataService.read(stack)
                .ifPresent(card -> tooltipComponents.add(Component.translatable("item.economia.card.type." + card.cardType().name().toLowerCase())));
        cardItemDataService.accountNumber(stack)
                .ifPresent(accountNumber -> tooltipComponents.add(Component.translatable("item.economia.card.account_number", accountNumber)));
        cardItemDataService.debitDailyLimit(stack)
                .filter(limit -> limit > 0L)
                .ifPresent(limit -> tooltipComponents.add(Component.translatable("item.economia.card.debit_daily_limit", limit)));
        cardItemDataService.creditLimit(stack)
                .ifPresent(limit -> tooltipComponents.add(Component.translatable("item.economia.card.credit_limit", limit)));
    }
}
