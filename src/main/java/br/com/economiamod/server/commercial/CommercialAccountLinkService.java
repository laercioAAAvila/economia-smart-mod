package br.com.economiamod.server.commercial;

import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class CommercialAccountLinkService {
    private final CommercialOwnerRepository ownerRepository = new CommercialOwnerRepository();
    private final CommercialAccountLinkWriteRepository writeRepository = new CommercialAccountLinkWriteRepository();
    private final CardValidationService cardValidationService = new CardValidationService(new CardItemDataService());

    public boolean linkReceivingAccount(UUID ownerPlayerUuid, UUID commercialBlockId, ItemStack cardStack) throws SQLException {
        CardValidationResult card = validOwnedCard(ownerPlayerUuid, commercialBlockId, cardStack);
        if (card == null || !card.cardType().hasDebit()) {
            return false;
        }
        writeRepository.linkAccount(commercialBlockId, card.accountId());
        return true;
    }

    public boolean linkFundingCard(UUID ownerPlayerUuid, UUID commercialBlockId, ItemStack cardStack) throws SQLException {
        CardValidationResult card = validOwnedCard(ownerPlayerUuid, commercialBlockId, cardStack);
        if (card == null || !card.cardType().hasCredit()) {
            return false;
        }
        writeRepository.linkFundingCard(commercialBlockId, card.cardId());
        return true;
    }

    private CardValidationResult validOwnedCard(UUID ownerPlayerUuid, UUID commercialBlockId, ItemStack cardStack) throws SQLException {
        if (!ownerRepository.owner(commercialBlockId).filter(ownerPlayerUuid::equals).isPresent()) {
            return null;
        }
        CardValidationResult card = cardValidationService.validate(cardStack);
        return card.type() == CardValidationResultType.VALID ? card : null;
    }
}
