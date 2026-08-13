package br.com.economiamod.server.commercial;

import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class CommercialBlockTypeRepository {
    public Optional<CommercialBlockType> findType(UUID commercialBlockId) throws SQLException {
        String sql = "SELECT block_type FROM economy_commercial_blocks"
                + " WHERE id = ? AND (server_uuid = ? OR server_uuid IS NULL) AND status = 'ACTIVE'";
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, commercialBlockId);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(CommercialBlockType.valueOf(resultSet.getString("block_type")))
                        : Optional.empty();
            }
        }
    }
}
