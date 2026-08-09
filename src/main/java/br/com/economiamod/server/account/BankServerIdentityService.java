package br.com.economiamod.server.account;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class BankServerIdentityService {
    public static final BankServerIdentityService INSTANCE = new BankServerIdentityService();

    private volatile UUID serverUuid;

    private BankServerIdentityService() {
    }

    public synchronized UUID initialize() throws SQLException {
        String configured = EconomyServerConfig.BANK_SERVER_UUID.get();
        UUID resolved;
        try {
            resolved = configured == null || configured.isBlank() ? null : UUID.fromString(configured.strip());
        } catch (IllegalArgumentException exception) {
            resolved = null;
            EconomiaMod.LOGGER.warn("UUID bancário do servidor inválido; um novo identificador será gerado; valor omitido.");
        }
        if (resolved == null) {
            resolved = UUID.randomUUID();
            EconomyServerConfig.BANK_SERVER_UUID.set(resolved.toString());
            EconomyServerConfig.SPEC.save();
        }
        serverUuid = resolved;
        adoptLegacyAccounts(resolved);
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

    private void adoptLegacyAccounts(UUID serverId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_accounts
                        SET server_uuid = ?, updated_at = CURRENT_TIMESTAMP, version = version + 1
                      WHERE account_type = 'PLAYER' AND server_uuid IS NULL
                     """)) {
            statement.setObject(1, serverId);
            int adopted = statement.executeUpdate();
            if (adopted > 0) {
                EconomiaMod.LOGGER.info("Contas bancárias legadas vinculadas ao servidor atual: {}.", adopted);
            }
        }
    }
}
