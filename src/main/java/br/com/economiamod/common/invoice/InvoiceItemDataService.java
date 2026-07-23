package br.com.economiamod.common.invoice;

import br.com.economiamod.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class InvoiceItemDataService {
    private static final String ROOT_TAG = "EconomiaInvoice";
    private static final String ACCOUNT_ID_TAG = "AccountId";
    private static final String ENTRY_ID_TAG = "EntryId";
    private static final String AMOUNT_TAG = "Amount";

    public ItemStack create(UUID accountId, UUID entryId, long amount) {
        ItemStack stack = new ItemStack(ModItems.CREDIT_INVOICE.get());
        CompoundTag root = new CompoundTag();
        CompoundTag invoiceTag = new CompoundTag();
        invoiceTag.putUUID(ACCOUNT_ID_TAG, accountId);
        invoiceTag.putUUID(ENTRY_ID_TAG, entryId);
        invoiceTag.putLong(AMOUNT_TAG, Math.max(0L, amount));
        root.put(ROOT_TAG, invoiceTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.economia.credit_invoice.named", amount));
        return stack;
    }

    public Optional<InvoiceItemData> read(ItemStack stack) {
        if (!isInvoiceItem(stack)) {
            return Optional.empty();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(ROOT_TAG)) {
            return Optional.empty();
        }
        CompoundTag invoiceTag = customData.copyTag().getCompound(ROOT_TAG);
        if (!invoiceTag.hasUUID(ACCOUNT_ID_TAG) || !invoiceTag.hasUUID(ENTRY_ID_TAG) || !invoiceTag.contains(AMOUNT_TAG)) {
            return Optional.empty();
        }
        return Optional.of(new InvoiceItemData(
                invoiceTag.getUUID(ACCOUNT_ID_TAG),
                invoiceTag.getUUID(ENTRY_ID_TAG),
                invoiceTag.getLong(AMOUNT_TAG)
        ));
    }

    public boolean isInvoiceItem(ItemStack stack) {
        return stack.is(ModItems.CREDIT_INVOICE.get());
    }
}
