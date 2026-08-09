package br.com.economiamod.common.invoice;

import br.com.economiamod.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ClaimInvoiceItemDataService {
    private static final String ROOT_TAG = "EconomiaClaimInvoice";
    private static final String ID_TAG = "InvoiceId";
    private static final String AMOUNT_TAG = "Amount";
    private static final String TYPE_TAG = "InvoiceType";

    public ItemStack create(UUID invoiceId, long amount, String invoiceType) {
        ItemStack stack = new ItemStack(ModItems.CREDIT_INVOICE.get());
        CompoundTag invoice = new CompoundTag();
        invoice.putUUID(ID_TAG, invoiceId);
        invoice.putLong(AMOUNT_TAG, Math.max(0L, amount));
        invoice.putString(TYPE_TAG, invoiceType == null ? "LAND" : invoiceType);
        CompoundTag root = new CompoundTag();
        root.put(ROOT_TAG, invoice);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.economia.claim_invoice.named", amount));
        return stack;
    }

    public Optional<ClaimInvoiceItemData> read(ItemStack stack) {
        if (!stack.is(ModItems.CREDIT_INVOICE.get())) {
            return Optional.empty();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(ROOT_TAG)) {
            return Optional.empty();
        }
        CompoundTag invoice = customData.copyTag().getCompound(ROOT_TAG);
        if (!invoice.hasUUID(ID_TAG) || !invoice.contains(AMOUNT_TAG) || !invoice.contains(TYPE_TAG)) {
            return Optional.empty();
        }
        return Optional.of(new ClaimInvoiceItemData(invoice.getUUID(ID_TAG), invoice.getLong(AMOUNT_TAG),
                invoice.getString(TYPE_TAG)));
    }
}
