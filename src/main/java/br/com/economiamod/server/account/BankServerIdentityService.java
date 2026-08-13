package br.com.economiamod.server.account;

import br.com.economiamod.EconomiaMod;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class BankServerIdentityService {
    public static final BankServerIdentityService INSTANCE = new BankServerIdentityService();

    private volatile UUID serverUuid;

    private BankServerIdentityService() {
    }

    public synchronized UUID initialize(MinecraftServer server) throws SQLException {
        UUID resolved = BankWorldIdentitySavedData.get(server);
        serverUuid = resolved;
        EconomiaMod.LOGGER.info("Identidade econômica do mundo inicializada: serverUuid={}.", resolved);
        return resolved;
    }

    public UUID current() {
        UUID current = serverUuid;
        if (current == null) {
            throw new IllegalStateException("Bank server identity is not initialized");
        }
        return current;
    }

    public synchronized void clear() {
        serverUuid = null;
    }

}
