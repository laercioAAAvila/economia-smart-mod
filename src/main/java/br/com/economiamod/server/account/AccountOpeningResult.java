package br.com.economiamod.server.account;

import java.util.UUID;

public record AccountOpeningResult(boolean success, String code, UUID accountId) {
    public static AccountOpeningResult created(UUID accountId) {
        return new AccountOpeningResult(true, "created", accountId);
    }

    public static AccountOpeningResult denied(String code) {
        return new AccountOpeningResult(false, code, null);
    }
}
