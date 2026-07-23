package br.com.economiamod.common.card;

import br.com.economiamod.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CardItemDataService {
    private static final String ROOT_TAG = "EconomiaCard";
    private static final String CARD_ID_TAG = "CardId";
    private static final String SECURITY_VERSION_TAG = "SecurityVersion";
    private static final String CARD_TYPE_TAG = "CardType";
    private static final String ACCOUNT_NUMBER_TAG = "AccountNumber";
    private static final String DEBIT_DAILY_LIMIT_TAG = "DebitDailyLimit";
    private static final String CREDIT_LIMIT_TAG = "CreditLimit";

    public ItemStack createCardStack(CardType cardType, UUID cardId, int securityVersion) {
        return createCardStack(cardType, cardId, securityVersion, "");
    }

    public ItemStack createCardStack(CardType cardType, UUID cardId, int securityVersion, String accountNumber) {
        return createCardStack(cardType, cardId, securityVersion, accountNumber, "");
    }

    public ItemStack createCardStack(CardType cardType, UUID cardId, int securityVersion, String accountNumber, String cardName) {
        return createCardStack(cardType, cardId, securityVersion, accountNumber, cardName, 0L);
    }

    public ItemStack createCardStack(CardType cardType, UUID cardId, int securityVersion, String accountNumber, String cardName, long creditLimit) {
        ItemStack stack = new ItemStack(itemFor(cardType));
        CompoundTag root = new CompoundTag();
        CompoundTag cardTag = new CompoundTag();
        cardTag.putUUID(CARD_ID_TAG, cardId);
        cardTag.putInt(SECURITY_VERSION_TAG, securityVersion);
        cardTag.putString(CARD_TYPE_TAG, cardType.name());
        if (accountNumber != null && accountNumber.matches("[0-9]{6}")) {
            cardTag.putString(ACCOUNT_NUMBER_TAG, accountNumber);
        }
        cardTag.putLong(DEBIT_DAILY_LIMIT_TAG, 0L);
        if (cardType.hasCredit()) {
            cardTag.putLong(CREDIT_LIMIT_TAG, Math.max(0L, creditLimit));
        }
        root.put(ROOT_TAG, cardTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        if (cardName != null && !cardName.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(cardName.trim()));
        }
        return stack;
    }

    public Optional<CardItemData> read(ItemStack stack) {
        CardType expectedType = typeFor(stack).orElse(null);
        if (expectedType == null) {
            return Optional.empty();
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(ROOT_TAG)) {
            return Optional.empty();
        }

        CompoundTag root = customData.copyTag();
        CompoundTag cardTag = root.getCompound(ROOT_TAG);
        if (!cardTag.hasUUID(CARD_ID_TAG) || !cardTag.contains(SECURITY_VERSION_TAG) || !cardTag.contains(CARD_TYPE_TAG)) {
            return Optional.empty();
        }

        CardType storedType;
        try {
            storedType = CardType.valueOf(cardTag.getString(CARD_TYPE_TAG));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        if (storedType != expectedType) {
            return Optional.empty();
        }

        return Optional.of(new CardItemData(
                cardTag.getUUID(CARD_ID_TAG),
                cardTag.getInt(SECURITY_VERSION_TAG),
                storedType
        ));
    }

    public boolean isValidCardItem(ItemStack stack) {
        return read(stack).isPresent();
    }

    public Optional<String> accountNumber(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(ROOT_TAG)) {
            return Optional.empty();
        }

        String accountNumber = customData.copyTag().getCompound(ROOT_TAG).getString(ACCOUNT_NUMBER_TAG);
        return accountNumber.matches("[0-9]{6}") ? Optional.of(accountNumber) : Optional.empty();
    }

    public Optional<Long> debitDailyLimit(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(ROOT_TAG)) {
            return Optional.empty();
        }

        CompoundTag cardTag = customData.copyTag().getCompound(ROOT_TAG);
        return cardTag.contains(DEBIT_DAILY_LIMIT_TAG) ? Optional.of(cardTag.getLong(DEBIT_DAILY_LIMIT_TAG)) : Optional.empty();
    }

    public Optional<Long> creditLimit(ItemStack stack) {
        CardType cardType = read(stack).map(CardItemData::cardType).orElse(null);
        if (cardType == null || !cardType.hasCredit()) {
            return Optional.empty();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(ROOT_TAG)) {
            return Optional.empty();
        }

        CompoundTag cardTag = customData.copyTag().getCompound(ROOT_TAG);
        return cardTag.contains(CREDIT_LIMIT_TAG) ? Optional.of(cardTag.getLong(CREDIT_LIMIT_TAG)) : Optional.empty();
    }

    public void setDebitDailyLimit(ItemStack stack, long limit) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(ROOT_TAG)) {
            return;
        }
        CompoundTag root = customData.copyTag();
        CompoundTag cardTag = root.getCompound(ROOT_TAG);
        cardTag.putLong(DEBIT_DAILY_LIMIT_TAG, Math.max(0L, limit));
        root.put(ROOT_TAG, cardTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public void setCreditLimit(ItemStack stack, long limit) {
        CardType cardType = read(stack).map(CardItemData::cardType).orElse(null);
        if (cardType == null || !cardType.hasCredit()) {
            return;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(ROOT_TAG)) {
            return;
        }
        CompoundTag root = customData.copyTag();
        CompoundTag cardTag = root.getCompound(ROOT_TAG);
        cardTag.putLong(CREDIT_LIMIT_TAG, Math.max(0L, limit));
        root.put(ROOT_TAG, cardTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private Optional<CardType> typeFor(ItemStack stack) {
        if (stack.is(ModItems.DEBIT_CARD.get())) {
            return Optional.of(CardType.DEBIT);
        }
        if (stack.is(ModItems.CREDIT_CARD.get())) {
            return Optional.of(CardType.CREDIT);
        }
        if (stack.is(ModItems.DEBIT_CREDIT_CARD.get())) {
            return Optional.of(CardType.DEBIT_CREDIT);
        }
        return Optional.empty();
    }

    private Item itemFor(CardType cardType) {
        return switch (cardType) {
            case DEBIT -> ModItems.DEBIT_CARD.get();
            case CREDIT -> ModItems.CREDIT_CARD.get();
            case DEBIT_CREDIT -> ModItems.DEBIT_CREDIT_CARD.get();
        };
    }
}
