package br.com.economiamod.server.account;

import br.com.economiamod.common.account.AccountType;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class SystemAccountIds {
    public static final UUID TREASURY = id(AccountType.SYSTEM_TREASURY);
    public static final UUID CASH = id(AccountType.SYSTEM_CASH);
    public static final UUID CURRENCY_ISSUANCE = id(AccountType.SYSTEM_CURRENCY_ISSUANCE);

    private SystemAccountIds() {
    }

    public static UUID id(AccountType accountType) {
        return UUID.nameUUIDFromBytes(("economia:" + accountType.name()).getBytes(StandardCharsets.UTF_8));
    }
}

