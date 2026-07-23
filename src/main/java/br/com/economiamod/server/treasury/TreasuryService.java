package br.com.economiamod.server.treasury;

import br.com.economiamod.server.account.SystemAccountIds;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class TreasuryService {
    public TreasuryBalance balance() throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT balance FROM economy_accounts WHERE id = ?")) {
            statement.setObject(1, SystemAccountIds.TREASURY);
            try (ResultSet resultSet = statement.executeQuery()) {
                return new TreasuryBalance(resultSet.next() ? resultSet.getLong("balance") : 0L);
            }
        }
    }

    public boolean hasEnough(long amount) throws SQLException {
        if (amount < 0L) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        return balance().balance() >= amount;
    }
}

