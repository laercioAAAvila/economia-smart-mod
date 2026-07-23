package br.com.economiamod.common.card;

import java.util.UUID;

public record CardItemData(UUID cardId, int securityVersion, CardType cardType) {
}

