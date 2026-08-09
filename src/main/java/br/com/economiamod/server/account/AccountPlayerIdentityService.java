package br.com.economiamod.server.account;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class AccountPlayerIdentityService {
    public void refresh(UUID playerUuid, String playerName) throws SQLException {
        if (playerUuid == null || playerName == null || playerName.isBlank()) {
            return;
        }
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_accounts
                        SET minecraft_player_name = ?, updated_at = CURRENT_TIMESTAMP, version = version + 1
                      WHERE account_type = 'PLAYER' AND player_uuid = ?
                        AND server_uuid = ?
                        AND minecraft_player_name IS DISTINCT FROM ?
                     """)) {
            statement.setString(1, playerName);
            statement.setObject(2, playerUuid);
            statement.setObject(3, BankServerIdentityService.INSTANCE.current());
            statement.setString(4, playerName);
            statement.executeUpdate();
        }
    }
}
