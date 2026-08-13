package br.com.economiamod.server.group;

import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PrivatePropertyAccessService {
    public boolean hasAccess(UUID playerUuid) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT 1
                       FROM economy_claim_territories t
                      WHERE t.claim_type = 'PRIVATE_PROPERTY'
                        AND t.server_uuid = ?
                        AND (t.owner_player_uuid = ? OR EXISTS (
                            SELECT 1 FROM economy_private_property_members m
                             WHERE m.territory_id = t.id AND m.player_uuid = ?
                        ))
                      LIMIT 1
                     """)) {
            statement.setObject(1, BankServerIdentityService.INSTANCE.current());
            statement.setObject(2, playerUuid);
            statement.setObject(3, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Set<UUID> relatedPlayers(UUID playerUuid) throws SQLException {
        Set<UUID> players = new HashSet<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     WITH accessible AS (
                         SELECT t.id, t.owner_player_uuid
                           FROM economy_claim_territories t
                          WHERE t.claim_type = 'PRIVATE_PROPERTY'
                            AND t.server_uuid = ?
                            AND (t.owner_player_uuid = ? OR EXISTS (
                                SELECT 1 FROM economy_private_property_members own_access
                                 WHERE own_access.territory_id = t.id AND own_access.player_uuid = ?
                            ))
                     )
                     SELECT owner_player_uuid AS player_uuid FROM accessible
                     UNION
                     SELECT member.player_uuid
                       FROM accessible
                       JOIN economy_private_property_members member ON member.territory_id = accessible.id
                     """)) {
            statement.setObject(1, BankServerIdentityService.INSTANCE.current());
            statement.setObject(2, playerUuid);
            statement.setObject(3, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(resultSet.getObject("player_uuid", UUID.class));
                }
            }
        }
        return players;
    }
}
