package br.com.economiamod.server.commercial;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class CommercialOwnerRepository {
    public Optional<UUID> owner(UUID commercialBlockId) throws SQLException {
        String sql = "SELECT owner_player_uuid FROM economy_commercial_blocks WHERE id = ? AND status = 'ACTIVE'";
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, commercialBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(resultSet.getObject("owner_player_uuid", UUID.class));
            }
        }
    }
}
