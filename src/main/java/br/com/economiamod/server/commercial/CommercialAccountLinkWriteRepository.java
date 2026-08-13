package br.com.economiamod.server.commercial;

import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class CommercialAccountLinkWriteRepository {
    public void linkAccount(UUID commercialBlockId, UUID accountId) throws SQLException {
        update(commercialBlockId, "linked_account_id", accountId);
    }

    public void linkFundingCard(UUID commercialBlockId, UUID cardId) throws SQLException {
        update(commercialBlockId, "funding_card_id", cardId);
    }

    private void update(UUID commercialBlockId, String column, UUID value) throws SQLException {
        String sql = "UPDATE economy_commercial_blocks SET " + column
                + " = ?, server_uuid = ?, updated_at = CURRENT_TIMESTAMP"
                + " WHERE id = ? AND (server_uuid = ? OR server_uuid IS NULL) AND status = 'ACTIVE'";
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
            statement.setObject(3, commercialBlockId);
            statement.setObject(4, BankServerIdentityService.INSTANCE.current());
            statement.executeUpdate();
        }
    }
}
